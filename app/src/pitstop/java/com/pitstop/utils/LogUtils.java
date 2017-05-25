package com.pitstop.utils;

import android.content.Context;
import android.util.Log;

import com.pitstop.BuildConfig;
import com.pitstop.database.LocalDebugMessageAdapter;
import com.pitstop.models.DebugMessage;

public class LogUtils {

    private static boolean DEBUG = BuildConfig.DEBUG;
    private static boolean NOT_RELEASE = !BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE);
    private static boolean NOT_BETA = !BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA);

    public static void debugLogV(String tag, String message, boolean showLogcat, int type, Context context) {
        if(false) {
            if (showLogcat) {
                Log.v(tag, message);
            }
            new LocalDebugMessageAdapter(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_V));
        }
    }

    public static void debugLogD(String tag, String message, boolean showLogcat, int type, Context context) {
        if(false) {
            if (showLogcat) {
                Log.d(tag, message);
            }
            new LocalDebugMessageAdapter(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_D));
        }
    }

    public static void debugLogI(String tag, String message, boolean showLogcat, int type, Context context) {
        if(false) {
            if (showLogcat) {
                Log.i(tag, message);
            }
            new LocalDebugMessageAdapter(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_I));
        }
    }

    public static void debugLogW(String tag, String message, boolean showLogcat, int type, Context context) {
        if(false) {
            if (showLogcat) {
                Log.w(tag, message);
            }
            new LocalDebugMessageAdapter(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_W));
        }
    }

    public static void debugLogE(String tag, String message, boolean showLogcat, int type, Context context) {
        if(false) {
            if (showLogcat) {
                Log.e(tag, message);
            }
            new LocalDebugMessageAdapter(context).addMessage(
                    new DebugMessage(System.currentTimeMillis(), tag + ": " + message, type, DebugMessage.LEVEL_E));
        }
    }

    public static void LOGV(String tag, String message) {
        if(false) {
            Log.v(tag, message);
        }
    }

    public static void LOGD(String tag, String message) {
        if(false) {
            Log.d(tag, message);
        }
    }

    public static void LOGI(String tag, String message) {
        if(false) {
            Log.i(tag, message);
        }
    }

    public static void LOGW(String tag, String message) {
        if(false) {
            Log.w(tag, message);
        }
    }

    public static void LOGE(String tag, String message) {
        if(false) {
            Log.e(tag, message);
        }
    }

    public static void LOGA(String tag, String message) {
        if(false) {
            Log.wtf(tag, message);
        }
    }

}