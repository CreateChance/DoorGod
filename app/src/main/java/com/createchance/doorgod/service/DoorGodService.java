package com.createchance.doorgod.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.createchance.doorgod.adapter.AppInfo;
import com.createchance.doorgod.database.ProtectedApplication;
import com.createchance.doorgod.util.LogUtil;

import org.litepal.crud.DataSupport;
import org.litepal.tablemanager.Connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Door God Service.
 */

public class DoorGodService extends Service {

    private static final String TAG = "DoorGodService";

    private List<AppInfo> mAppInfoList = new ArrayList<>();

    private PackageManager mPm;

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
}
