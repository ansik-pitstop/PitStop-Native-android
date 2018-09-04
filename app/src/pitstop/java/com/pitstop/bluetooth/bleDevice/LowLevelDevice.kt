package com.pitstop.bluetooth.bleDevice

import android.bluetooth.BluetoothDevice

/**
 * Created by Karol Zdebel on 9/2/2018.
 */
interface LowLevelDevice: AbstractDevice {
    fun clearDtcs(): Boolean         //215B, 212B, ELM327, RVD(maybe)
    fun onConnectionStateChange(state: Int)         //ELM327, 215B, 212B
    fun connectToDevice(device: BluetoothDevice): Boolean     //215B, 212B, ELM327
}