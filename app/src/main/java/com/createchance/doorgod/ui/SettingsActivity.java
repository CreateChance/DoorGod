package com.createchance.doorgod.ui;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.createchance.doorgod.R;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LockTypeUtil;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private static final int CODE_REQUEST_ENROLL_ACTIVITY = 500;
    private SharedPreferences mPrefs;
    private static final String LOCK_ENROLL_STATUS = "com.createchance.doorgod.LOCK_ENROLL_STATUS";
    private static final String LOCK_ENROLLED = "ENROLLED";

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
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.menu_title_settings);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // bind to service.
        Intent intent = new Intent(this, DoorGodService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);

        // get prefs
        mPrefs = getSharedPreferences(LOCK_ENROLL_STATUS, MODE_PRIVATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CODE_REQUEST_ENROLL_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putBoolean(LOCK_ENROLLED, true);
                    editor.commit();
                    finish();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
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
            case R.id.lock_type:
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setIcon(R.drawable.ic_settings_white_48dp)
                        .setTitle(R.string.settings_lock_type_title)
                        .setSingleChoiceItems(R.array.lock_type,
                                getSelectedPos(mService.getLockType()),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            Intent intent = new Intent(SettingsActivity.this, EnrollPatternActivity.class);
                                            startActivityForResult(intent, CODE_REQUEST_ENROLL_ACTIVITY);
                                        } else if (which == 1) {
                                            Intent intent = new Intent(SettingsActivity.this, EnrollPinActivity.class);
                                            startActivityForResult(intent, CODE_REQUEST_ENROLL_ACTIVITY);
                                        }
                                        dialog.dismiss();
                                    }
                                });
                builder.create().show();
                break;
            case R.id.trust_wifi:
                if (mService.isWifiEnabled()) {
                    Intent intent = new Intent(SettingsActivity.this, TrustedWifiSettingActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(SettingsActivity.this, R.string.settings_trust_wifi_not_init_info, Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
    }

    private int getSelectedPos(int locktype) {
        if (locktype == LockTypeUtil.TYPE_PATTERN) {
            return 0;
        } else if (locktype == LockTypeUtil.TYPE_PIN) {
            return 1;
        }

        return -1;
    }
}
