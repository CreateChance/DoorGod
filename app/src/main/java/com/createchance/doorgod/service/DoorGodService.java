package com.createchance.doorgod.service;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.createchance.doorgod.adapter.AppInfo;
import com.createchance.doorgod.database.LockInfo;
import com.createchance.doorgod.database.ProtectedApplication;
import com.createchance.doorgod.fingerprint.CryptoObjectHelper;
import com.createchance.doorgod.fingerprint.MyAuthCallback;
import com.createchance.doorgod.ui.DoorGodActivity;
import com.createchance.doorgod.util.FingerprintAuthRequest;
import com.createchance.doorgod.util.FingerprintAuthResponse;
import com.createchance.doorgod.util.LogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Door God Service.
 */

public class DoorGodService extends Service {

    private static final String TAG = "DoorGodService";

    private List<AppInfo> mAppInfoList = new ArrayList<>();

    private PackageManager mPm;

    private UsageStatsManager mUsageStatsManager;

    private AppStartWatchThread mAppStartWatchThread;

    private List<String> mProtectedAppList;

    private String currentLockedApp;

    private List<String> mUnlockedAppList = new ArrayList<>();

    private int lockType = -1;

    //private FingerprintManagerCompat fingerprintManager;
    private MyAuthCallback myAuthCallback = null;
    private CancellationSignal cancellationSignal = null;

    private boolean isScreenOn = true;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            LogUtil.d(TAG, "action: " + action);
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mUnlockedAppList.clear();

