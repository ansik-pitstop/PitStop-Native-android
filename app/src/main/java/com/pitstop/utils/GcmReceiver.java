package com.pitstop.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.parse.PushService;

import io.smooch.core.GcmService;
import io.smooch.core.Smooch;

/**
 * Created by Ben Wu on 2016-06-22.
 */
public class GcmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        intent.setClass(context, PushService.class);

        if(intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
            Smooch.setGoogleCloudMessagingToken(intent.getStringExtra("registration_id"));
        } else {
            GcmService.triggerSmoochGcm(intent.getExtras(), context);
        }

        //context.startService(intent);
    }
}
