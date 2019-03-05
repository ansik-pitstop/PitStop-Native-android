package com.pitstop.bluetooth.searcher

import android.util.Log
import com.continental.rvd.mobile_sdk.*
import com.continental.rvd.mobile_sdk.errors.NetworkError
import com.continental.rvd.mobile_sdk.errors.SDKException
import com.continental.rvd.mobile_sdk.events.*
import com.continental.rvd.mobile_sdk.internal.license.domain.LicenseEntity
import com.pitstop.bluetooth.BluetoothDeviceManager
import com.pitstop.bluetooth.bleDevice.RVDDevice
import java.lang.Error

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDBluetoothDeviceSearcher(private val sdkIntentService: RvdIntentService
                                 , private val rvdBluetoothListener: RVDBluetoothDeviceSearcherStatusListener
                                 , private val deviceManager: BluetoothDeviceManager) {

    private val TAG = RVDBluetoothDeviceSearcher::class.java.simpleName
    private var sdk: RvdApi? = null

    fun start(): Boolean {
        Log.d(TAG,"start()")
        if (sdk == null){

            sdkIntentService.initializeRvdApi(CollectionMode.APPLICATION_CONTROLLED, object: Callback<RvdApi>() {
                override fun onSuccess(sdk: RvdApi?) {

                    if (sdk == null) {
                        deviceManager.onConnectionFailure(Error("Failed to initialize SDK"))
                        return
                    }

                    Log.d(TAG,"successfully initialized RVD SDK!")
                    rvdBluetoothListener.onMessageFromDevice("Successfully initialized RVD SDK!")
                    this@RVDBluetoothDeviceSearcher.sdk = sdk

                    // Bluetooth
                    sdk.addEventListener()?.onBluetoothEvents(object: OnBluetoothEvents {
                        override fun onBluetoothConnectTo(devicesAvailableForConnection: Array<out BluetoothDongle>?) {
                            Log.d(TAG,"onBluetoothConnectTo(devicesAvailableForConnection: Array<out BluetoothDongle>?)")
                            val dongle = devicesAvailableForConnection?.getOrNull(0)
                            sdk.setDongleToConnectTo(dongle)
//                            sdk.connectManuallyToDongle(dongle, object: Callback<Void>() {
//                                override fun onSuccess(p0: Void?) {
//                                    Log.d(TAG,"sdk.connectManuallyToDongle() - success")
//                                    rvdBluetoothListener.onConnectionCompleted()
//                                }
//                                override fun onError(error: Throwable?) {
//                                    rvdBluetoothListener.onConnectionFailure(Error(error?.localizedMessage))
//                                }
//                            })
                        }

                        override fun onBluetoothPairTo(devices: Array<out BluetoothDongle>?) {
                            val dongle = devices?.getOrNull(0)
                            if (dongle != null) sdk.setDongleToPairTo(dongle.address)
                        }

                        override fun onBluetoothPairingFinished() {
                            print("onBluetoothPairingFinished()")
                            rvdBluetoothListener.onMessageFromDevice("onBluetoothPairingFinished()")
                        }

                        override fun onBluetoothPairingError(error: SDKException?) {
                            // Param object should be instance of Throwable
                            // TODO: Send connection failure to rvdBluetoothListener
//                            rvdBluetoothListener.onConnectionFailure()
                            print(error)
                            if (error == null) return
                            rvdBluetoothListener.onMessageFromDevice(error.localizedMessage)
                        }

                        override fun onBluetoothPairingStarted() {
//                            deviceManager.getVin()
                            print("onBluetoothPairingStarted()")
                            rvdBluetoothListener.onMessageFromDevice("onBluetoothPairingStarted()")
                        }

                        override fun onBluetoothOn() {
                            print("onBluetoothOn()")
                            rvdBluetoothListener.onMessageFromDevice("onBluetoothOn()")
                        }

                        override fun onBluetoothOff() {
                            print("onBluetoothOff()")
                            rvdBluetoothListener.onMessageFromDevice("onBluetoothOff()")
                        }
                    })

                    // Network
                    sdk.addEventListener()?.onNetworkEvents(object: OnNetworkEvents {
                        override fun onInternetConnectionRequired() {
                            // TODO: Send failure to rvdBluetoothListener
//                          rvdBluetoothListener.onConnectionFailure(retObject as Error)
                            print("onInternetConnectionRequired()")
                            rvdBluetoothListener.onMessageFromDevice("onInternetConnectionRequired()")
                        }

                        override fun onRvdBackendConnectionProblem(error: NetworkError?) {
                            rvdBluetoothListener.onMessageFromDevice("onRvdBackendConnectionProblem error: ${error.toString()}")
                        }
                    })

                    sdk.addEventListener()?.onDongleEvents(object: OnDongleEvents {
                        override fun onDongleConnecting(toBluetoothDevice: BluetoothDongle?) {
//                            deviceManager.scanFinished()
                        }

                        override fun onDongleNotConfigured(notFirstBinding: Boolean?) {
                            rvdBluetoothListener.onBindingRequired()
                        }
                    })

                    sdk.addEventListener()?.onLicenseEvents(object: OnLicenseEvents {

                        // List of licenses available to the user
//                        override fun onApplicationAvailable(applications: MutableList<Subscription>?) {
//                        }

                        override fun onLicenseUnverifiable() {
                            rvdBluetoothListener.onConnectionFailure(Error("onLicenseUnverifiable()"))
                            rvdBluetoothListener.onMessageFromDevice("onLicenseUnverifiable()")
                        }

                        override fun onApplicationAvailable(applications: MutableList<Subscription>?) {
                            rvdBluetoothListener.onMessageFromDevice("onApplicationAvailable")
                        }

                        override fun onApplicationSetup() {
                            rvdBluetoothListener.onMessageFromDevice("onApplicationSetup()")
                        }

                        override fun onLicenseInvalid() {
                            rvdBluetoothListener.onMessageFromDevice("onLicenseInvalid()")
                        }

                        override fun onLicenseReceived(license: LicenseEntity?) {
                            rvdBluetoothListener.onMessageFromDevice("onLicenseReceived " + license!!)
                        }

                        override fun onLicenseValid(daysToExpire: Int) {
                            rvdBluetoothListener.onMessageFromDevice("onLicenseValid(daysToExpire: Int: )" + daysToExpire)
                        }
                    })

                    sdk.addEventListener()?.onAuthenticationEvents(object: OnAuthenticationEvents {
                        override fun onAuthenticationSuccess() {
                            rvdBluetoothListener.onMessageFromDevice("onAuthenticationSuccess()")
                        }

                        override fun onAuthenticationError(error: SDKException?) {
                            rvdBluetoothListener.onMessageFromDevice("onAuthenticationError error: ${error.toString()}")
                        }
                    })

                    sdk.addEventListener()?.onCarEvents(object: OnCarEvents {
                        override fun onCarConnected() {
                            rvdBluetoothListener.onConnectionCompleted()
                            deviceManager.onCompleted(RVDDevice(sdk!!, deviceManager))
                        }

                        override fun onCarDisconnected() {
                            rvdBluetoothListener.onMessageFromDevice("onCarDisconnected()")
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

    fun getAvailableSubscriptions(): Boolean {
//        val subscription: List<Subscription> = arrayListOf()
//        rvdBluetoothListener.onGotAvailableSubscriptions(AvailableSubscriptions(subscription, ""))
        if (sdk == null || sdk?.isInitialized == false) return false
        sdk?.getAvailableSubscriptions(object: Callback<AvailableSubscriptions>() {
            override fun onSuccess(subscriptions: AvailableSubscriptions?) {
                if (subscriptions == null) return
                rvdBluetoothListener.onGotAvailableSubscriptions(subscriptions)
            }
            override fun onError(error: Throwable?) {
                rvdBluetoothListener.onConnectionFailure(Error(error?.localizedMessage))
            }
        })
        return true
    }

    fun respondBindingRequest(start: Boolean): Boolean {
        Log.d(TAG,"respondBindingRequest() start: $start")
        if (start) sdk?.startBinding()
        return start
    }

    fun selectSubscription(subscription: Subscription) {
        sdk?.saveSelectedSubscription(subscription, object: Callback<Void>() {
            override fun onSuccess(success: Void?) {

            }

            override fun onError(error: Throwable?) {

            }
        })
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

}