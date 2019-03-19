package com.pitstop.bluetooth.searcher

import android.util.Log
import com.continental.rvd.mobile_sdk.*
import com.continental.rvd.mobile_sdk.errors.NetworkError
import com.continental.rvd.mobile_sdk.errors.SDKException
import com.continental.rvd.mobile_sdk.events.*
import com.continental.rvd.mobile_sdk.internal.license.domain.LicenseEntity
import com.pitstop.bluetooth.BluetoothDeviceManager
import com.pitstop.bluetooth.bleDevice.RVDDevice
import com.pitstop.bluetooth.communicator.BluetoothCommunicator
import com.pitstop.bluetooth.communicator.IBluetoothCommunicator
import java.lang.Error

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDBluetoothDeviceSearcher(private val sdkIntentService: RvdIntentService
                                 , private val rvdBluetoothListener: RVDBluetoothDeviceSearcherStatusListener
                                 , private val deviceManager: BluetoothDeviceManager) {

    private val TAG = RVDBluetoothDeviceSearcher::class.java.simpleName
    private var sdk: RvdApi? = null
    private var connectedDongle: BluetoothDongle? = null
    private var rvdDevice: RVDDevice? = null

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

                    rvdDevice = RVDDevice(sdk!!, deviceManager)

                    // Bluetooth
                    sdk.addEventListener()!!.onBluetoothEvents(object: OnBluetoothEvents {
                        override fun onBluetoothConnectTo(devicesAvailableForConnection: Array<out BluetoothDongle>?) {
                            val dongle = devicesAvailableForConnection?.getOrNull(0)
                            sdk.setDongleToConnectTo(dongle)
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
                            if (error == null) return
                            rvdBluetoothListener.onMessageFromDevice(error.localizedMessage)
                        }
                    })

                    // Network
                    sdk.addEventListener()!!.onNetworkEvents(object: OnNetworkEvents {
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

                    sdk.addEventListener()!!.onBindingEvents(object: OnBindingEvents {
                        override fun onBindingCanBeResumed() {
                            rvdBluetoothListener.onBindingRequired()
                        }

                        override fun onBindingError(error: SDKException?) {
                            rvdBluetoothListener.onMessageFromDevice("onBindingError: ${error.toString()}")
                        }

                        override fun onBindingFinished() {
                            rvdBluetoothListener.onBindingFinished()
                            deviceIsReadyToUse()
                        }

                        override fun onBindingNotNeeded() {
                            rvdBluetoothListener.onMessageFromDevice("onBindingNotNeeded()")
                        }

                        override fun onBindingProcessChanged(progress: Float) {
                            rvdBluetoothListener.onMessageFromDevice("progress: $progress")
                            rvdBluetoothListener.onBindingProgress(progress)
                        }

                        override fun onBindingUserInput(bindingQuestion: BindingQuestion?) {
                            rvdBluetoothListener.onBindingQuestionPrompted(bindingQuestion!!)
                        }

                    })

                    sdk.addEventListener()!!.onDongleEvents(object: OnDongleEvents {
                        override fun onDongleConnecting(toBluetoothDevice: BluetoothDongle?) {
                            rvdBluetoothListener.onMessageFromDevice("onDongleConnecting: $toBluetoothDevice")
                        }

                        override fun onDongleNotConfigured(notFirstBinding: Boolean?) {
                            rvdBluetoothListener.onMessageFromDevice("onDongleNotConfigured notFirstBinding: $notFirstBinding")
                            rvdBluetoothListener.onBindingRequired()
                        }

                        override fun onDongleConfigured() {
                            rvdBluetoothListener.onMessageFromDevice("onDongleConfigured")
                        }

                        override fun onDongleConnected(toBluetoothDevice: BluetoothDongle?) {
                            if (toBluetoothDevice != null) {
                                rvdBluetoothListener.onMessageFromDevice("Connected to: ${toBluetoothDevice.name}")
                            }
                            rvdDevice?.bluetoothDevice = toBluetoothDevice
                        }

                        override fun onDongleDisconnected(reason: BluetoothDisconnectionReason?) {
//                            rvdBluetoothListener.onMessageFromDevice("onDongleDisconnected: ${reason.toString()}")
                        }
                    })

                    sdk.addEventListener()!!.onLicenseEvents(object: OnLicenseEvents {
                        override fun onLicenseInvalid() {
                            rvdBluetoothListener.onMessageFromDevice("You license is invalid")
                        }

                        override fun onLicenseReceived(license: LicenseEntity?) {
                            rvdDevice?.license = license
                        }
                    })

                    sdk.addEventListener()!!.onAuthenticationEvents(object: OnAuthenticationEvents {
                        override fun onAuthenticationSuccess() {
                        }

                        override fun onAuthenticationError(error: SDKException?) {
                            rvdBluetoothListener.onMessageFromDevice("onAuthenticationError error: ${error.toString()}")
                        }
                    })

                    sdk.addEventListener()!!.onCarEvents(object: OnCarEvents {
                        override fun onCarConnected() {
                            rvdBluetoothListener.onMessageFromDevice("onCarConnected()")
                            if (!sdk.isBindingNeeded) {
                                deviceIsReadyToUse()
                            }
                        }

                        override fun onCarDisconnected() {
//                            rvdBluetoothListener.onMessageFromDevice("onCarDisconnected()")
                            deviceManager.setState(IBluetoothCommunicator.DISCONNECTED)
                        }

                        override fun onCarMoving(isMoving: Boolean) {
                            rvdBluetoothListener.onMessageFromDevice("onCarMoving $isMoving")
                        }
                    })

                }

                override fun onError(p0: Throwable?) {
                    Log.d(TAG,"Failed to initialize RVD SDK")
                }
            })
        }
        return true
    }

    private fun deviceIsReadyToUse() {
        val rvdDevice = rvdDevice
        if (rvdDevice != null && sdk?.isVehicleConnected == true) {
            deviceManager.onCompleted(rvdDevice)
            deviceManager.setState(IBluetoothCommunicator.CONNECTED)
        }
    }

    fun getAvailableSubscriptions(): Boolean {
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

    fun startBinding(start: Boolean): Boolean {
        Log.d(TAG,"respondBindingRequest()")
        if (start) {
            if (sdk?.bindingStatus == BindingStatus.State.STARTED) return true
            if (sdk?.isVehicleConnected == true && sdk?.canBindingBeResumed() == true) {
                rvdBluetoothListener.onMessageFromDevice("Resuming binding")
                sdk?.resumeBinding()
            } else {
                sdk?.startBinding()
            }
        } else {
            deviceIsReadyToUse()
        }
        return true
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