package com.createchance.doorgod.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.createchance.doorgod.R;
import com.createchance.doorgod.database.PatternLockInfo;
import com.eftimoff.patternview.PatternView;

public class EnrollPatternActivity extends AppCompatActivity {

    private static final String TAG = "EnrollPatternActivity";

    private Button btnCancel;
    private Button btnOk;
    private PatternView patternView;
    private TextView enrollInfo;

    private boolean isPatternConfirm = false;

    private String patternString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_pattern);

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
                        PatternLockInfo info = new PatternLockInfo();
                        info.setPatternString(patternView.getPatternString());
                        info.save();

                        // stop ourselves
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        btnOk.setText(R.string.pattern_lock_enroll_btn_step1);
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
                        btnOk.setText(R.string.pattern_lock_enroll_btn_step2);
                        enrollInfo.setText(R.string.pattern_lock_enroll_step2_info);
                    }
                }

                // clear pad.
                patternView.clearPattern();
            }
        });
    }

}
