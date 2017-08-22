package com.pitstop.observer;

import java.util.HashMap;

/**
 * Created by Karol Zdebel on 7/21/2017.
 */

public interface BluetoothDtcObserver extends Observer {

    //DTC data retrieved from the device that is currently being used
    void onGotDtc(HashMap<Boolean,String> dtc);
    void onErrorGettingDtc();
}
