package com.pitstop.bluetooth.bleDevice

/**
 * Created by Ben Wu on 2016-08-29.
 */
interface AbstractDevice {

    val vin: Boolean
    val supportedPids: Boolean
    val dtcs: Boolean
    val pendingDtcs: Boolean
    val communicatorState: Int
    fun getPids(pids: String): Boolean
    fun setPidsToSend(pids: String, timeInterval: Int): Boolean
    fun requestSnapshot(): Boolean
    fun closeConnection(): Boolean
    fun setCommunicatorState(state: Int): Boolean
}
