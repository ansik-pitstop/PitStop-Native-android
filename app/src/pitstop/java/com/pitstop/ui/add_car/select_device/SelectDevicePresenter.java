package com.pitstop.ui.add_car.select_device;

import android.util.Log;

public class SelectDevicePresenter {

    private final String TAG = getClass().getSimpleName();
    private SelectDeviceView view;

    public void subscribe(SelectDeviceView view) {
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    public void connectToBluetoothDevice() {
        Log.d(TAG,"connectToBluetoothDevice()");
        view.loadConnectToBluetoothView();
    }

    public void addSIMCardDevice() {
        Log.d(TAG,"addSIMCardDevice()");
        view.loadInsertVinView();
    }
}