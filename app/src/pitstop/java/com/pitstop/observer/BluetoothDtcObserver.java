package com.pitstop.observer;

import com.pitstop.bluetooth.dataPackages.DtcPackage;

/**
 * Created by Karol Zdebel on 7/20/2017.
 */

public interface BluetoothDtcObserver {
    void onGotDtc(DtcPackage dtcPackage);
}
