package com.pitstop.bluetooth.searcher

import com.pitstop.bluetooth.bleDevice.AbstractDevice

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
interface RVDBLuetoothDeviceSearcherStatusListener {
    fun onBindingRequired()
    fun onBindingQuestionPrompted(question: String)
    fun onFirmwareUpdateRequired()
    fun onFirmwareUpdateStatus(status: Int)
    fun onError(err: String)
    fun onConnectionCompleted(device: AbstractDevice)
}