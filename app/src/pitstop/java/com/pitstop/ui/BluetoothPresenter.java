package com.pitstop.ui;

import com.castel.obd.bluetooth.ObdManager;
import com.pitstop.bluetooth.BluetoothAutoConnectService;


public interface BluetoothPresenter extends BasePresenter, ObdManager.IBluetoothDataListener {

    void bindBluetoothService();

    void onServiceBound(BluetoothAutoConnectService service);

    void unbindBluetoothService();

    void checkBluetoothService();
}
