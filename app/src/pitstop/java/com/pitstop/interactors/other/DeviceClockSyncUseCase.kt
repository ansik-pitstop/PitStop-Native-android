package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 11/3/2017.
 */
interface DeviceClockSyncUseCase: Interactor {

    interface Callback{
        fun onClockSynced()
        fun onError(error: RequestError)
    }

    fun execute(rtcTime: Long, deviceId: String, vin: String, callback: Callback)
}