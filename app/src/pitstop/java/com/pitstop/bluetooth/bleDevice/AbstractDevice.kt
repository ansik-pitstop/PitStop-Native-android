package com.pitstop.bluetooth.bleDevice

import com.pitstop.bluetooth.communicator.BluetoothCommunicator

/**
 * Created by Ben Wu on 2016-08-29.
 */
interface AbstractDevice {

    fun getVin(): Boolean
    fun getSupportedPids(): Boolean
    fun getDtcs(): Boolean
    fun getPendingDtcs(): Boolean
    fun getCommunicatorState(): Int
    fun getPids(pids: List<String>): Boolean
    fun setPidsToSend(pids: List<String>, timeInterval: Int): Boolean
    fun requestSnapshot(): Boolean
    fun closeConnection(): Boolean
}
