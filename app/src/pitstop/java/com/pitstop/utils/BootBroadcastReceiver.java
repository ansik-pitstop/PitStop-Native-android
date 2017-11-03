package com.pitstop.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.ui.main_activity.MainActivity;

/**
 * Created by David Liu on 11/30/2015.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        // BOOT_COMPLETED start CarService
        if (intent.getAction().equals(ACTION)) {
            //CarService
            Log.i(MainActivity.Companion.getTAG(),"Starting auto connect service from boot broadcast receiver");
            Intent serviceIntent = new Intent(context, BluetoothAutoConnectService.class);
            context.startService(serviceIntent);
        }
    }
}
