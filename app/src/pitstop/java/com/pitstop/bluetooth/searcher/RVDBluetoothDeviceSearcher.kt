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
class RVDBluetoothDeviceSearcher(private val sdkIntentService: RvdIntentService
                                 , private val rvdBluetoothListener: RVDBluetoothDeviceSearcherStatusListener
                                 , private val deviceManager: BluetoothDeviceManager)
    : OnBluetoothEvents, OnNetworkEvents, OnDongleEvents, OnLicenseEvents, OnAuthenticationEvents {

    private val TAG = RVDBluetoothDeviceSearcher::class.java.simpleName
    private var sdk: RvdApi? = null

    fun start(): Boolean{
        Log.d(TAG,"start()")
        if (sdk == null){

            sdkIntentService.initializeRvdApi(CollectionMode.APPLICATION_CONTROLLED, object: Callback<RvdApi>() {
                override fun onSuccess(sdk: RvdApi?) {
                    Log.d(TAG,"successfully initialized RVD SDK!")
                    this@RVDBluetoothDeviceSearcher.sdk = sdk

                    sdk?.addEventListener()?.onBluetoothEvents(this@RVDBluetoothDeviceSearcher)
                    sdk?.addEventListener()?.onNetworkEvents(this@RVDBluetoothDeviceSearcher)
                    sdk?.addEventListener()?.onDongleEvents(this@RVDBluetoothDeviceSearcher)
                    sdk?.addEventListener()?.onLicenseEvents(this@RVDBluetoothDeviceSearcher)
                    sdk?.addEventListener()?.onAuthenticationEvents(this@RVDBluetoothDeviceSearcher)
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
        if (start) sdk?.startBinding()
        return start
    }

    fun answerBindingQuestion(questionType: BindingQuestionType, response: String): Boolean {
        Log.d(TAG,"answerBindingQuestion() question:$questionType, response:$response")
        sdk?.answerBindingUserInput(questionType, response)
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
}