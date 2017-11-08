package com.pitstop.observer

import com.pitstop.bluetooth.BluetoothAutoConnectService

/**
 * Created by ishan on 2017-11-07.
 */
interface AutoConnectServiceBindingObserver {
    fun onServiceBinded(bluetoothAutoConnectService: BluetoothAutoConnectService)
}