package com.pitstop.interactors.other

import com.pitstop.interactors.Interactor
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-11-14.
 */
interface StoreFuelConsumedUseCase: Interactor {
    interface Callback {
        fun onFuelConsumedStored(fuelConsumed: Double)
        fun onError(error: RequestError);
    }
    fun execute(fuelConsumed: Double, callback: Callback);
}