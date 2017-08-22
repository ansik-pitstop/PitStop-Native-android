package com.pitstop.observer;

import java.util.Set;

/**
 * Created by Karol Zdebel on 7/21/2017.
 */

public interface BluetoothDtcObserver extends Observer {

    //DTC data retrieved from the device that is currently being used
    void onGotDtc(Set<String> dtc);
    void onErrorGettingDtc();
}
