package com.pitstop.bluetooth.searcher

import com.continental.rvd.mobile_sdk.BindingQuestion

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
interface RVDBluetoothDeviceSearcherStatusListener {
    fun onConnectionFailure(err: Error)
    fun onConnectionCompleted()

    fun onBindingRequired()
    fun onBindingQuestionPrompted(question: BindingQuestion)
    fun onBindingProgress(progress: Float)
    fun onBindingFinished()
    fun onBindingError(err: Error)

    fun onFirmwareInstallationRequired()
    fun onFirmwareInstallationProgress(progress: Float)
    fun onFirmwareInstallationFinished()
    fun onFirmwareInstallationError(err: Error)
}