                isScreenOn = false;
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                isScreenOn = true;
            }
        }
    };

    private ServiceBinder mBinder = new ServiceBinder();
    public class ServiceBinder extends Binder {
        public List<AppInfo> getAppList() {
            return mAppInfoList;
        }

        public List<String> getProtectedAppList() {
            List<String> applist = new ArrayList<>();

            List<ProtectedApplication> applicationList = DataSupport.findAll(ProtectedApplication.class);
            for (ProtectedApplication application: applicationList) {
                applist.add(application.getPackageName());
            }

            return applist;
        }

        public void addProtectedApp(List<String> list) {
            // remove all the apps first.
            removeAllProtectedApp();
            // then add protected apps.
            for (String app:list) {
                LogUtil.d(TAG, "add app: " + app);
                ProtectedApplication application = new ProtectedApplication();
                application.setPackageName(app);
                application.save();
            }

            // update protected app list.
            mProtectedAppList = getProtectedAppList();
        }

        public void addUnlockedApp() {
            LogUtil.d(TAG, "add unlock app: " + currentLockedApp);
            mUnlockedAppList.add(currentLockedApp);
            currentLockedApp = null;
        }

        // save lock info: lock string and type
        public void saveLockInfo(String lockString, int type) {
            DataSupport.deleteAll(LockInfo.class);

            LockInfo info = new LockInfo();
            info.setLockString(lockString);
            info.setLockType(type);
            info.save();
            lockType = type;
        }

        public int getLockType() {
            if (lockType == -1) {
                LockInfo info = DataSupport.findFirst(LockInfo.class);
                if (info != null) {
                    lockType = info.getLockType();
                } else {
                    LogUtil.d(TAG, "info is null.");
                }
            }

            return lockType;
        }

        public void startFingerprintAuth() {
            EventBus.getDefault().post(new FingerprintAuthRequest());
        }

        public boolean hasFingerprintHardware() {
            boolean detected = FingerprintManagerCompat.from(DoorGodService.this).isHardwareDetected();

            LogUtil.d(TAG, "hasFingerprintHardware: " + detected);

            return detected;
        }

        public boolean isFingerprintEnrolled() {
            boolean enrolled = FingerprintManagerCompat.from(DoorGodService.this).hasEnrolledFingerprints();

            LogUtil.d(TAG, "isFingerprintEnrolled: " + enrolled);

            return enrolled
                    ;
        }

        public void cancelFingerprint() {
            LogUtil.d(TAG, "Request for canceling fingerprint auth.");
            if (cancellationSignal != null) {
                // cancel fingerprint auth here.
                cancellationSignal.cancel();
            }
        }

        private void removeAllProtectedApp() {
            DataSupport.deleteAll(ProtectedApplication.class);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // register event bus.
        EventBus.getDefault().register(this);

        try {
            myAuthCallback = new MyAuthCallback();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mPm = getPackageManager();

        initAppList();

        // create database.
        Connector.getDatabase();

        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        mProtectedAppList = mBinder.getProtectedAppList();

        // start working thread.
        mAppStartWatchThread = new AppStartWatchThread();
        mAppStartWatchThread.start();

        // register screen state listener.
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.d(TAG, "Service bind.");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // unregister event bus.
        EventBus.getDefault().unregister(this);

        unregisterReceiver(mReceiver);
        LogUtil.e(TAG, "Service died, so no apps can be protected!");
    }

    /*
     * Fingerprint auth handle function.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFingerprintAuth(FingerprintAuthRequest req) {
        // start fingerprint auth here.
        FingerprintManagerCompat manager = FingerprintManagerCompat.from(DoorGodService.this);
        if (manager.isHardwareDetected() && manager.hasEnrolledFingerprints()) {
            try {
                CryptoObjectHelper cryptoObjectHelper = new CryptoObjectHelper();
                cancellationSignal = new CancellationSignal();
                LogUtil.d(TAG, "Now we start listen for finger print auth.");
                manager.authenticate(cryptoObjectHelper.buildCryptoObject(), 0,
                        cancellationSignal, myAuthCallback, null);
            } catch (Exception e) {
                LogUtil.d(TAG, "Fingerprint exception happens.");
                e.printStackTrace();
                // send this error.
                EventBus.getDefault().
                        post(new FingerprintAuthResponse(FingerprintAuthResponse.MSG_AUTH_ERROR));
            }
        }
    }

    private void initAppList() {
        LogUtil.d(TAG, "Init installed app list.");
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> homeApps = mPm.queryIntentActivities(intent, 0);

        // sort the results
        Collections.sort(homeApps, new ResolveInfo.DisplayNameComparator(mPm));

        Set<String> packageNameSet = new HashSet<>();
        for (ResolveInfo info : homeApps) {
            // skip ourselves
            if (info.activityInfo.packageName.equals(getPackageName())) {
                continue;
            }

            if (!packageNameSet.contains(info.activityInfo.packageName)) {
                packageNameSet.add(info.activityInfo.packageName);
                AppInfo appInfo = new AppInfo();
                appInfo.setAppPackageName(info.activityInfo.packageName);
                appInfo.setAppName((String) info.activityInfo.applicationInfo.loadLabel(mPm));
                appInfo.setAppIcon(info.activityInfo.applicationInfo.loadIcon(mPm));
                mAppInfoList.add(appInfo);
            }
        }
    }

    private void checkIfNeedProtection() {
        long time = System.currentTimeMillis();
        List<UsageStats> usageStatsList = mUsageStatsManager.
                queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 2000, time);

        if (usageStatsList != null && !usageStatsList.isEmpty() && isScreenOn) {
            SortedMap<Long, UsageStats> usageStatsMap = new TreeMap<>();
            for (UsageStats usageStats : usageStatsList) {
                usageStatsMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!usageStatsMap.isEmpty()) {
                String topPackageName = usageStatsMap.get(usageStatsMap.lastKey()).getPackageName();
                if (mProtectedAppList.contains(topPackageName)
                        && !mUnlockedAppList.contains(topPackageName)) {
                    LogUtil.d(TAG, "protecting: " + topPackageName);
                    Intent intent = new Intent(DoorGodService.this, DoorGodActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    DoorGodService.this.startActivity(intent);
                    currentLockedApp = topPackageName;
                }
            }
        }
    }

    private class AppStartWatchThread extends Thread {
        @Override
        public void run() {
            super.run();

            while (true) {
                try {
                    Thread.sleep(500);
                    checkIfNeedProtection();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
