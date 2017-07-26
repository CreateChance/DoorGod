package com.createchance.doorgod.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.createchance.doorgod.R;
import com.createchance.doorgod.adapter.AppInfo;
import com.createchance.doorgod.database.LockInfo;
import com.createchance.doorgod.database.ProtectedApplication;
import com.createchance.doorgod.database.TrustedWifi;
import com.createchance.doorgod.fingerprint.CryptoObjectHelper;
import com.createchance.doorgod.fingerprint.MyAuthCallback;
import com.createchance.doorgod.ui.DoorGodActivity;
import com.createchance.doorgod.util.AppListForegroundEvent;
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

    private PackageManager mPm;

    private UsageStatsManager mUsageStatsManager;

    private WifiManager mWifiManager;

    private List<String> mSavedWifiList = new ArrayList<>();
    private List<String> mTrustedWifiList = new ArrayList<>();
    private String mConnectedWifi;

    private AppStartWatchThread mAppStartWatchThread;

    private List<AppInfo> mProtectedAppList = new ArrayList<>();

    private List<AppInfo> mUnprotectedAppList = new ArrayList<>();

    private Set<String> mCheckList = new HashSet<>();

    private String currentLockedApp;

    private List<String> mUnlockedAppList = new ArrayList<>();

    private int lockType = -1;

    //private FingerprintManagerCompat fingerprintManager;
    private MyAuthCallback myAuthCallback = null;
    private CancellationSignal cancellationSignal = null;

    private boolean isAppListInForeground = false;

    private boolean isScreenOn = true;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            LogUtil.d(TAG, "action: " + action);
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                mUnlockedAppList.clear();

                isScreenOn = false;

                if (isAppListInForeground) {
                    isAppListInForeground = false;
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_HOME);
                    startActivity(i);
                }

            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                isScreenOn = true;
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    initConnectedWifi();
                } else if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                    mConnectedWifi = null;
                }
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (state == WifiManager.WIFI_STATE_ENABLED) {
                    initSavedWifiList();
                }
            }
        }
    };

    private ServiceBinder mBinder = new ServiceBinder();
    public class ServiceBinder extends Binder {
        public List<AppInfo> getProtectedAppList() {
            return mProtectedAppList;
        }

        public List<AppInfo> getUnProtectedAppList() {
            return mUnprotectedAppList;
        }

        public int markToProtect(AppInfo appInfo) {
            LogUtil.d(TAG, "mark to protect: " + appInfo);

            mUnprotectedAppList.remove(appInfo);
            mProtectedAppList.add(appInfo);

            return mProtectedAppList.size();
        }

        public int markToUnprotect(AppInfo appInfo) {
            LogUtil.d(TAG, "mark to unprotect: " + appInfo);

            mProtectedAppList.remove(appInfo);
            mUnprotectedAppList.add(appInfo);

            return mUnprotectedAppList.size();
        }

        public void saveProtectList() {
            removeAllProtectedApp();
            mCheckList.clear();

            for (AppInfo info : mProtectedAppList) {
                ProtectedApplication app = new ProtectedApplication();
                app.setPackageName(info.getAppPackageName());
                app.save();
                // create new check list
                mCheckList.add(info.getAppPackageName());
            }
        }

        public void discardProtectListSettings() {
            mProtectedAppList.clear();
            mUnprotectedAppList.clear();
            // do init again if user discard settings.
            initAppList();
        }

        public boolean isProtectedAppListChanged() {
            List<ProtectedApplication> protectedList = DataSupport.findAll(ProtectedApplication.class);

            if (protectedList.size() == mProtectedAppList.size()) {
                for (int i = 0; i < mProtectedAppList.size(); i++) {
                    ProtectedApplication app = new ProtectedApplication(mProtectedAppList.get(i).getAppPackageName());
                    if (!protectedList.contains(app)) {
                        return true;
                    }
                }
            } else {
                return true;
            }

            return false;
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

        public boolean isWifiEnabled() {
            return mWifiManager.isWifiEnabled();
        }

        public List<String> getSavedWifiList() {
            initSavedWifiList();

            return mSavedWifiList;
        }

        public void setTrustedWifi(String ssid) {
            TrustedWifi trustedWifi = new TrustedWifi();
            trustedWifi.setSsid(ssid);
            trustedWifi.save();

            mTrustedWifiList.add(ssid);
        }

        public void removeTrustedWifi(String ssid) {
            DataSupport.deleteAll(TrustedWifi.class, "ssid = ?", ssid);

            mTrustedWifiList.remove(ssid);
        }

        public List<String> getTrustedWifi() {
            initTrustedWifiList();

            return mTrustedWifiList;
        }

        // return connected wifi ssid, null if wifi disconnected.
        public String getConnectedWifiSsid() {
            return mConnectedWifi;
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

            return enrolled;
        }

        public void cancelFingerprint() {
            LogUtil.d(TAG, "Request for canceling fingerprint auth.");
            if (cancellationSignal != null) {
                // cancel fingerprint auth here.
                cancellationSignal.cancel();
            }
        }

        public String getCurrentAppName() {
            for (AppInfo info : mProtectedAppList) {
                if (info.getAppPackageName().equals(currentLockedApp)) {
                    return info.getAppName();
                }
            }

            // otherwise it is ourselves
            return getString(R.string.app_name);
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

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        initTrustedWifiList();
        if (mWifiManager.isWifiEnabled()) {
            initSavedWifiList();
            initConnectedWifi();
        }

        mProtectedAppList = mBinder.getProtectedAppList();

        // start working thread.
        mAppStartWatchThread = new AppStartWatchThread();
        mAppStartWatchThread.start();

        // register screen state listener.
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, filter);

        makeForeground();
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

    /*
     * App list activity is foreground handle.
     */
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onAppListForeground(AppListForegroundEvent event) {
        if (event.isForeground()) {
            isAppListInForeground = true;
            // We quit foreground cause we have foreground activity now.
            stopForeground(true);
        } else {
            // we have to make us foreground now.
            makeForeground();
        }
    }

    private void initAppList() {
        LogUtil.d(TAG, "Init protected and unprotected app list.");
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> homeApps = mPm.queryIntentActivities(intent, 0);

        // sort the results
        Collections.sort(homeApps, new ResolveInfo.DisplayNameComparator(mPm));

        // get protected app list from database.
        List<ProtectedApplication> protectedList = DataSupport.findAll(ProtectedApplication.class);

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

                if (protectedList.contains(new ProtectedApplication(info.activityInfo.packageName))) {
                    mProtectedAppList.add(appInfo);
                    // init check list
                    mCheckList.add(appInfo.getAppPackageName());
                } else {
                    mUnprotectedAppList.add(appInfo);
                }
            }
        }
    }

    private void initSavedWifiList() {
        mSavedWifiList.clear();
        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration configuration : list) {
            mSavedWifiList.add(configuration.SSID);
        }

        // remove old ssid cause it is removed by user.
        for (String ssid : mTrustedWifiList) {
            if (!mSavedWifiList.contains(ssid)) {
                mTrustedWifiList.remove(ssid);
                DataSupport.deleteAll(TrustedWifi.class, "ssid = ?", ssid);
            }
        }
    }

    private void initTrustedWifiList() {
        mTrustedWifiList.clear();
        List<TrustedWifi> trustedWifis = DataSupport.findAll(TrustedWifi.class);

        for (TrustedWifi wifi : trustedWifis)  {
            mTrustedWifiList.add(wifi.getSsid());
        }
    }

    private void initConnectedWifi() {
        mConnectedWifi = mWifiManager.getConnectionInfo().getSSID();
        LogUtil.d(TAG, "connected wifi: " + mConnectedWifi);
    }

    private void checkIfNeedProtection() {

        if (mTrustedWifiList.contains(mConnectedWifi)) {
            // if we are connected to trusted wifi, so do not check.
            return;
        }

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
                if (mCheckList.contains(topPackageName)
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

    private void makeForeground() {
        // start this service in foreground
        Intent intent = new Intent(this, DoorGodActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.service_foreground_notification_title))
                .setSmallIcon(R.drawable.ic_lock_white_48dp)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .build();
        startForeground(1, notification);
    }
}
