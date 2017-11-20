package com.pitstop.interactors.other

import android.os.Handler
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.ScannerRepository
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 11/3/2017.
 */
class DeviceClockSyncUseCaseImpl(private val scannerRepository: ScannerRepository
                                 , private val useCaseHandler: Handler
                                 , private val mainHandler: Handler): DeviceClockSyncUseCase {

    private val tag: String = javaClass.simpleName
    private var rtcTime: Long = 0
    private var deviceId: String = ""
    private var vin: String = ""
    private var callback: DeviceClockSyncUseCase.Callback? = null

    override fun execute(rtcTime: Long, deviceId: String, vin: String
                         , callback: DeviceClockSyncUseCase.Callback) {
        Logger.getInstance().logI(tag,"Use case started execution: rtcTime=$rtcTime, deviceId=$deviceId, vin=$vin"
                ,false, DebugMessage.TYPE_USE_CASE)
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
                Logger.getInstance().logI(tag,"Use case finished: success"
                        ,false, DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback!!.onClockSynced()})
            }

            override fun onError(error: RequestError) {
                Logger.getInstance().logE(tag,"Use case returned error: error=$error"
                        ,false, DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback!!.onError(error)})
            }

        })
    }
}