package com.pitstop.ui.mainFragments;

import com.pitstop.bluetooth.BluetoothAutoConnectService;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface BluetoothFragmentCallback {
    void onDeviceConnected(BluetoothAutoConnectService bluetoothAutoConnectService);
    void onDeviceDisconnected();
}
