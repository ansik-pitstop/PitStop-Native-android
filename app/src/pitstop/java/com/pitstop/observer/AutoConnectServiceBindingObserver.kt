package com.pitstop.observer

import com.pitstop.bluetooth.BluetoothService

/**
 * Created by ishan on 2017-11-07.
 */
interface AutoConnectServiceBindingObserver {
    fun onServiceBinded(bluetoothService: BluetoothService)
}