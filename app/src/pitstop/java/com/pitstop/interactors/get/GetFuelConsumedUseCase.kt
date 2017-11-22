package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-11-14.
 */
interface GetFuelConsumedUseCase:Interactor {
    interface Callback{
        fun onFuelConsumedGot(fuelConsumed: Double)
        fun onError(error: RequestError)
    }

    fun execute(scanner: String, callback :Callback)
}