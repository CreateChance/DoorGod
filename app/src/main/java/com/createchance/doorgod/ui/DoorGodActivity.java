package com.createchance.doorgod.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.createchance.doorgod.R;
import com.createchance.doorgod.lockfragments.PatternLockFragment;
import com.createchance.doorgod.lockfragments.PinLockFragment;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LockTypeUtil;
import com.createchance.doorgod.util.LogUtil;
import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class DoorGodActivity extends AppCompatActivity implements AuthFailed {

    private static final String TAG = "DoorGodActivity";

    private boolean isLaunchByUser = false;

    private SharedPreferences mPrefs;
    private SharedPreferences mIntruRecAttemptsPrefs;
    private static final String LOCK_ENROLL_STATUS = "com.createchance.doorgod.LOCK_ENROLL_STATUS";
    private static final String LOCK_ENROLLED = "ENROLLED";

    private CameraView mCameraView;

    private int mAuthFailedTimes = 0;
    private Handler mBackgroundHandler;

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

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    // we use time stamp and locked app name to be the image file name for easy to determine time.
                    String picName = String.valueOf(System.currentTimeMillis()) + "_" + mService.getCurrentAppName() + ".jpg";
                    // picture save to /sdcard/Android/data/com.createchance.doorgod/files/Pictures/
                    // this picture is a cache folder, will removed when uninstall.
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), picName);
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(file);
                        os.write(data);
                        os.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }
            });
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_door_god);

        // get camera view
        mCameraView = (CameraView) findViewById(R.id.cameraview);
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }

        // get prefs
        mPrefs = getSharedPreferences(LOCK_ENROLL_STATUS, MODE_PRIVATE);
        mIntruRecAttemptsPrefs = getSharedPreferences(IntrusionRecordSettingActivity.PREFS_INTRU_REC, MODE_PRIVATE);

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // use the front camera to catch user face.
            mCameraView.setFacing(CameraView.FACING_FRONT);
            mCameraView.start();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    1);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mService != null) {
            mService.cancelFingerprint();
        }

        mCameraView.stop();
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
            PinLockFragment fragment = new PinLockFragment();
            fragment.setCallback(this);
            transaction.add(R.id.lock_fragment_container, fragment);
        } else if (type == LockTypeUtil.TYPE_PATTERN) {
            PatternLockFragment fragment = new PatternLockFragment();
            fragment.setCallback(this);
            transaction.add(R.id.lock_fragment_container, fragment);
        }

        transaction.commit();
    }

    private boolean isLockEnrolled() {
        return mPrefs.getBoolean(LOCK_ENROLLED, false);
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    @Override
    public void onFailed() {
        mAuthFailedTimes++;

        if (!mIntruRecAttemptsPrefs.
                getBoolean(IntrusionRecordSettingActivity.KEY_INTRU_REC_ENABLED, true)) {
            return;
        }

        if (mAuthFailedTimes >= mIntruRecAttemptsPrefs.
                getInt(IntrusionRecordSettingActivity.KEY_INTRU_REC_ATTEMPTS_TIMES, 2)) {
            mCameraView.takePicture();
            mAuthFailedTimes = 0;
        }
    }
}
