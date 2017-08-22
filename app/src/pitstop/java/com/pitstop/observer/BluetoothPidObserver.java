package com.pitstop.observer;

import java.util.HashMap;

/**
 * Created by Karol Zdebel on 8/18/2017.
 */

public interface BluetoothPidObserver extends Observer {
    void onGotAllPid(HashMap<String,String> allPid);
    void onErrorGettingAllPid();
}
