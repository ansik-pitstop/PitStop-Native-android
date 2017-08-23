package com.pitstop.bluetooth.handler;

import android.util.Log;

import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public class RtcDataHandler{
    private final String TAG = getClass().getSimpleName();

    private BluetoothDataHandlerManager bluetoothDataHandlerManager;

    public RtcDataHandler(BluetoothDataHandlerManager bluetoothDataHandlerManager){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
    }

    public void handleRtcData(long rtc, String deviceId){

        Log.d(TAG,"handleRtcData() rtc:"+rtc+", deviceId:"+deviceId);

        bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_RTC_GOT,deviceId
                ,String.valueOf(rtc));

        //Check if device needs to sync rtc time
        final long YEAR = 32000000;
        long currentTimeInMillis = System.currentTimeMillis();
        long currentTime = currentTimeInMillis / 1000;
        long diff = currentTime - rtc;

        //Sync if difference is greater than a year
        if (diff > YEAR){
            bluetoothDataHandlerManager.requestDeviceSync();
            bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_SYNCING);
        }
    }

}
