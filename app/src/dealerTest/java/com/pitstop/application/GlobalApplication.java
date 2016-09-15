package com.pitstop.application;

import android.app.Application;
import android.support.multidex.MultiDex;
import android.util.Log;

/**
 * Created by Ansik on 12/28/15.
 */
public class GlobalApplication extends Application {

    private static String TAG = GlobalApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        MultiDex.install(this);
    }
}
