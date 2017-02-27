package com.createchance.doorgod.adapter;

import android.graphics.drawable.Drawable;

/**
 * App info bean.
 */

public class AppInfo {
    private String appName;

    private Drawable appIcon;

    private String appPackageName;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    public String getAppPackageName() {
        return appPackageName;
    }

    public void setAppPackageName(String appPackageName) {
        this.appPackageName = appPackageName;
    }
}
