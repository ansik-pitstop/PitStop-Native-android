package com.pitstop.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.facebook.stetho.common.LogUtil;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.DebugMessage;

/**
 * Created by Karol Zdebel on 9/19/2017.
 */

public class BluetoothDataVisualizer {

    private static final String TAG = BluetoothDataVisualizer.class.getSimpleName();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static boolean pidDataSentVisible = false;


}
