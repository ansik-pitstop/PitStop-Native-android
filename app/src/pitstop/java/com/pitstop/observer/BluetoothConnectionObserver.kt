package com.pitstop.observer

import com.continental.rvd.mobile_sdk.BindingQuestion
import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.models.ReadyDevice

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

interface BluetoothConnectionObserver : Observer {

    //Searching for bluetooth device associated with user car
    fun onSearchingForDevice()

    //Device is ready to be interacted with
    fun onDeviceReady(readyDevice: ReadyDevice)

    //Device that was previously ready now disconnected and can no longer be interacted with
    fun onDeviceDisconnected()

    //Device is connected and now being verified
    fun onDeviceVerifying()

    //Device has been verified and is now syncing rtc time
    fun onDeviceSyncing()

    fun onGotSuportedPIDs(value: String)

    fun onConnectingToDevice()

    fun onFoundDevices()

    fun onGotPid(pidPackage: PidPackage)

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
