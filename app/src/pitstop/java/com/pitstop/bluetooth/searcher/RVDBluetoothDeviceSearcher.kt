package com.pitstop.bluetooth.searcher

import android.media.MediaDrm
import android.util.Log
import com.continental.rvd.mobile_sdk.*
import com.continental.rvd.mobile_sdk.errors.SDKException
import com.continental.rvd.mobile_sdk.events.*
import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error
import com.continental.rvd.mobile_sdk.internal.logs.logging.domain.LogEntity
import com.pitstop.bluetooth.BluetoothDeviceManager
import com.pitstop.bluetooth.bleDevice.RVDDevice

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDBluetoothDeviceSearcher(private val sdkIntentService: RvdIntentService
                                 , private val rvdBluetoothListener: RVDBluetoothDeviceSearcherStatusListener
                                 , private val deviceManager: BluetoothDeviceManager)
    : IEventsInterface.IEventListener, OnBluetoothEvents, OnNetworkEvents, OnDongleEvents, OnLicenseEvents
                                , OnAuthenticationEvents {

    private val TAG = RVDBluetoothDeviceSearcher::class.java.simpleName
    private var sdk: RvdApi? = null

    fun start(): Boolean{
        Log.d(TAG,"start()")
        if (sdk == null){

            sdkIntentService.initializeRvdApi(CollectionMode.APPLICATION_CONTROLLED, object: Callback<RvdApi>() {
                override fun onSuccess(sdk: RvdApi?) {
                    Log.d(TAG,"successfully initialized RVD SDK!")
                    this@RVDBluetoothDeviceSearcher.sdk = sdk

//                    sdk?.addNotificationListener(this@RVDBluetoothDeviceSearcher
//                            ,IEventsInterface.EventType.ALL)
                    sdk?.addEventListener().on


                }

                override fun onError(p0: Throwable?) {
                    Log.d(TAG,"Failed to initialize RVD SDK")
                }
            })


            sdkIntentService.initSDK(ISDKApi.VDCMode.APPLICATION_CONTROLLED, object: TApiCallback<ISDKApi>{
                override fun onSuccess(sdk: ISDKApi?) {
                    Log.d(TAG,"successfully initialized RVD SDK!")
                    this@RVDBluetoothDeviceSearcher.sdk = sdk

                    sdk?.addNotificationListener(this@RVDBluetoothDeviceSearcher
                            ,IEventsInterface.EventType.ALL)
                }

                override fun onError(p0: Throwable?) {
                    Log.d(TAG,"Failed to initialize RVD SDK")
                }

            }, false)
            return true
        }else{
            return false
        }
    }

    fun respondBindingRequest(start: Boolean): Boolean {
        Log.d(TAG,"respondBindingRequest() start: $start")
        if (start) sdk?.startBindingProcess(false)
        return start
    }

    fun answerBindingQuestion(questionType: EBindingQuestionType, response: String): Boolean {
        Log.d(TAG,"answerBindingQuestion() question:$questionType, response:$response")
        sdk?.answerBindingUserInput(questionType,response)
        return true
    }

    fun respondFirmwareInstallationRequest(start: Boolean): Boolean {
        Log.d(TAG,"respondFirmwareUpdateRequest() start: $start")
        if (start) sdk?.startDownloadUpdate()
        return start
    }

    override fun onBluetoothPairingFinished() {
        deviceManager.scanFinished()
    }

    override fun onBluetoothPairingError(error: SDKException?) {
        // Param object should be instance of Throwable
        // TODO: Send connection failure to rvdBluetoothListener
//        rvdBluetoothListener.onConnectionFailure()
    }

    override fun onInternetConnectionRequired() {
        // TODO: Send failure to rvdBluetoothListener
//        rvdBluetoothListener.onConnectionFailure(retObject as Error)
    }

    override fun onDongleConnecting(toBluetoothDevice: BluetoothDongle?) {
        deviceManager.scanFinished()
    }

    override fun onDongleNotConfigured(notFirstBinding: Boolean?) {
        rvdBluetoothListener.onBindingRequired()
    }

    override fun onLicenseUnverifiable() {
        // TODO: Send failure to rvdBluetoothListener
//        rvdBluetoothListener.onConnectionFailure()
    }





    override fun onNotification(event: IEventsInterface.Event, retObject: Any?) {
//        Log.d(TAG,"onNotification() event: $event ")

        when (event) {

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
//            IEventsInterface.Event.UPDATE_STARTED -> {
//
//            }

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