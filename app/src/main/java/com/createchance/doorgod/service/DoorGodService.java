package com.createchance.doorgod.service;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.createchance.doorgod.adapter.AppInfo;
import com.createchance.doorgod.database.ProtectedApplication;
import com.createchance.doorgod.ui.DoorGodActivity;
import com.createchance.doorgod.util.LogUtil;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        private void removeAllProtectedApp() {
            DataSupport.deleteAll(ProtectedApplication.class);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mPm = getPackageManager();

        initAppList();

        // create database.
        Connector.getDatabase();

        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        mProtectedAppList = mBinder.getProtectedAppList();

        // start working thread.
        mAppStartWatchThread = new AppStartWatchThread();
        mAppStartWatchThread.start();
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
        LogUtil.e(TAG, "Service died, so no apps can be protected!");
    }

    private void initAppList() {
        LogUtil.d(TAG, "Init installed app list.");
        List<ApplicationInfo> applications = mPm
                .getInstalledApplications(0);
        Collections.sort(applications,
                new ApplicationInfo.DisplayNameComparator(mPm));

        for (ApplicationInfo application:applications) {
            AppInfo info = new AppInfo();
            info.setAppIcon(application.loadIcon(mPm));
            info.setAppName((String) application.loadLabel(mPm));
            info.setAppPackageName(application.packageName);
            mAppInfoList.add(info);
        }
    }

    private void checkIfNeedProtection() {
        long time = System.currentTimeMillis();
        List<UsageStats> usageStatsList = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, time - 2000, time);

        if (usageStatsList != null && !usageStatsList.isEmpty()) {
            SortedMap<Long, UsageStats> usageStatsMap = new TreeMap<>();
            for (UsageStats usageStats : usageStatsList) {
                usageStatsMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!usageStatsMap.isEmpty()) {
                String topPackageName = usageStatsMap.get(usageStatsMap.lastKey()).getPackageName();
                LogUtil.d(TAG, "starting: " + topPackageName);
                if (mProtectedAppList.contains(topPackageName)) {
                    Intent intent = new Intent(this, DoorGodActivity.class);
                    startActivity(intent);
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
                    Thread.sleep(1000);
                    checkIfNeedProtection();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
