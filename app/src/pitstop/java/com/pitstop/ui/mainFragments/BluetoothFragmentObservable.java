package com.pitstop.ui.mainFragments;

import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.observer.Observer;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothFragmentObservable extends Observer{
    void onDeviceConnected(BluetoothAutoConnectService bluetoothAutoConnectService);
    void onDeviceDisconnected();
}
