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
import com.createchance.doorgod.lockfragments.PatternLockFragment;
import com.createchance.doorgod.lockfragments.PinLockFragment;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LockTypeUtil;
import com.createchance.doorgod.util.LogUtil;

import java.util.Set;

public class DoorGodActivity extends AppCompatActivity {

    private static final String TAG = "DoorGodActivity";

    private boolean isLaunchByUser = false;

    private SharedPreferences mPrefs;
    private static final String LOCK_ENROLL_STATUS = "com.createchance.doorgod.LOCK_ENROLL_STATUS";
    private static final String LOCK_ENROLLED = "ENROLLED";

    private DoorGodService.ServiceBinder mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = (DoorGodService.ServiceBinder) service;

            mService.startFingerprintAuth();

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
            if (action != null &&
                    action.equals(Intent.ACTION_MAIN) && catagory.contains(Intent.CATEGORY_LAUNCHER)) {
                isLaunchByUser = true;
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
            Intent intent = new Intent(DoorGodActivity.this, AppListActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mService != null) {
            mService.startFingerprintAuth();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mService != null) {
            mService.cancelFingerprint();
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
        LogUtil.d(TAG, "onDestroy");
        super.onDestroy();

        unbindService(mConnection);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (!isLaunchByUser) {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            startActivity(i);
        }
    }

    public DoorGodService.ServiceBinder getService() {
        return mService;
    }

    public boolean isLaunchFromHome() {
        return isLaunchByUser;
    }

    private void addFragment(int type) {
        LogUtil.d(TAG, "Lock type: " + type);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (type == LockTypeUtil.TYPE_PIN) {
            transaction.add(R.id.lock_fragment_container, new PinLockFragment());
        } else if (type == LockTypeUtil.TYPE_PATTERN) {
            transaction.add(R.id.lock_fragment_container, new PatternLockFragment());
        }

        transaction.commit();
    }

    private boolean isLockEnrolled() {
        return mPrefs.getBoolean(LOCK_ENROLLED, false);
    }
}
