package com.pitstop.interactors.add

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 10/11/2017.
 */
interface AddDtcUseCase: Interactor {

    interface Callback{
        fun onDtcAdded()
        fun onError(requestError: RequestError)
    }

    fun execute(dtc: String, isPending: Boolean, rtcTime: Long, callback: Callback)
}