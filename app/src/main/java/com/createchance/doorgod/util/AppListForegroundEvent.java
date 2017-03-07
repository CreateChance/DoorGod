package com.createchance.doorgod.util;

/**
 * AppListForegroundEvent
 */

public class AppListForegroundEvent {
    private boolean isForeground = false;

    public AppListForegroundEvent(boolean isForeground) {
        this.isForeground = isForeground;
    }

    public boolean isForeground() {
        return isForeground;
    }
}
