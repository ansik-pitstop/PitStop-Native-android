package com.pitstop.bluetooth.bleDevice

import com.continental.rvd.mobile_sdk.ISDKApi
import com.continental.rvd.mobile_sdk.TApiCallback
import com.pitstop.bluetooth.BluetoothDeviceManager

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDDevice(private val rvdSDK: ISDKApi, private val deviceManager: BluetoothDeviceManager): AbstractDevice {

    override fun getVin(): Boolean {
        rvdSDK.getVin(object: TApiCallback<String>{
            override fun onSuccess(VIN: String?) {
                if (VIN != null)
                    deviceManager.onGotVin(VIN)
            }

            override fun onError(p0: Throwable?) {
            }

        })
        return true
    }

    override fun getPids(pids: String): Boolean {
        return true
    }

    override fun getSupportedPids(): Boolean {
        return true
    }

    override fun setPidsToSend(pids: String?, timeInterval: Int): Boolean {
        return true
    }

    override fun requestSnapshot(): Boolean {
        //Get all available pids then ask for them all
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