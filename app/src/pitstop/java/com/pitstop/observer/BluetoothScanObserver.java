package com.pitstop.observer;

import com.pitstop.bluetooth.dataPackages.DtcPackage;

/**
 * Created by Karol Zdebel on 7/20/2017.
 */

public interface BluetoothScanObserver extends Observer {
    void onGotDtc(DtcPackage dtcPackage);
}
