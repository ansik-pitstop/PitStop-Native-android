package com.pitstop.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.pitstop.bluetooth.dataPackages.PidPackage;

/**
 * Created by Karol Zdebel on 9/19/2017.
 */

public class BluetoothDataVisualizer {

    private static final String TAG = BluetoothDataVisualizer.class.getSimpleName();

    public static void visualizePidReceived(PidPackage pidPackage, Context context){
        if (pidPackage == null){
            Log.d(TAG,"visualizePidReceived() pidPackage = null");
            Toast.makeText(context,"NULL pid values received",Toast.LENGTH_LONG).show();
        }else{
            Log.d(TAG,"visualizePidReceived() pidPackage.pids: "+pidPackage.pids.keySet());
            String rpm = pidPackage.pids.get("210C");
            Toast.makeText(context,"Pid values received, RPM: "+rpm,Toast.LENGTH_LONG).show();
        }
    }

    public static void visualizePidDataSent(boolean success, Context context){
        Log.d(TAG,"visualizePidDataSent() success ? "+success);
        if (success)
            Toast.makeText(context,"Pid values sent to server successfully",Toast.LENGTH_LONG)
                    .show();
        else
            Toast.makeText(context,"Pid values failed to send to server: ",Toast.LENGTH_LONG)
                    .show();
    }
}
