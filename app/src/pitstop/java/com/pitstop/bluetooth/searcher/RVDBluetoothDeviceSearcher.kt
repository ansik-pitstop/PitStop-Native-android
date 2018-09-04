package com.pitstop.bluetooth.searcher

import android.util.Log
import com.continental.rvd.mobile_sdk.*
import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error
import com.pitstop.bluetooth.bleDevice.RVDDevice

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDBluetoothDeviceSearcher(private val sdkIntentService: SDKIntentService
                                 , private val rvdBluetoothListener
                                 : RVDBluetoothDeviceSearcherStatusListener)
    : IEventsInterface.IEventListener {

    private val TAG = RVDBluetoothDeviceSearcher::class.java.simpleName
    private var sdk: ISDKApi? = null

    fun start(): Boolean{
        Log.d(TAG,"start()")
        sdkIntentService.initSDK(ISDKApi.VDCMode.APPLICATION_CONTROLLED, object: TApiCallback<ISDKApi>{
            override fun onSuccess(sdk: ISDKApi?) {
                Log.d(TAG,"successfully initialized RVD SDK!")
                this@RVDBluetoothDeviceSearcher.sdk = sdk
                sdk?.addNotificationListener(this@RVDBluetoothDeviceSearcher
                        ,IEventsInterface.EventType.ALL)
            }

            override fun onError(p0: Throwable?) {
                Log.d(TAG,"error while initializing RVD SDK!")
            }

        }, false)
        return true

    }

    fun answerBindingQuestion(question: String, response: String){
        Log.d(TAG,"answerBindingQuestion() question:$question, response:$response")

    }

    fun respondBindingRequest(start: Boolean){
        Log.d(TAG,"respondBindingRequest() start: $start")

    }

    fun respondFirmwareInstallationRequest(start: Boolean){
        Log.d(TAG,"respondFirmwareUpdateRequest() start: $start")
    }

    override fun onNotification(event: IEventsInterface.Event, retObject: Any?) {
        Log.d(TAG,"onNotification() event: $event")
        when (event){

            //BLUETOOTH
            IEventsInterface.Event.BLUETOOTH_CONNECT_TO -> {

            }
            IEventsInterface.Event.BLUETOOTH_PAIR_TO -> {

            }
            IEventsInterface.Event.BLUETOOTH_PAIRING_STARTED -> {

            }
            IEventsInterface.Event.BLUETOOTH_PAIRING_FINISHED -> {

            }
            IEventsInterface.Event.BLUETOOTH_PAIRING_ERROR -> {
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
                val question = retObject as BindingQuestion
                rvdBluetoothListener.onBindingQuestionPrompted(question.question)
            }

            //UPDATE EVENTS
            IEventsInterface.Event.UPDATE_AVAILABLE -> {
                rvdBluetoothListener.onFirmwareInstallationRequired()
            }
            IEventsInterface.Event.UPDATE_DOWNLOAD_STARTED-> {

            }
            IEventsInterface.Event.UPDATE_DOWNLOAD_PROGRESS_CHANGED-> {

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
                rvdBluetoothListener.onCompleted(RVDDevice(sdk!!))
            }
            IEventsInterface.Event.CAR_DISCONNECTED -> {

            }
        }
    }
}