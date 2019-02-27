package com.pitstop.bluetooth.bleDevice

import android.util.Log
import com.continental.rvd.mobile_sdk.*
import com.continental.rvd.mobile_sdk.errors.SDKException
import com.continental.rvd.mobile_sdk.events.OnCarEvents
import com.continental.rvd.mobile_sdk.events.OnDongleEvents
import com.continental.rvd.mobile_sdk.events.OnLiveReadingsEvents
import com.pitstop.bluetooth.BluetoothDeviceManager
import com.pitstop.bluetooth.communicator.BluetoothCommunicator
import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.bluetooth.dataPackages.RVDPidPackage
import java.lang.Exception

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RVDDevice(private val rvdSDK: RvdApi, private val deviceManager: BluetoothDeviceManager)
    : AbstractDevice, OnDongleEvents, OnCarEvents, OnLiveReadingsEvents {

    private val TAG = RVDDevice::class.java.simpleName
    private val expectedPidList: MutableList<Int> = mutableListOf()
    private var currentPidPackage: RVDPidPackage? = null

    init{
        //We need to register all events related to disconnecting from device and getting live data
        rvdSDK.addEventListener().onLiveReadingsEvents(this)
        rvdSDK.addEventListener().onDongleEvents(this)
        rvdSDK.addEventListener().onCarEvents(this)
    }


    // MARK: Dongle methods
    override fun onDongleConnected(toBluetoothDevice: BluetoothDongle?) {
        deviceManager?.setState(BluetoothCommunicator.CONNECTED)
    }

    override fun onDongleConnecting(toBluetoothDevice: BluetoothDongle?) {
        deviceManager?.setState(BluetoothCommunicator.CONNECTING)
    }

    override fun onDongleDisconnected(reason: BluetoothDisconnectionReason?) {
        deviceManager?.setState(BluetoothCommunicator.DISCONNECTED)
    }

    // MARK: Dongle methods
    override fun onCarDisconnected() {
        deviceManager?.setState(BluetoothCommunicator.DISCONNECTED)
    }

    override fun onLiveReadingsReceived(sample: LiveReadingSample?) {
        rvdSDK.getDongleInformation(object: Callback<DongleInformation>() {
            override fun onSuccess(dongleInformation: DongleInformation?) {
                if (dongleInformation != null){
                    if (currentPidPackage == null) currentPidPackage = RVDPidPackage(dongleInformation.name, System.currentTimeMillis())
                    val liveReadingSample = sample as LiveReadingSample
                    currentPidPackage!!.addPid(liveReadingSample.pid?.id.toString()
                            ,liveReadingSample.value)
                    if (currentPidPackage!!.pids.size == expectedPidList.size)
                        deviceManager.onGotPids(currentPidPackage!!)
                }
            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting dongle info! error: $error")
            }
        })
    }

    override fun onLiveReadingsError(error: SDKException?) {
        Log.e(TAG,"Live reading error!")
    }

    override fun getVin(): Boolean {
        Log.d(TAG,"getVin()")

        rvdSDK.getDongleInformation(object: Callback<DongleInformation>() {
            override fun onSuccess(dongleInformation: DongleInformation?) {
                if (dongleInformation == null) return
                val deviceName = dongleInformation!!.name

                rvdSDK.getVehicleVin(object: Callback<String>() {
                    override fun onSuccess(vin: String?) {
                        deviceManager.onGotVin(vin!!, deviceName)
                    }

                    override fun onError(error: Throwable?) {
                        Log.d(TAG,"Error getting VIN! error: $error")
                    }
                })
            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting dongle info! error: $error")
            }
        })
        return true
    }

    override fun setPidsToSend(pids: List<String>, timeInterval: Int): Boolean {
        Log.d(TAG,"setPidsToSend() pids: $pids, timeInterval: $timeInterval")
        pids.forEach {
            rvdSDK.startLiveReading(LiveReadingId(it.toInt(), 0, "", ""), timeInterval)
        }
        expectedPidList.addAll(pids.map{ it.toInt() })
        return true
    }

    //Add a timeout in case not all pids that were requested are returned, once the timeout occurs return all pids found in the package
    override fun getPids(pids: List<String>): Boolean {
        Log.d(TAG,"getPids() pids: $pids")
        pids.forEach {
            rvdSDK.getDongleInformation(object: Callback<DongleInformation>() {
                override fun onSuccess(dongleInformation: DongleInformation?) {
                    if (dongleInformation != null){
                        val pidPackage = RVDPidPackage(dongleInformation.name, System.currentTimeMillis())
                        rvdSDK.sampleLiveReading(LiveReadingId(it.toInt(), 0, "", ""), object: Callback<LiveReadingSample>() {
                            override fun onSuccess(liveReadingSample: LiveReadingSample?) {
                                Log.d(TAG,"Got live reading sample: $liveReadingSample")
                                if (liveReadingSample != null)
                                    pidPackage.addPid(liveReadingSample.pid?.id.toString(), liveReadingSample.value)
                                if (pidPackage.pids.size == pids.size) deviceManager.onGotPids(pidPackage)
                            }

                            override fun onError(error: Throwable?) {
                                Log.d(TAG,"Error getting live reading sample! error: $error")
                            }
                        })
                    }
                }

                override fun onError(error: Throwable?) {
                    Log.d(TAG,"Error getting dongle info! error: $error")
                }
            })

        }
        return true
    }

    override fun getSupportedPids(): Boolean {
        Log.d(TAG,"getSupportedPids()")
        rvdSDK.getDongleInformation(object: Callback<DongleInformation>() {
            override fun onSuccess(dongleInformation: DongleInformation?) {
                if (dongleInformation != null){
                    rvdSDK.getAvailableLiveReading(object: Callback<Map<LiveReadingId, Boolean>>() {
                        override fun onSuccess(supportedPids: Map<LiveReadingId, Boolean>?) {
                            if (supportedPids != null){
                                val supportedList = mutableListOf<String>()
                                supportedPids.filter { it.value }.forEach {
                                    supportedList.add(it.key.id.toString())
                                }
                                deviceManager.onGotSupportedPids(supportedList, dongleInformation.name)
                            }
                        }

                        override fun onError(error: Throwable?) {
                            Log.d(TAG,"Error getting available live reading! $error")
                        }
                    })
                }
            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting dongle info! error: $error")
            }
        })
        return true
    }

    override fun requestSnapshot(): Boolean {
        Log.d(TAG,"requestSnapshot()")
        //Get all available pids then ask for them all
        rvdSDK.getDongleInformation(object: Callback<DongleInformation>() {
            override fun onSuccess(dongleInformation: DongleInformation?) {
                if (dongleInformation != null) {
                    val pidPackage = RVDPidPackage(dongleInformation.name, System.currentTimeMillis())
                    rvdSDK.getAvailableLiveReading(object: Callback<Map<LiveReadingId, Boolean>>() {
                        override fun onSuccess(availableLiveReading: Map<LiveReadingId, Boolean>?) {
                            if (availableLiveReading != null)
                                availableLiveReading.filter{true}.forEach {

                                    rvdSDK.sampleLiveReading(it.key, object: Callback<LiveReadingSample>() {
                                        override fun onSuccess(liveReadingSample: LiveReadingSample?) {
                                            if (liveReadingSample != null){
                                                pidPackage.addPid(liveReadingSample.pid?.id.toString(),liveReadingSample.value)
                                            }
                                            if (pidPackage.pids.size == availableLiveReading.filter { true }.size){
                                                deviceManager.onGotPids(pidPackage)
                                            }
                                        }
                                        override fun onError(error: Throwable?) {
                                            Log.d(TAG,"Error getting sample live reading! error: $error")
                                        }
                                    })
                                }
                        }

                        override fun onError(error: Throwable?) {
                            Log.d(TAG,"Error getting available live reading! $error")
                        }
                    })
                }
            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting dongle info! error: $error")
            }
        })
        return true
    }

    override fun getDtcs(): Boolean {
        Log.d(TAG,"getDtcs()")
        rvdSDK.getDongleInformation(object: Callback<DongleInformation>() {
            override fun onSuccess(dongleInformation: DongleInformation?) {
                if (dongleInformation != null) {

                    rvdSDK.readDiagnosticTroubleCodes(object: Callback<DiagnosticTroubleCodes>() {
                        override fun onSuccess(dtcItem: DiagnosticTroubleCodes?) {
                            val allEngineCodes = mutableMapOf<String,Boolean>()
                            dtcItem!!.emissionsRelatedPendingFaultCodes.forEach {
                                it.faultCodesWithDescription.forEach {
                                    allEngineCodes[it.code.toString()] = false
                                }
                            }
                            dtcItem!!.emissionsRelatedStoredFaultCodes.forEach{
                                it.faultCodesWithDescription.forEach {
                                    allEngineCodes[it.code.toString()] = true
                                }
                            }
                            //Manufacturer specific fault code below is commented out because not sure if our back-end supports it
//                            dtcItem.manufacturerSpecificFaultCodes.numericFaultCodes.forEach({
//                                it.value.forEach { allEngineCodes.add(it.faultCode,true) }
//                            })

                            deviceManager.onGotDtcData(DtcPackage(dongleInformation.name
                                    , System.currentTimeMillis().toString(), allEngineCodes))
                        }

                        override fun onError(error: Throwable?) {
                            Log.d(TAG,"Error on getting DTC\'s! error: $error")
                        }
                    })
                }
            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting dongle info! error: $error")
            }
        })


        return true
    }

    override fun getPendingDtcs(): Boolean {
        Log.d(TAG,"getPendingDtcs()")
        rvdSDK.getDongleInformation(object: Callback<DongleInformation>() {
            override fun onSuccess(dongleInformation: DongleInformation?) {
                if (dongleInformation != null) {
                    rvdSDK.readDiagnosticTroubleCodes(object: Callback<DiagnosticTroubleCodes>() {
                        override fun onSuccess(dtcItem: DiagnosticTroubleCodes?) {
                            Log.d(TAG,"Successfully got dtc item: $dtcItem")
                            if (dtcItem != null) {
                                val allEngineCodes = mutableMapOf<String,Boolean>()
                                dtcItem.emissionsRelatedPendingFaultCodes.forEach {
                                    it.faultCodesWithDescription.forEach {
                                        allEngineCodes[it.code.toString()] = false
                                    }
                                }
                                deviceManager.onGotDtcData(DtcPackage(dongleInformation.name
                                        , System.currentTimeMillis().toString(), allEngineCodes))
                            }
                        }

                        override fun onError(error: Throwable?) {
                            Log.d(TAG,"Error getting dtcs: $error")
                        }
                    })
                }
            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting dongle info! error: $error")
            }
        })

        return true
    }

    override fun closeConnection(): Boolean {
        Log.d(TAG,"closeConnection")
        return try {
            rvdSDK.destroy()
            Log.d(TAG,"Connection closed")
            true
        } catch (error: Exception) {
            Log.d(TAG,"Failed to close connection, $error")
            false
        }
    }

    override fun getCommunicatorState(): Int {
        return if (rvdSDK.isDongleConnected) {
            BluetoothCommunicator.CONNECTED
        } else {
            BluetoothCommunicator.DISCONNECTED
        }
    }
}