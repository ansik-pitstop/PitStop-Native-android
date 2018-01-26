package com.pitstop.bluetooth.communicator;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by Paul Soladoye on 19/04/2016.
 */
public abstract class BluetoothCommand {
    public void execute(BluetoothGatt gatt){ }
}
