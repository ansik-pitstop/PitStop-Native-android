package com.pitstop.ui;

import com.castel.obd.bluetooth.ObdManager;


public interface BluetoothPresenter extends BasePresenter, ObdManager.IBluetoothDataListener {

    void bindBluetoothService();

    void unbindBluetoothService();

}
