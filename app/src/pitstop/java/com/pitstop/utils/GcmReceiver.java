package com.pitstop.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pitstop.BuildConfig;

import io.smooch.core.Smooch;

/**
 * Created by Ben Wu on 2016-06-22.
 */
public class GcmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String registrationId = intent.getStringExtra("registration_id");
        if(BuildConfig.DEBUG) {
            Logger.getInstance().LOGE("Action", intent.getAction());
            Logger.getInstance().LOGE("Registration", ""+intent.getStringExtra("registration_id"));
            Logger.getInstance().LOGE("Extras", intent.getExtras().toString());
        }

        if(intent.getAction().equals("com.google.android.c2dm.intent.REGISTRATION")) {
            if(registrationId != null && !registrationId.startsWith("|ID|")) {
                Logger.getInstance().LOGE("FCM", "Setting Smooch FCM: " + registrationId);
                Smooch.setFirebaseCloudMessagingToken(registrationId);
            } else {
                abortBroadcast();
            }
        }
    }
}
