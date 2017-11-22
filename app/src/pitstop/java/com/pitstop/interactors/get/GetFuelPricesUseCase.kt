package com.pitstop.interactors.get

import android.location.Location
import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-11-16.
 */
interface GetFuelPricesUseCase: Interactor {

    interface Callback{
        fun onFuelPriceGot(fuelPrice: Double)
        fun onError(error: RequestError)
    }

    fun execute(postslCode: String?, callback: Callback)
}