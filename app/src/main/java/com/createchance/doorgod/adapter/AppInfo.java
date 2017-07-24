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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof AppInfo)) {
            return false;
        }

        // if package name equals, that means the same app.
        if (!((AppInfo) obj).getAppPackageName().equals(this.appPackageName)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "appName='" + appName + '\'' +
                ", appIcon=" + appIcon +
                ", appPackageName='" + appPackageName + '\'' +
                '}';
    }
}
