package com.pitstop.bluetooth.bleDevice

import android.bluetooth.BluetoothDevice
import java.util.*

/**
 * Created by Karol Zdebel on 8/22/2018.
 */
class ContinentalDevice: AbstractDevice {
    override fun getServiceUuid(): UUID {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReadChar(): UUID {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getWriteChar(): UUID {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBytes(payload: String?): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun parseData(data: ByteArray?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onConnectionStateChange(state: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestData() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getVin(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getRtc(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setRtc(rtcTime: Long): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPids(pids: String?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSupportedPids(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setPidsToSend(pids: String?, timeInterval: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun requestSnapshot(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearDtcs(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDtcs(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPendingDtcs(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getFreezeFrame(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun connectToDevice(device: BluetoothDevice?): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeConnection(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setCommunicatorState(state: Int): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCommunicatorState(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}