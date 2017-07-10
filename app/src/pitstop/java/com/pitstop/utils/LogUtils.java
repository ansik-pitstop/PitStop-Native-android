package com.pitstop.utils;

import android.content.Context;
import android.util.Log;

import com.pitstop.BuildConfig;
import com.pitstop.database.LocalDebugMessageHelper;
import com.pitstop.models.DebugMessage;

public class LogUtils {

    private static boolean DEBUG = BuildConfig.DEBUG;
    private static boolean NOT_RELEASE = !BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE);
    private static boolean NOT_BETA = !BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA);

    public static void debugLogV(String tag, String message, boolean showLogcat, int type, Context context) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.v(tag, message);
            }
            new LocalDebugMessageHelper(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_V));
        }
    }

    public static void debugLogD(String tag, String message, boolean showLogcat, int type, Context context) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.d(tag, message);
            }
            new LocalDebugMessageHelper(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_D));
        }
    }

    public static void debugLogI(String tag, String message, boolean showLogcat, int type, Context context) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.i(tag, message);
            }
            new LocalDebugMessageHelper(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_I));
        }
    }

    public static void debugLogW(String tag, String message, boolean showLogcat, int type, Context context) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.w(tag, message);
            }
            new LocalDebugMessageHelper(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_W));
        }
    }

    public static void debugLogE(String tag, String message, boolean showLogcat, int type, Context context) {
        if(NOT_RELEASE && NOT_BETA) {
            if (showLogcat) {
                Log.e(tag, message);
            }
            new LocalDebugMessageHelper(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_E));
        }
    }

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