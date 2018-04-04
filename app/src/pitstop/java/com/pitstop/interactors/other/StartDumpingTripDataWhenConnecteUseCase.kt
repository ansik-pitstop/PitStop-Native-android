package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 3/21/2018.
 */
interface StartDumpingTripDataWhenConnecteUseCase: Interactor {

    interface Callback{
        fun started()
        fun onError(error: RequestError)
    }

    fun execute(callback: Callback)
}