package com.pitstop.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.smooch.core.Smooch;

/**
 * Created by Ben Wu on 2016-06-22.
 */
public class GcmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String registrationId = intent.getStringExtra("registration_id");

        if(intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
            if(registrationId != null && !registrationId.startsWith("|ID|")) {
                Smooch.setFirebaseCloudMessagingToken(registrationId);
            } else {
                abortBroadcast();
            }
        }
    }
}
