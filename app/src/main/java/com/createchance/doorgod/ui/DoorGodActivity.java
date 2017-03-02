package com.createchance.doorgod.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.createchance.doorgod.R;
import com.createchance.doorgod.lockfragments.PatternLockFragment;
import com.createchance.doorgod.lockfragments.PinLockFragment;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LockTypeUtil;
import com.createchance.doorgod.util.LogUtil;

public class DoorGodActivity extends AppCompatActivity {

    private static final String TAG = "DoorGodActivity";

    private DoorGodService.ServiceBinder mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = (DoorGodService.ServiceBinder) service;

            addFragment(mService.getLockType());
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

        Intent intent = new Intent(DoorGodActivity.this, DoorGodService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }

    public DoorGodService.ServiceBinder getService() {
        return mService;
    }

    private void addFragment(int type) {
        LogUtil.d(TAG, "type: " + type);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (type == LockTypeUtil.TYPE_PIN) {
            transaction.add(R.id.lock_fragment_container, new PinLockFragment());
        } else if (type == LockTypeUtil.TYPE_PATTERN) {
            transaction.add(R.id.lock_fragment_container, new PatternLockFragment());
        }
        transaction.commit();
    }
}
