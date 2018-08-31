package com.pitstop.bluetooth.bleDevice

import android.bluetooth.BluetoothDevice
import java.util.*

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDDevice: AbstractDevice {

    override fun getServiceUuid(): UUID {
    }

    override fun getReadChar(): UUID {
    }

    override fun getWriteChar(): UUID {
    }

    override fun getBytes(payload: String?): ByteArray {
    }

    override fun parseData(data: ByteArray?) {
    }

    override fun onConnectionStateChange(state: Int) {
    }

    override fun requestData() {
    }

    override fun getVin(): Boolean {
    }

    override fun getRtc(): Boolean {
    }

    override fun setRtc(rtcTime: Long): Boolean {
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

    override fun getFreezeFrame(): Boolean {
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