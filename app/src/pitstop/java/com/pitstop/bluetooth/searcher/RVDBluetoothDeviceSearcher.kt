package com.pitstop.bluetooth.searcher

import android.util.Log
import com.continental.rvd.mobile_sdk.*
import com.continental.rvd.mobile_sdk.errors.SDKException
import com.continental.rvd.mobile_sdk.events.*
import com.pitstop.bluetooth.BluetoothDeviceManager
import com.pitstop.bluetooth.bleDevice.RVDDevice

/**
 * Created by Karol Zdebel on 8/31/2018.
 */

class RVDBluetoothDeviceSearcher(private val rvdIntentService: RvdIntentService
                                 , private val rvdBluetoothListener: RVDBluetoothDeviceSearcherStatusListener
                                 , private val deviceManager: BluetoothDeviceManager)
     {

    private val TAG = RVDBluetoothDeviceSearcher::class.java.simpleName
    private var sdk: RvdApi? = null


    fun start(): Boolean{
        Log.d(TAG,"start()")
        if (sdk == null){

            rvdIntentService.initializeRvdApi(CollectionMode.APPLICATION_CONTROLLED, object: Callback<RvdApi>(){
                override fun onSuccess(sdk: RvdApi?) {
                    Log.d(TAG,"successfully initialized RVD SDK!")
                    this@RVDBluetoothDeviceSearcher.sdk = sdk
                    if(sdk == null) {
                        return
                    }
                    sdk.addEventListener().onLogsEvents(object: OnLogsEvents{
                        override fun onLog(log: String?) {

                            Log.d(TAG,"log: ${log}")

                        }
                    })

                    sdk.addEventListener().onBluetoothEvents(object: OnBluetoothEvents{
                        override fun onBluetoothPairingFinished() {
                            deviceManager.scanFinished()
                        }
                    })

                    sdk.addEventListener().onBluetoothEvents(object: OnBluetoothEvents{
                        override fun onBluetoothPairingError(error: SDKException?) {
                            rvdBluetoothListener.onConnectionFailure(retObject as Error)
                        }
                    })

                    sdk.addEventListener().onNetworkEvents(object: OnNetworkEvents{
                        override fun onInternetConnectionRequired() {
                            rvdBluetoothListener.onConnectionFailure(retObject as Error)
                        }
                    })
                    sdk.addEventListener().onDongleEvents(object : OnDongleEvents{
                        override fun onDongleConnecting(toBluetoothDevice: BluetoothDongle?) {
                            deviceManager.scanFinished()
                        }
                    })
                    sdk.addEventListener().onDongleEvents(object : OnDongleEvents{
                        override fun onDongleNotConfigured(notFirstBinding: Boolean?) {
                            rvdBluetoothListener.onBindingRequired()
                        }
                    })
                    sdk.addEventListener().onLicenseEvents(object: OnLicenseEvents{
                        override fun onLicenseUnverifiable() {
                            rvdBluetoothListener.onConnectionFailure(retObject as Error)
                        }
                    })
                    sdk.addEventListener().onAuthenticationEvents(object : OnAuthenticationEvents{
                        override fun onAuthenticationSuccess() {
                            rvdBluetoothListener.onConnectionCompleted()
                        }
                    })
                    sdk.addEventListener().onAuthenticationEvents(object : OnAuthenticationEvents{
                        override fun onAuthenticationError(error: SDKException?) {
                            rvdBluetoothListener.onConnectionFailure(retObject as Error)
                        }
                    })
                    sdk.addEventListener().onBindingEvents(object : OnBindingEvents{
                        override fun onBindingProcessChanged(progress: Float) {
                            rvdBluetoothListener.onBindingProgress(retObject as Float)
                        }
                    })
                    sdk.addEventListener().onBindingEvents(object : OnBindingEvents{
                        override fun onBindingError(error: SDKException?) {
                            rvdBluetoothListener.onBindingError(retObject as Error)
                        }
                    })
                    sdk.addEventListener().onBindingEvents(object : OnBindingEvents{
                        override fun onBindingFinished() {
                            rvdBluetoothListener.onBindingFinished()
                        }
                    })
                    sdk.addEventListener().onBindingEvents(object : OnBindingEvents{
                        override fun onBindingUserInput(bindingQuestion: BindingQuestion?) {
                            rvdBluetoothListener.onBindingQuestionPrompted(retObject as BindingQuestion)
                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateAvailable(firmwareCheckEntity: FirmwareCheck?) {
                            rvdBluetoothListener.onFirmwareInstallationRequired()
                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateDownloadProgressChanged(progress: Float) {
                            rvdBluetoothListener.onFirmwareInstallationProgress(retObject as Float)
                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateDownloadError(error: SDKException?) {
                            rvdBluetoothListener.onFirmwareInstallationError(retObject as Error)
                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateError(error: SDKException?) {
                            rvdBluetoothListener.onFirmwareInstallationError(retObject as Error)

                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateVerificationResult(firmwareCheckResults: FirmwareCheckResults?) {
                            val result = retObject as FirmwareCheckResults
                            if (result == FirmwareCheckResults.OK) {
                                sdk?.startFirmwareInstallation()
                            }                        }
                    })
                    sdk.addEventListener().onFirmwareEvents(object : OnFirmwareEvents{
                        override fun onFirmwareInstallationProgressChanged(progress: Float) {
                            rvdBluetoothListener.onFirmwareInstallationProgress(retObject as Float)
                        }
                    })
                    sdk.addEventListener().onFirmwareEvents(object : OnFirmwareEvents{
                        override fun onFirmwareInstallationFinished() {
                            rvdBluetoothListener.onFirmwareInstallationFinished()
                        }
                    })
                    sdk.addEventListener().onFirmwareEvents(object : OnFirmwareEvents{
                        override fun onFirmwareInstallationError(error: SDKException?) {
                            rvdBluetoothListener.onFirmwareInstallationError(retObject as Error)
                        }
                    })
                    sdk.addEventListener().onCarEvents(object : OnCarEvents{
                        override fun onCarConnected() {
                            deviceManager.onCompleted(RVDDevice(sdk!!,deviceManager))
                        }
                    })


                }




                override fun onError(p0: Throwable?) {
                    Log.d(TAG,"Failed to initialize RVD SDK")
                }

            })
            return true
        }else{
            return false
        }
    }

    fun respondBindingRequest(start: Boolean): Boolean {
        Log.d(TAG,"respondBindingRequest() start: $start")
        if (start) sdk?.startBinding(false)
        return start
    }

    fun answerBindingQuestion(questionType: BindingQuestionType, response: String): Boolean {
        Log.d(TAG,"answerBindingQuestion() question:$questionType, response:$response")
        sdk?.answerBindingUserInput(questionType,response)
        return true
    }

    fun respondFirmwareInstallationRequest(start: Boolean): Boolean {
        Log.d(TAG,"respondFirmwareUpdateRequest() start: $start")
        if (start) sdk?.startDownloadUpdate()
        return start
    }

    override fun onNotification(event: IEventsInterface.Event, retObject: Any?) {
//        Log.d(TAG,"onNotification() event: $event ")

        when (event){

            IEventsInterface.Event.LOG -> {

                Log.d(TAG,"log: ${(retObject as LogEntity).message}")
            }

            //BLUETOOTH
            IEventsInterface.Event.BLUETOOTH_CONNECT_TO -> {
            }
            IEventsInterface.Event.BLUETOOTH_PAIR_TO -> {
            }
            IEventsInterface.Event.BLUETOOTH_OFF -> {
            }
            IEventsInterface.Event.BLUETOOTH_PAIRING_STARTED -> {
            }
            IEventsInterface.Event.BLUETOOTH_PAIRING_FINISHED -> {
                deviceManager.scanFinished()
            }

            IEventsInterface.Event.BLUETOOTH_PAIRING_ERROR -> {
                // Param object should be instance of Throwable
                rvdBluetoothListener.onConnectionFailure(retObject as Error)
            }

            //NETWORK
            IEventsInterface.Event.NETWORK_AVAILABLE -> {

            }
            IEventsInterface.Event.NETWORK_UNAVAILABLE-> {

            }
            IEventsInterface.Event.INTERNET_CONNECTION_REQUIRED -> {
                rvdBluetoothListener.onConnectionFailure(retObject as Error)
            }

            //DONGLE EVENTS
            IEventsInterface.Event.DONGLE_STATE_DISCONNECTED -> {

            }
            IEventsInterface.Event.DONGLE_STATE_CONNECTING -> {
                deviceManager.scanFinished()
            }
            IEventsInterface.Event.DONGLE_STATE_CONNECTED -> {

            }
            IEventsInterface.Event.DONGLE_CONFIGURED -> {
                //No binding needed, already done
            }
            IEventsInterface.Event.DONGLE_NOT_CONFIGURED -> {
                rvdBluetoothListener.onBindingRequired()
            }

            //LICENSE EVENTS
            IEventsInterface.Event.LICENSE_INVALID -> {

            }
            IEventsInterface.Event.LICENSE_UNVERIFIABLE -> {
                rvdBluetoothListener.onConnectionFailure(retObject as Error)
            }

            //AUTHENTICATION EVENTS
            IEventsInterface.Event.AUTHENTICATION_SUCCESS -> {
                rvdBluetoothListener.onConnectionCompleted()
            }
            IEventsInterface.Event.AUTHENTICATION_ERROR -> {
                rvdBluetoothListener.onConnectionFailure(retObject as Error)
            }
            IEventsInterface.Event.AUTHENTICATION_FAILURE -> {
                rvdBluetoothListener.onConnectionFailure(retObject as Error)
            }

            //BINDING EVENTS
            IEventsInterface.Event.BINDING_STARTED -> {

            }
            IEventsInterface.Event.BINDING_PROGRESS_CHANGED -> {
                rvdBluetoothListener.onBindingProgress(retObject as Float)
            }
            IEventsInterface.Event.BINDING_ERROR -> {
                rvdBluetoothListener.onBindingError(retObject as Error)
            }
            IEventsInterface.Event.BINDING_FINISHED -> {
                rvdBluetoothListener.onBindingFinished()
            }
            IEventsInterface.Event.BINDING_STOPPED -> {
                rvdBluetoothListener.onBindingError(retObject as Error)
            }
            IEventsInterface.Event.BINDING_USER_INPUT -> {
                rvdBluetoothListener.onBindingQuestionPrompted(retObject as BindingQuestion)
            }

            //UPDATE EVENTS
            IEventsInterface.Event.UPDATE_AVAILABLE -> {
                rvdBluetoothListener.onFirmwareInstallationRequired()
            }
            IEventsInterface.Event.UPDATE_DOWNLOAD_STARTED-> {

            }
            IEventsInterface.Event.UPDATE_DOWNLOAD_PROGRESS_CHANGED-> {
                rvdBluetoothListener.onFirmwareInstallationProgress(retObject as Float)
            }
            IEventsInterface.Event.UPDATE_DOWNLOAD_ERROR -> {
                rvdBluetoothListener.onFirmwareInstallationError(retObject as Error)
            }
            IEventsInterface.Event.UPDATE_FINISHED -> {
            }
            IEventsInterface.Event.UPDATE_ERROR -> {
                rvdBluetoothListener.onFirmwareInstallationError(retObject as Error)
            }
            IEventsInterface.Event.UPDATE_VERIFICATION_RESULT -> {
                val result = retObject as FirmwareCheckResults
                if (result == FirmwareCheckResults.OK) {
                    sdk?.startFirmwareInstallation()
                }
            }
            IEventsInterface.Event.UPDATE_STARTED -> {

            }

            //FIRMWARE EVENT
            IEventsInterface.Event.FIRMWARE_INSTALLATION_STARTED -> {

            }
            IEventsInterface.Event.FIRMWARE_INSTALLATION_PROGRESS_CHANGED -> {
                rvdBluetoothListener.onFirmwareInstallationProgress(retObject as Float)
            }
            IEventsInterface.Event.FIRMWARE_INSTALLATION_FINISHED -> {
                rvdBluetoothListener.onFirmwareInstallationFinished()
            }
            IEventsInterface.Event.FIRMWARE_INSTALLATION_ERROR -> {
                rvdBluetoothListener.onFirmwareInstallationError(retObject as Error)
            }

            //CAR EVENTS
            IEventsInterface.Event.CAR_CONNECTED -> {
                deviceManager.onCompleted(RVDDevice(sdk!!,deviceManager))
            }
            IEventsInterface.Event.CAR_DISCONNECTED -> {

            }
        }
    }
}