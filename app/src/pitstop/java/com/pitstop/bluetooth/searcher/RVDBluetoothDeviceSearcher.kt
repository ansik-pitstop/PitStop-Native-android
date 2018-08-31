package com.pitstop.bluetooth.searcher

import android.util.Log
import com.continental.rvd.mobile_sdk.*

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDBluetoothDeviceSearcher(private val sdkIntentService: SDKIntentService
                                 , private val rvdBluetoothListener
                                 : RVDBLuetoothDeviceSearcherStatusListener)
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

    fun respondFirmwareUpdateRequest(start: Boolean){
        Log.d(TAG,"respondFirmwareUpdateRequest() start: $start")
    }

    override fun onNotification(event: IEventsInterface.Event, retObject: Any?) {
        Log.d(TAG,"onNotification() event: $event")
        when (event){

            //BINDING EVENTS
            IEventsInterface.Event.BINDING_STARTED -> {

            }
            IEventsInterface.Event.BINDING_PROGRESS_CHANGED -> {
                val progress = event as Float
            }
            IEventsInterface.Event.BINDING_ERROR -> {

            }
            IEventsInterface.Event.BINDING_FINISHED -> {

            }
            IEventsInterface.Event.BINDING_STOPPED -> {

            }
            IEventsInterface.Event.BINDING_USER_INPUT -> {
                val question = retObject as BindingQuestion
            }

            //AUTHENTICATION EVENTS
            IEventsInterface.Event.AUTHENTICATION_SUCCESS -> {

            }
            IEventsInterface.Event.AUTHENTICATION_ERROR -> {

            }
            IEventsInterface.Event.AUTHENTICATION_FAILURE -> {

            }

            IEventsInterface.Event.BLUETOOTH_CONNECT_TO -> {

            }
            IEventsInterface.Event.BLUETOOTH_PAIR_TO -> {

            }
            IEventsInterface.Event.BLUETOOTH_PAIRING_STARTED -> {

            }
            IEventsInterface.Event.BLUETOOTH_PAIRING_FINISHED -> {

            }
            IEventsInterface.Event.BLUETOOTH_PAIRING_ERROR -> {

            }

            //UPDATE EVENTS
            IEventsInterface.Event.UPDATE_AVAILABLE -> {

            }
            IEventsInterface.Event.UPDATE_DOWNLOAD_STARTED-> {

            }
            IEventsInterface.Event.UPDATE_DOWNLOAD_PROGRESS_CHANGED-> {

            }
            IEventsInterface.Event.UPDATE_DOWNLOAD_ERROR -> {

            }
            IEventsInterface.Event.UPDATE_FINISHED -> {

            }
            IEventsInterface.Event.UPDATE_ERROR -> {

            }
            IEventsInterface.Event.UPDATE_VERIFICATION_RESULT -> {

            }
            IEventsInterface.Event.UPDATE_STARTED -> {

            }

            //FIRMWARE EVENT
            IEventsInterface.Event.FIRMWARE_INSTALLATION_STARTED -> {

            }
            IEventsInterface.Event.FIRMWARE_INSTALLATION_PROGRESS_CHANGED -> {

            }
            IEventsInterface.Event.FIRMWARE_INSTALLATION_FINISHED -> {

            }
            IEventsInterface.Event.FIRMWARE_INSTALLATION_ERROR -> {

            }

            //DONGLE EVENTS
            IEventsInterface.Event.DONGLE_STATE_DISCONNECTED -> {

            }
            IEventsInterface.Event.DONGLE_STATE_CONNECTING -> {

            }
            IEventsInterface.Event.DONGLE_STATE_CONNECTED -> {

            }

            //CAR EVENTS
            IEventsInterface.Event.CAR_CONNECTED -> {

            }
            IEventsInterface.Event.CAR_DISCONNECTED -> {

            }

            //LICENSE EVENTS
            IEventsInterface.Event.LICENSE_INVALID -> {

            }
            IEventsInterface.Event.LICENSE_UNVERIFIABLE -> {

            }
        }
    }
}