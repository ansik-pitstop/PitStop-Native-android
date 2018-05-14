package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.ScannerRepository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 11/3/2017.
 */
class DeviceClockSyncUseCaseImpl(private val scannerRepository: ScannerRepository
                                 , private val userRepository: UserRepository
                                 , private val carRepository: CarRepository
                                 , private val useCaseHandler: Handler
                                 , private val mainHandler: Handler): DeviceClockSyncUseCase {

    private val tag: String = javaClass.simpleName
    private var rtcTime: Long = 0
    private var deviceId: String = ""
    private var vin: String = ""
    private var callback: DeviceClockSyncUseCase.Callback? = null

    override fun execute(rtcTime: Long, deviceId: String, vin: String
                         , callback: DeviceClockSyncUseCase.Callback) {
        Logger.getInstance().logI(tag, "Use case started execution: bluetoothDeviceTime=$rtcTime, deviceId=$deviceId, vin=$vin"
                , DebugMessage.TYPE_USE_CASE)
        this.rtcTime = rtcTime
        this.deviceId = deviceId
        this.vin = vin
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {

        //If the deviceId or VIN is missing then attempt to retrieve it from the car repository
        if (deviceId.isEmpty() || vin.isEmpty()){
            userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
                override fun onSuccess(data: Settings?) {
                    if (data?.hasMainCar() == true){
                        carRepository.get(data.carId)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.from(useCaseHandler.looper),true)
                                .subscribe({next ->
                                    if (next.isLocal) return@subscribe //Only use remote responses
                                    if (next.data == null){
                                        Log.e(tag,"Data is null")
                                        onErrorSyncingClock(RequestError.getUnknownError())
                                    }else{
                                        Log.d(tag,"car scanner id: ${next.data.scannerId}, device id in execute: $deviceId")
                                        //Use deviceId directly from device since its not associated with car on backend
                                        if (!deviceId.isEmpty() && next.data.scannerId.isEmpty()){
                                            Log.d(tag,"using deviceId: $deviceId, vin: ${next.data.vin}")
                                            deviceClockSync(rtcTime, deviceId, next.data.vin)
                                        }
                                        //Use deviceId and VIN both which are associated with the currently selected car by user
                                        else if (next.data.scannerId != null){
                                            Log.d(tag,"using deviceId: ${next.data.scannerId}, vin: ${next.data.vin}")
                                            deviceClockSync(rtcTime, next.data.scannerId, next.data.vin)
                                        }else{
                                            Log.e(tag,"no device id to associate with vehicle")
                                            onErrorSyncingClock(RequestError.getUnknownError())
                                        }
                                    }
                                },{err ->
                                    onErrorSyncingClock(RequestError(err))
                                })

                    }else{
                        Log.e(tag,"No main car found")
                        onErrorSyncingClock(RequestError.getUnknownError())
                    }
                }

                override fun onError(error: RequestError?) {
                    Log.e(tag,"error retrieving settings")
                    onErrorSyncingClock(error ?: RequestError.getUnknownError())
                }

            })
        }
        //Otherwise use values provided directly by the device
        else{
            Log.d(tag,"using deviceId: $deviceId, vin: $vin")
            deviceClockSync(rtcTime,deviceId,vin)
        }
    }

    private fun onClockSynced(){
        Logger.getInstance().logI(tag, "Use case finished: success"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onClockSynced()})
    }

    private fun onErrorSyncingClock(error: RequestError){
        Logger.getInstance().logE(tag, "Use case returned error: error=$error"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback!!.onError(error)})
    }

    private fun deviceClockSync(rtcTime: Long, deviceId: String, vin: String){
        scannerRepository.deviceClockSync(rtcTime,deviceId,vin.toUpperCase(),"scanner"
                , object: Repository.Callback<String>{

            override fun onSuccess(data: String) {
                onClockSynced()
            }

            override fun onError(error: RequestError) {
                Log.e(tag,"error syncing clock")
                onErrorSyncingClock(error)
            }

        })
    }
}