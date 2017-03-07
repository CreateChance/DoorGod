package com.createchance.doorgod.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.andrognito.pinlockview.IndicatorDots;
import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;
import com.createchance.doorgod.R;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LockTypeUtil;
import com.createchance.doorgod.util.LogUtil;

public class EnrollPinActivity extends AppCompatActivity {

    private static final String TAG = "EnrollPinActivity";

    private IndicatorDots indicatorDots;

    private PinLockView pinLockView;

    private TextView pinLockInfo;

    private boolean isPinConfirm = false;
    private String pinCode;

    private DoorGodService.ServiceBinder mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = (DoorGodService.ServiceBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private PinLockListener mPinLockListener = new PinLockListener() {
        @Override
        public void onComplete(String pin) {
            LogUtil.d(TAG, "Pin complete: " + pin);
            if (isPinConfirm) {
                if (pinCode.equals(pin)) {
                    // save the lock info here.
                    mService.saveLockInfo(pin, LockTypeUtil.TYPE_PIN);

                    isPinConfirm = false;
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EnrollPinActivity.this,
                            R.string.pin_lock_enroll_info_mismatch, Toast.LENGTH_LONG).show();
                    indicatorDots.setPinLength(0);
                    pinLockView.resetPinLockView();
                    isPinConfirm = false;
                    pinLockInfo.setText(R.string.pin_lock_enroll_step1_info);
                }
            } else {
                pinCode = pin;
                isPinConfirm = true;
                pinLockView.resetPinLockView();
                indicatorDots.setPinLength(0);
                pinLockInfo.setText(R.string.pin_lock_enroll_step2_info);
            }
        }

        @Override
        public void onEmpty() {
            LogUtil.d(TAG, "Pin empty");
            indicatorDots.setPinLength(0);
        }

        @Override
        public void onPinChange(int pinLength, String intermediatePin) {
            LogUtil.d(TAG, "Pin changed, new length " + pinLength + " with intermediate pin " + intermediatePin);
            indicatorDots.setPinLength(pinLength);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_pin);

        // bind to service.
        Intent intent = new Intent(this, DoorGodService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);

        pinLockInfo = (TextView) findViewById(R.id.pin_lock_info);

        indicatorDots = (IndicatorDots) findViewById(R.id.indicator_dots);
        indicatorDots.setPinLength(0);

        pinLockView = (PinLockView) findViewById(R.id.pin_lock_view);
        pinLockView.setPinLockListener(mPinLockListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }
}
