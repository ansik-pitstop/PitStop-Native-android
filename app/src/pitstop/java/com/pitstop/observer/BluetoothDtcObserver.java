package com.pitstop.observer;

import com.pitstop.bluetooth.dataPackages.DtcPackage;

/**
 * Created by Karol Zdebel on 7/21/2017.
 */

public interface BluetoothDtcObserver extends Observer {

    //DTC data retrieved from the device that is currently being used
    void onGotDtc(DtcPackage dtc);
    void onErrorGettingDtc();
}
