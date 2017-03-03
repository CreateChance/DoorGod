package com.createchance.doorgod.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.createchance.doorgod.R;
import com.createchance.doorgod.lockfragments.BaseFragment;
import com.createchance.doorgod.lockfragments.PatternLockFragment;
import com.createchance.doorgod.lockfragments.PinLockFragment;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LockTypeUtil;
import com.createchance.doorgod.util.LogUtil;

import java.util.Set;

public class DoorGodActivity extends AppCompatActivity {

    private static final String TAG = "DoorGodActivity";

    private boolean isLaunchFromHome = false;

    private BaseFragment fragment;

    private SharedPreferences mPrefs;
    private static final String LOCK_ENROLL_STATUS = "com.createchance.doorgod.LOCK_ENROLL_STATUS";
    private static final String LOCK_ENROLLED = "ENROLLED";

    private DoorGodService.ServiceBinder mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = (DoorGodService.ServiceBinder) service;

            if (mService.getLockType() > 0) {
                addFragment(mService.getLockType());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_door_god);

        // get prefs
        mPrefs = getSharedPreferences(LOCK_ENROLL_STATUS, MODE_PRIVATE);

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            Set<String> catagory = intent.getCategories();
            if (action.equals(Intent.ACTION_MAIN) && catagory.contains(Intent.CATEGORY_LAUNCHER)) {
                isLaunchFromHome = true;
            }
        }

        // start and bind service.
        Intent startServiceIntent = new Intent(DoorGodActivity.this, DoorGodService.class);
        startService(startServiceIntent);
        bindService(startServiceIntent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!isLockEnrolled()) {
            Toast.makeText(DoorGodActivity.this,
                    R.string.first_start_info, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(DoorGodActivity.this, SettingsActivity.class);
            startActivityForResult(intent, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (mService.getLockType() > 0) {
            addFragment(mService.getLockType());
        } else {
            Toast.makeText(DoorGodActivity.this,
                    R.string.no_lock_type_enrolled_warning, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }

    @Override
    public void onBackPressed() {
        if (isLaunchFromHome) {
            fragment.cancelFingerprint();
        } else {
            super.onBackPressed();

            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            startActivity(i);
        }
    }

    public DoorGodService.ServiceBinder getService() {
        return mService;
    }

    public boolean isStartByUser() {
        return isLaunchFromHome;
    }

    private void addFragment(int type) {
        LogUtil.d(TAG, "type: " + type);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (type == LockTypeUtil.TYPE_PIN) {
            fragment = new PinLockFragment();
        } else if (type == LockTypeUtil.TYPE_PATTERN) {
            fragment = new PatternLockFragment();
        }
        transaction.add(R.id.lock_fragment_container, fragment);
        transaction.commit();
    }

    private boolean isLockEnrolled() {
        return mPrefs.getBoolean(LOCK_ENROLLED, false);
    }
}
