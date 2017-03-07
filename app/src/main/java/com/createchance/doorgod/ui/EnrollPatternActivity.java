package com.createchance.doorgod.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.createchance.doorgod.R;
import com.createchance.doorgod.lockfragments.Lock9View;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LockTypeUtil;

public class EnrollPatternActivity extends AppCompatActivity {

    private static final String TAG = "EnrollPatternActivity";

    private Button btnCancel;
    private Lock9View patternView;
    private TextView enrollInfo;

    private boolean isPatternConfirm = false;

    private String patternString;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_pattern);

        // bind to service.
        Intent intent = new Intent(this, DoorGodService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);

        patternView = (Lock9View) findViewById(R.id.patternView);
        patternView.setCallBack(new Lock9View.CallBack() {
            @Override
            public void onFinish(String password) {
                if (isPatternConfirm) {
                    isPatternConfirm = false;
                    if (patternString.equals(password)) {
                        // save lock info.
                        mService.saveLockInfo(patternString, LockTypeUtil.TYPE_PATTERN);
                        // stop ourselves
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        enrollInfo.setText(R.string.pattern_lock_enroll_step1_info);
                        Toast.makeText(EnrollPatternActivity.this,
                                R.string.pattern_lock_enroll_info_mismatch, Toast.LENGTH_LONG).show();
                    }
                } else {
                    patternString = password;
                    isPatternConfirm = true;
                    enrollInfo.setText(R.string.pattern_lock_enroll_step2_info);
                }
            }
        });

        enrollInfo = (TextView) findViewById(R.id.enroll_info);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }
}
