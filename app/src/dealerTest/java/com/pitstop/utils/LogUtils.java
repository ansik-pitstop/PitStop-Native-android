package com.pitstop.utils;

import android.util.Log;

import com.pitstop.BuildConfig;

/**
 * Created by Ben Wu on 2016-06-22.
 */
public class LogUtils {

    private static boolean DEBUG = BuildConfig.DEBUG;

    public static void LOGV(String tag, String message) {
        if(DEBUG) {
            Log.v(tag, message);
        }
    }

    public static void LOGD(String tag, String message) {
        if(DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void LOGI(String tag, String message) {
        if(DEBUG) {
            Log.i(tag, message);
        }
    }

    public static void LOGW(String tag, String message) {
        if(DEBUG) {
            Log.w(tag, message);
        }
    }

    public static void LOGE(String tag, String message) {
        if(DEBUG) {
            Log.e(tag, message);
        }
    }

    public static void LOGA(String tag, String message) {
        if(DEBUG) {
            Log.wtf(tag, message);
        }
    }

}
