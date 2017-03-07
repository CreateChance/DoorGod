package com.createchance.doorgod.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.createchance.doorgod.util.LogUtil;

/**
 *
 */

public class BootUpReceiver extends BroadcastReceiver {

    private static final String TAG = "BootUpReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        LogUtil.d(TAG, "action: " + action);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            LogUtil.d(TAG, "Boot complete, starting door god service.");
            Intent i = new Intent(context, DoorGodService.class);
            context.startService(i);
        }
    }
}
