package com.pitstop.bluetooth.bleDevice

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDDevice: AbstractDevice {

    override fun getVin(): Boolean {
        return true
    }

    override fun getPids(pids: String?): Boolean {
        return true
    }

    override fun getSupportedPids(): Boolean {
        return true
    }

    override fun setPidsToSend(pids: String?, timeInterval: Int): Boolean {
        return true
    }

    override fun requestSnapshot(): Boolean {
        return true
    }

    override fun clearDtcs(): Boolean {
        return true
    }

    override fun getDtcs(): Boolean {
        return true
    }

    override fun getPendingDtcs(): Boolean {
        return true
    }

    override fun closeConnection(): Boolean {
        return true
    }

    override fun setCommunicatorState(state: Int): Boolean {
        return true
    }

    override fun getCommunicatorState(): Int {
        return 0
    }
}