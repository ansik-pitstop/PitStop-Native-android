package com.pitstop.bluetooth.searcher

import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error
import com.pitstop.bluetooth.bleDevice.AbstractDevice

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
interface RVDBluetoothDeviceSearcherStatusListener {
    fun onBindingRequired()
    fun onBindingQuestionPrompted(question: String)
    fun onBindingStatusUpdate(status: Float)
    fun onFirmwareUpdateRequired()
    fun onFirmwareUpdateStatus(status: Float)
    fun onStopped()
    fun onError(err: Error)
    fun onConnectionCompleted(device: AbstractDevice)
}