package com.pitstop.bluetooth.bleDevice

import android.bluetooth.BluetoothDevice
import java.util.*

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDDevice: AbstractDevice {

    //Remove
    override fun getServiceUuid(): UUID {
    }

    //Remove
    override fun getReadChar(): UUID {
    }

    //Remove
    override fun getWriteChar(): UUID {
    }

    //Remove
    override fun getBytes(payload: String?): ByteArray {
    }

    //Remove
    override fun parseData(data: ByteArray?) {
    }


    override fun onConnectionStateChange(state: Int) {
    }

    //Remove
    override fun requestData() {
    }

    override fun getVin(): Boolean {
        return false
    }

    override fun getRtc(): Boolean {
        return false
    }

    override fun setRtc(rtcTime: Long): Boolean {
        return false
    }

    override fun getPids(pids: String?): Boolean {
        return false
    }

    override fun getSupportedPids(): Boolean {
        return false
    }

    override fun setPidsToSend(pids: String?, timeInterval: Int): Boolean {
        return false
    }

    override fun requestSnapshot(): Boolean {
        return false
    }

    override fun clearDtcs(): Boolean {
        return false
    }

    override fun getDtcs(): Boolean {
        return false
    }

    override fun getPendingDtcs(): Boolean {
        return false
    }

    override fun getFreezeFrame(): Boolean {
        return false
    }

    override fun connectToDevice(device: BluetoothDevice?): Boolean {
    }

    override fun closeConnection(): Boolean {
    }

    override fun setCommunicatorState(state: Int): Boolean {
    }

    override fun getCommunicatorState(): Int {
    }
}