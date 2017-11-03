package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.ScannerRepository
/**
 * Created by Karol Zdebel on 11/3/2017.
 */
class DeviceClockSyncUseCaseImpl(val scannerRepository: ScannerRepository
                                 , val useCaseHandler: Handler
                                 , val mainHandler: Handler): DeviceClockSyncUseCase {

    val tag: String = javaClass.simpleName
    var rtcTime: Long = 0
    var deviceId: String = ""
    var vin: String = ""
    var callback: DeviceClockSyncUseCase.Callback? = null

    override fun execute(rtcTime: Long, deviceId: String, vin: String
                         , callback: DeviceClockSyncUseCase.Callback) {
        this.rtcTime = rtcTime
        this.deviceId = deviceId
        this.vin = vin
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        scannerRepository.deviceClockSync(rtcTime,deviceId,vin.toUpperCase(),"scanner"
                , object: Repository.Callback<String>{

            override fun onSuccess(data: String) {
                Log.d(tag,"onSuccess() data: $data")
                mainHandler.post({callback!!.onClockSynced()})
            }

            override fun onError(error: RequestError) {
                Log.d(tag,"onError() error: $error")
                mainHandler.post({callback!!.onError(error)})
            }

        })
    }
}