package com.pitstop.ui;

import com.pitstop.bluetooth.BluetoothService;
import com.pitstop.observer.BluetoothDtcObserver;
import com.pitstop.observer.Device215BreakingObserver;
import com.pitstop.observer.BluetoothConnectionObserver;

/**
 * BluetoothPresenter is for Activities that need Bluetooth data. <br>
 * A BluetoothPresenter does:
 * 1. Keep a reference of BluetoothAutoConnectService, so you can explicitly ask for specific data like VIN, DTC <br>
 * 2. Act as a bluetooth data callback for BluetoothAutoConnectService (IBluetoothDataListener)
 */
public interface BluetoothPresenter extends BasePresenter, BluetoothConnectionObserver
        , Device215BreakingObserver, BluetoothDtcObserver {

    void bindBluetoothService();

    void onServiceBound(BluetoothService service);

    void unbindBluetoothService();

    void onServiceUnbind();

    void checkBluetoothService();
}
