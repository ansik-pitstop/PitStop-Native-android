package com.pitstop.observer;

import com.pitstop.bluetooth.dataPackages.DtcPackage;

/**
 * Created by Karol Zdebel on 7/21/2017.
 */

public interface BluetoothScanObservable extends Subject {

    //Invoked if dtc data has been received from the device
    void notifyDtcData(DtcPackage dtcPackage);
}
