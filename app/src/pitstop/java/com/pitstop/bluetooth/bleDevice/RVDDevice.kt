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

class RVDDevice(private val rvdSDK: RvdApi, private val deviceManager: BluetoothDeviceManager)

    : AbstractDevice {

    private val TAG = RVDDevice::class.java.simpleName
    private val expectedPidList: MutableList<Int> = mutableListOf()
    private var currentPidPackage: RVDPidPackage? = null

    init{
        rvdSDK.addEventListener().onCarEvents(object: OnCarEvents {
            override fun onCarDisconnected() {
                deviceManager?.setState(BluetoothCommunicator.DISCONNECTED)
            }
        })

        rvdSDK.addEventListener().onDongleEvents(object: OnDongleEvents {
            override fun onDongleDisconnected(reason: BluetoothDisconnectionReason?) {
                deviceManager?.setState(BluetoothCommunicator.DISCONNECTED)
            }

            override fun onDongleConnected(toBluetoothDevice: BluetoothDongle?) {
                deviceManager?.setState(BluetoothCommunicator.CONNECTED)
            }

            override fun onDongleConnecting(toBluetoothDevice: BluetoothDongle?) {
                deviceManager?.setState(BluetoothCommunicator.CONNECTING)
            }
        })

        rvdSDK.addEventListener().onLiveReadingsEvents(object: OnLiveReadingsEvents {
            override fun onLiveReadingsError(error: SDKException?) {
                Log.e(TAG,"Live reading error!")
            }

            override fun onLiveReadingsReceived(retObject: LiveReadingSample?) {
                rvdSDK.getDongleInformation(object: Callback<DongleInformation>() {
                    override fun onSuccess(deviceInfo: DongleInformation?) {
                        if (deviceInfo != null) {
                            if (currentPidPackage == null) currentPidPackage = RVDPidPackage(deviceInfo.name, System.currentTimeMillis())
                            val liveReadingSample = retObject as LiveReadingSample
                            currentPidPackage!!.addPid(liveReadingSample.pid?.id.toString()
                                    ,liveReadingSample.value)
                            if (currentPidPackage!!.pids.size == expectedPidList.size)
                                deviceManager.onGotPids(currentPidPackage!!)
                        }
                    }
                    override fun onError(error: Throwable?) {
                        Log.d(TAG,"Error getting device info! error: $error")
                    }
                })
            }
        })
    }

    override fun getVin(): Boolean {
        Log.d(TAG,"getVin()")
        rvdSDK.getDongleInformation(object : Callback<DongleInformation>() {
            override fun onSuccess(deviceInfo: DongleInformation?) {
                if (deviceInfo != null)
                    rvdSDK.getVehicleVin(object: Callback<String>(){
                        override fun onSuccess(VIN: String?) {
                            Log.d(TAG,"Got VIN successfully! VIN: $VIN")
                            if (VIN != null)
                                deviceManager.onGotVin(VIN,deviceInfo.name)
                        }

                        override fun onError(error: Throwable?) {
                            Log.d(TAG,"Error getting VIN! error: $error")
                        }
        })

            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting device info! error: $error")
            }

        })

        return true
    }

    override fun setPidsToSend(pids: List<String>, timeInterval: Int): Boolean {
        Log.d(TAG,"setPidsToSend() pids: $pids, timeInterval: $timeInterval")
        pids.forEach {
            rvdSDK.startLiveReading(LiveReadingId(it.toInt(),0,"",""),timeInterval)
        }
        expectedPidList.addAll(pids.map{ it.toInt() })
        return true
    }

    //Add a timeout in case not all pids that were requested are returned, once the timeout occurs return all pids found in the package
    override fun getPids(pids: List<String>): Boolean {
        Log.d(TAG,"getPids() pids: $pids")
        pids.forEach {

            rvdSDK.getDongleInformation(object: Callback<DongleInformation>(){
                override fun onSuccess(deviceInfo: DongleInformation?) {
                    if (deviceInfo != null){
                        val pidPackage = RVDPidPackage(deviceInfo.name,System.currentTimeMillis())
                        rvdSDK.sampleLiveReading(LiveReadingId(it.toInt(),0,"",""), object : Callback<LiveReadingSample>
                        (){

                            override fun onSuccess(liveReadingSample: LiveReadingSample?) {
                                Log.d(TAG,"Got live reading sample: $liveReadingSample")
                                if (liveReadingSample != null)
                                    pidPackage.addPid(liveReadingSample.pid?.id.toString(),liveReadingSample.value)
                                if (pidPackage.pids.size == pids.size) deviceManager.onGotPids(pidPackage)
                            }

                            override fun onError(error: Throwable?) {
                                Log.d(TAG,"Error getting live reading sample! error: $error")
                            }

                        })
                    }
                }

                override fun onError(error: Throwable?) {
                    Log.d(TAG,"Error getting device info! error: $error")
                }

            })
        }
        return true
    }

    override fun getSupportedPids(): Boolean {
        Log.d(TAG,"getSupportedPids()")
        rvdSDK.getDongleInformation(object: Callback<DongleInformation>()
        {
            override fun onSuccess(deviceInfo: DongleInformation?) {
                if (deviceInfo != null)
                    rvdSDK.getAvailableLiveReading(object : Callback<Map<LiveReadingId,Boolean>>()
                    {
                        override fun onSuccess(supportedPids: Map<LiveReadingId, Boolean>?) {
                            if (supportedPids != null){
                                val supportedList = mutableListOf<String>()
                                supportedPids.filter { it.value }.forEach {
                                    supportedList.add(it.key.id.toString())
                                }
                                deviceManager.onGotSupportedPids(supportedList,deviceInfo.name)
                            }
                        }

                        override fun onError(error: Throwable?) {
                            Log.d(TAG,"Error getting available live reading! $error")
                        }

                    })
            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting device info! $error")
            }

        })
        return true
    }

    override fun requestSnapshot(): Boolean {
        Log.d(TAG,"requestSnapshot()")
        //Get all available pids then ask for them all
        rvdSDK.getDongleInformation(object: Callback<DongleInformation>()
        {
            override fun onSuccess(deviceInfo: DongleInformation?) {
                if (deviceInfo != null){
                    val pidPackage = RVDPidPackage(deviceInfo.name, System.currentTimeMillis())
                    rvdSDK.getAvailableLiveReading(object: Callback<Map<LiveReadingId,Boolean>>(){
                        override fun onSuccess(availableLiveReading: Map<LiveReadingId, Boolean>?) {
                            if (availableLiveReading != null)
                                availableLiveReading.filter{true}.forEach {
                                    rvdSDK.sampleLiveReading(it.key, object: Callback<LiveReadingSample>(){
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
                            Log.d(TAG,"Error getting available live reading! error: $error")
                        }

                    })
                }
            }

            override fun onError(error: Throwable?) {
                Log.e(TAG,"Error getting device info! error: $error")
            }

        })
        return true
    }

    override fun getDtcs(): Boolean {
        Log.d(TAG,"getDtcs()")
        rvdSDK.getDongleInformation(object: Callback<DongleInformation>(){
            override fun onSuccess(deviceInfo: DongleInformation?) {
                if (deviceInfo != null) rvdSDK.readDiagnosticTroubleCodes(object: Callback<DiagnosticTroubleCodes>(){
                    override fun onSuccess(dtcItem: DiagnosticTroubleCodes?) {
                        Log.d(TAG,"Successfully got dtc item: $dtcItem")
                        if (dtcItem != null){
                            val allEngineCodes = mutableMapOf<String,Boolean>()
                            dtcItem.emissionsRelatedPendingFaultCodes.forEach {
                                it.faultCodesWithDescription.forEach {
                                    allEngineCodes[it.code.toString()] = false
                                }
                            }
                            dtcItem.emissionsRelatedStoredFaultCodes.forEach{
                                it.faultCodesWithDescription.forEach {
                                    allEngineCodes[it.code.toString()] = true
                                }
                            }
                            //Manufacturer specific fault code below is commented out because not sure if our back-end supports it
//                            dtcItem.manufacturerSpecificFaultCodes.numericFaultCodes.forEach({
//                                it.value.forEach { allEngineCodes.add(it.faultCode,true) }
//                            })

                            deviceManager.onGotDtcData(DtcPackage(deviceInfo.name
                                    , System.currentTimeMillis().toString(), allEngineCodes))
                        }

                    }

                    override fun onError(error: Throwable?) {
                        Log.d(TAG,"Error getting dtcs: $error")
                    }
                })
            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting device info! error: $error")
            }

        })

        return true
    }

    override fun getPendingDtcs(): Boolean {
        Log.d(TAG,"getPendingDtcs()")

        rvdSDK.getDongleInformation(object: Callback<DongleInformation>(){
            override fun onSuccess(deviceInfo: DongleInformation?) {

                if (deviceInfo != null) rvdSDK.readDiagnosticTroubleCodes(object: Callback<DiagnosticTroubleCodes>(){
                    override fun onSuccess(dtcItem: DiagnosticTroubleCodes?) {
                        Log.d(TAG,"Successfully got dtc item: $dtcItem")
                        if (dtcItem != null){
                            val allEngineCodes = mutableMapOf<String,Boolean>()
                            dtcItem.emissionsRelatedPendingFaultCodes.forEach {
                                it.faultCodesWithDescription.forEach {
                                    allEngineCodes[it.code.toString()] = false
                                }
                            }
                            deviceManager.onGotDtcData(DtcPackage(deviceInfo.name
                                    , System.currentTimeMillis().toString(), allEngineCodes))
                        }

                    }

                    override fun onError(error: Throwable?) {
                        Log.d(TAG,"Error getting dtcs: $error")
                    }
                })
            }

            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error getting device info! error: $error")
            }

        })
        return true
    }

    override fun closeConnection(): Boolean {
        Log.d(TAG,"closeConnection")
        rvdSDK.deleteAndroidDevicesTrustedByDongle(object: Callback<Boolean>()
        {
            override fun onSuccess(success: Boolean?) {
                Log.d(TAG,"unpairDevices() callback value: $success")
                deviceManager.setState(BluetoothCommunicator.DISCONNECTED)
            }
            override fun onError(error: Throwable?) {
                Log.d(TAG,"Error closing connection! error: $error")
            }
      })

        return true
    }

    override fun getCommunicatorState(): Int {
        return  if (rvdSDK.isDongleConnected) BluetoothCommunicator.CONNECTED
        else BluetoothCommunicator.DISCONNECTED
    }
}