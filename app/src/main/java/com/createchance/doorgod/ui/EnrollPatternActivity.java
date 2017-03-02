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
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LockTypeUtil;
import com.eftimoff.patternview.PatternView;

public class EnrollPatternActivity extends AppCompatActivity {

    private static final String TAG = "EnrollPatternActivity";

    private Button btnCancel;
    private Button btnOk;
    private PatternView patternView;
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

        patternView = (PatternView) findViewById(R.id.patternView);
        enrollInfo = (TextView) findViewById(R.id.enroll_info);
        btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnOk = (Button) findViewById(R.id.btn_ok);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPatternConfirm) {
                    isPatternConfirm = false;
                    if (patternString.equals(patternView.getPatternString())) {
                        // save lock info.
                        mService.saveLockInfo(patternString, LockTypeUtil.TYPE_PATTERN);
                        // stop ourselves
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        btnOk.setText(R.string.lock_enroll_btn_step1);
                        enrollInfo.setText(R.string.pattern_lock_enroll_step1_info);
                        Toast.makeText(EnrollPatternActivity.this,
                                R.string.pattern_lock_enroll_info_mismatch, Toast.LENGTH_LONG).show();
                    }
                } else {
                    patternString = patternView.getPatternString();
                    if (patternString.equals("")) {
                        Toast.makeText(EnrollPatternActivity.this,
                                R.string.pattern_lock_enroll_toast_empty, Toast.LENGTH_SHORT).show();
                    } else {
                        isPatternConfirm = true;
                        btnOk.setText(R.string.lock_enroll_btn_step2);
                        enrollInfo.setText(R.string.pattern_lock_enroll_step2_info);
                    }
                }

                // clear pad.
                patternView.clearPattern();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }
}
