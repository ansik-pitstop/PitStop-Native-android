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
                            Log.d(TAG,"log: $log")
                        }
                    })

                    sdk.addEventListener().onBluetoothEvents(object: OnBluetoothEvents{
                        override fun onBluetoothPairingFinished() {
                            Log.d(TAG,"onBluetoothPairingFinished!")
                            deviceManager.scanFinished()
                        }
                    })

                    sdk.addEventListener().onBluetoothEvents(object: OnBluetoothEvents{

                        override fun onBluetoothPairTo(devices: Array<out BluetoothDongle>?) {
                            Log.d(TAG,"onBluetoothPairTo!" + devices)
                        }

                        override fun onBluetoothConnectTo(devicesAvailableForConnection: Array<out BluetoothDongle>?) {
                            Log.d(TAG,"onBluetoothConnectTo!" + devicesAvailableForConnection)
                        }

                        override fun onBluetoothPairingStarted() {
                            Log.d(TAG,"onBluetoothPairingStarted!" )
                        }

                        override fun onBluetoothPairingFinished() {
                            Log.d(TAG,"onBluetoothPairingStarted!" )
                        }
                        override fun onBluetoothPairingError(error: SDKException?) {
                            Log.d(TAG,"onBluetoothPairingError!")

                            if (error == null) {
                                return
                            }
                            val newError = java.lang.Error(error.errorMessage)
                            newError.stackTrace = error.stackTrace
                            rvdBluetoothListener.onConnectionFailure(newError)
                        }

                    })



                    sdk.addEventListener().onNetworkEvents(object: OnNetworkEvents{
                        override fun onInternetConnectionRequired() {
                            Log.d(TAG,"onInternetConnectionRequired!")

                            val error = java.lang.Error("Internet connection required")
                            rvdBluetoothListener.onConnectionFailure(error)
                        }
                    })
                    sdk.addEventListener().onDongleEvents(object : OnDongleEvents{
                        override fun onDongleConnecting(toBluetoothDevice: BluetoothDongle?) {
                            Log.d(TAG,"onDongleConnecting!")

                            deviceManager.scanFinished()
                        }
                    })
                    sdk.addEventListener().onDongleEvents(object : OnDongleEvents{
                        override fun onDongleNotConfigured(notFirstBinding: Boolean?) {
                            Log.d(TAG,"onDongleNotConfigured!")
                            if (sdk.bindingStatus.state == BindingStatus.State.PAUSED) {
                                sdk.resumeBinding(false)
                                return
                            }
                            if (sdk.bindingStatus.state != BindingStatus.State.STARTED) {
                                rvdBluetoothListener.onBindingRequired()
                            }
                         //   rvdBluetoothListener.onBindingRequired()
                        }
                    })
                    sdk.addEventListener().onLicenseEvents(object: OnLicenseEvents{
                        override fun onLicenseUnverifiable() {
                            val error = java.lang.Error("License unverifiable")
                            rvdBluetoothListener.onConnectionFailure(error)
                        }
                    })
                    sdk.addEventListener().onAuthenticationEvents(object : OnAuthenticationEvents{
                        override fun onAuthenticationSuccess() {
                            Log.d(TAG,"onAuthenticationSuccess!")
                            rvdBluetoothListener.onConnectionCompleted()
                        }
                    })
                    sdk.addEventListener().onAuthenticationEvents(object : OnAuthenticationEvents{
                        override fun onAuthenticationError(error: SDKException?) {
                            Log.d(TAG,"onAuthenticationError!")
                            if (error == null) {
                                return
                            }
                            val newError = java.lang.Error(error.errorMessage)
                            newError.stackTrace = error.stackTrace
                            rvdBluetoothListener.onConnectionFailure(newError)
                        }
                    })
                    sdk.addEventListener().onBindingEvents(object : OnBindingEvents{
                        override fun onBindingProcessChanged(progress: Float) {
                            Log.d(TAG,"onBindingProcessChanged!!!!" + progress)
                            rvdBluetoothListener.onBindingProgress(progress)
                        }
                    })
                    sdk.addEventListener().onBindingEvents(object : OnBindingEvents{
                        override fun onBindingError(error: SDKException?) {
                            Log.d(TAG,"onBindingError!")
                            if (error == null) {
                                return
                            }
                            Log.d(TAG,"error!" + error)

                            val newError = java.lang.Error(error.errorMessage)
                            newError.stackTrace = error.stackTrace
                            rvdBluetoothListener.onConnectionFailure(newError)
                        }
                    })
                    sdk.addEventListener().onBindingEvents(object : OnBindingEvents{
                        override fun onBindingFinished() {
                            Log.d(TAG,"onBindingFinished!")
                            rvdBluetoothListener.onBindingFinished()
                        }
                    })
                    sdk.addEventListener().onBindingEvents(object : OnBindingEvents{
                        override fun onBindingUserInput(bindingQuestion: BindingQuestion?) {
                            Log.d(TAG,"onBindingUserInput!")
                            if (bindingQuestion == null) {
                                return
                            }
                            rvdBluetoothListener.onBindingQuestionPrompted(bindingQuestion)
                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateAvailable(firmwareCheckEntity: FirmwareCheck?) {

                            rvdBluetoothListener.onFirmwareInstallationRequired()
                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateDownloadProgressChanged(progress: Float) {
                            rvdBluetoothListener.onFirmwareInstallationProgress(progress)
                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateDownloadError(error: SDKException?) {
                            if (error == null) {
                                return
                            }
                            val newError = java.lang.Error(error.errorMessage)
                            newError.stackTrace = error.stackTrace
                            rvdBluetoothListener.onFirmwareInstallationError(newError)
                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateError(error: SDKException?) {
                            if (error == null) {
                                return
                            }
                            val newError = java.lang.Error(error.errorMessage)
                            newError.stackTrace = error.stackTrace
                            rvdBluetoothListener.onFirmwareInstallationError(newError)
                        }
                    })
                    sdk.addEventListener().onUpdateEvents(object : OnUpdateEvents{
                        override fun onUpdateVerificationResult(firmwareCheckResults: FirmwareCheckResults?) {
                            if (firmwareCheckResults == FirmwareCheckResults.OK) {
                                sdk?.startFirmwareInstallation()
                            }
                        }
                    })
                    sdk.addEventListener().onFirmwareEvents(object : OnFirmwareEvents{
                        override fun onFirmwareInstallationProgressChanged(progress: Float) {
                            rvdBluetoothListener.onFirmwareInstallationProgress(progress)
                        }
                    })
                    sdk.addEventListener().onFirmwareEvents(object : OnFirmwareEvents{
                        override fun onFirmwareInstallationFinished() {
                            rvdBluetoothListener.onFirmwareInstallationFinished()
                        }
                    })
                    sdk.addEventListener().onFirmwareEvents(object : OnFirmwareEvents{
                        override fun onFirmwareInstallationError(error: SDKException?) {
                            if (error == null) {
                                return
                            }
                            val newError = java.lang.Error(error.errorMessage)
                            newError.stackTrace = error.stackTrace
                            rvdBluetoothListener.onFirmwareInstallationError(newError)
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
}