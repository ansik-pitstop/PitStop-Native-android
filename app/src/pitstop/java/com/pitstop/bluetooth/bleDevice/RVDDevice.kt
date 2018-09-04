package com.pitstop.bluetooth.bleDevice

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDDevice: AbstractDevice {

    override fun getVin(): Boolean {
    }

    override fun getPids(pids: String?): Boolean {
    }

    override fun getSupportedPids(): Boolean {
    }

    override fun setPidsToSend(pids: String?, timeInterval: Int): Boolean {
    }

    override fun requestSnapshot(): Boolean {
    }

    override fun clearDtcs(): Boolean {
    }

    override fun getDtcs(): Boolean {
    }

    override fun getPendingDtcs(): Boolean {
    }

    override fun closeConnection(): Boolean {
    }

    override fun setCommunicatorState(state: Int): Boolean {
    }

    override fun getCommunicatorState(): Int {
    }
}