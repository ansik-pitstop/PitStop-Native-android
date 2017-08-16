package com.pitstop.bluetooth.handler;

import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public class RtcDataHandler implements BluetoothDataHandler{

    private BluetoothDataHandlerManager bluetoothDataHandlerManager;
    private long terminalRtcTime = -1;
    private String currentDeviceId = "";

    public RtcDataHandler(BluetoothDataHandlerManager bluetoothDataHandlerManager){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
    }

    public void handleRtcData(long rtc, String deviceId){

        bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_RTC_GOT,deviceId
                ,String.valueOf(rtc));

        terminalRtcTime = rtc;
        //Check if device needs to sync rtc time
        final long YEAR = 32000000;
        long currentTimeInMillis = System.currentTimeMillis();
        long currentTime = currentTimeInMillis / 1000;
        long diff = currentTime - rtc;

        //Sync if difference is greater than a year
        if (diff > YEAR){
            bluetoothDataHandlerManager.requestDeviceSync();
            terminalRtcTime = currentTimeInMillis;
            bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_SYNCING);
        }
    }

    public long getTerminalRtcTime(){
        return terminalRtcTime;
    }

    @Override
    public void setDeviceId(String deviceId) {
        this.currentDeviceId = deviceId;
    }

}
