package com.pitstop.observer;

import com.pitstop.bluetooth.dataPackages.PidPackage;

/**
 * Created by Karol Zdebel on 8/18/2017.
 */

public interface BluetoothPidObserver extends Observer {
    void onGotAllPid(PidPackage pidPackage);
}
