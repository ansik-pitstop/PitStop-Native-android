package com.pitstop.bluetooth;

import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public class RtcDataHandler {

    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private BluetoothMixpanelTracker bluetoothMixpanelTracker;
    private long terminalRtcTime = -1;

    public RtcDataHandler(BluetoothConnectionObservable bluetoothConnectionObservable
            , BluetoothMixpanelTracker bluetoothMixpanelTracker){

        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
        this.bluetoothMixpanelTracker = bluetoothMixpanelTracker;
    }

    public void handleRtcData(long rtc, String deviceId){

        bluetoothMixpanelTracker.trackBluetoothEvent(MixpanelHelper.BT_RTC_GOT,deviceId
                ,String.valueOf(rtc));

        terminalRtcTime = rtc;
        //Check if device needs to sync rtc time
        final long YEAR = 32000000;
        long currentTimeInMillis = System.currentTimeMillis();
        long currentTime = currentTimeInMillis / 1000;
        long diff = currentTime - rtc;

        //Sync if difference is greater than a year
        if (diff > YEAR){
            bluetoothConnectionObservable.requestDeviceSync();
            terminalRtcTime = currentTimeInMillis;
            bluetoothMixpanelTracker.trackBluetoothEvent(MixpanelHelper.BT_SYNCING);
        }
    }

    public long getTerminalRtcTime(){
        return terminalRtcTime;
    }
}
