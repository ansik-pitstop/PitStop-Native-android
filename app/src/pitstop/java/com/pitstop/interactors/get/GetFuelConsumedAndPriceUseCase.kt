package com.pitstop.interactors.get

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-11-29.
 */
interface GetFuelConsumedAndPriceUseCase: Interactor {
    interface Callback{
        fun onGotFuelConsumedAndPrice(price: Double, fuelConsumed: Double);
        fun onError(error: RequestError)
    }
    fun execute(lastknowLocation: String, scannerId: String, callback: Callback)
}