package com.createchance.doorgod.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.createchance.doorgod.R;

public class IntrusionRecordSettingActivity extends AppCompatActivity {

    private static final String TAG = "IntrusionRecordSettingA";

    public static final String PREFS_INTRU_REC = "PREFS_INTRU_REC";
    public static final String KEY_INTRU_REC_ENABLED = "ENABLED";
    public static final String KEY_INTRU_REC_ATTEMPTS_TIMES = "ATTEMPTS_TIMES";
    private int userChoose = 2;
    private SharedPreferences mPrefs;

    private Switch mIntruRecEnabled;
    private CardView mAttemptTimes;
    private CardView mDetails;
    private TextView mAttemptTimesText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intrusion_record_setting);

        mIntruRecEnabled = (Switch) findViewById(R.id.intru_enable);
        mAttemptTimes = (CardView) findViewById(R.id.intru_attempt_times_setting);
        mDetails = (CardView) findViewById(R.id.intru_rec_details);
        mAttemptTimesText = (TextView) findViewById(R.id.intru_attempt_times);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.settings_intru_rec_title);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // get prefs
        mPrefs = getSharedPreferences(PREFS_INTRU_REC, MODE_PRIVATE);
        mIntruRecEnabled.setChecked(mPrefs.getBoolean(KEY_INTRU_REC_ENABLED, false));
        mAttemptTimesText.setText(getResources().
                getStringArray(R.array.intru_rec_attempts_times)[mPrefs.getInt(KEY_INTRU_REC_ATTEMPTS_TIMES, 2) - 1]);
        if (!mPrefs.
                getBoolean(IntrusionRecordSettingActivity.KEY_INTRU_REC_ENABLED, true)) {
            mAttemptTimes.setVisibility(View.GONE);
            mDetails.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void OnClick(View view) {
        switch (view.getId()) {
            case R.id.intru_enable:
                if (!((Switch)view).isChecked()) {
                    mAttemptTimes.setVisibility(View.GONE);
                    mDetails.setVisibility(View.GONE);
                } else {
                    mAttemptTimes.setVisibility(View.VISIBLE);
                    mDetails.setVisibility(View.VISIBLE);
                }
                SharedPreferences.Editor editor = mPrefs.edit();
                editor.putBoolean(KEY_INTRU_REC_ENABLED, ((Switch)view).isChecked());
                editor.apply();
                break;
            case R.id.intru_attempt_times_setting:
                showAttemptsDialog();
                break;
            case R.id.intru_rec_details:
                Intent intent = new Intent(IntrusionRecordSettingActivity.this, IntrusionRecordDetailsActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void showAttemptsDialog() {
        final String[] names = getResources().getStringArray(R.array.intru_rec_attempts_times);
        new AlertDialog.Builder(IntrusionRecordSettingActivity.this)
            .setTitle(R.string.settings_intru_rec_attempt_times_title)
            .setSingleChoiceItems(names, mPrefs.getInt(KEY_INTRU_REC_ATTEMPTS_TIMES, 2) - 1,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            userChoose = which;
                        }
                    })
            .setPositiveButton("ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            SharedPreferences.Editor editor = mPrefs.edit();
                            editor.putInt(KEY_INTRU_REC_ATTEMPTS_TIMES, userChoose + 1);
                            editor.apply();
                            mAttemptTimesText.setText(getResources().
                                    getStringArray(R.array.intru_rec_attempts_times)[userChoose]);
                        }
                    }).setNegativeButton("cancel", null).show();

    }
}
