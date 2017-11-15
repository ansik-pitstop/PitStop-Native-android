package com.pitstop.bluetooth.handler

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.other.StoreFuelConsumedUseCase
import com.pitstop.network.RequestError
import com.pitstop.observer.FuelObservable

/**
 * Created by ishan on 2017-11-15.
 */
class FuelHandler(private val fuelObservable: FuelObservable, private val useCaseComponent: UseCaseComponent) {
   val  TAG: String = FuelHandler::class.java.simpleName;

    fun handleFuelUpdate(fuelConsumed: Double):Unit{
        useCaseComponent.storeFuelConsumedUseCase.execute(fuelConsumed, object: StoreFuelConsumedUseCase.Callback{

            override fun onFuelConsumedStored(fuelConsumed: Double) {
                fuelObservable.notifyFuelConsumedUpdate(fuelConsumed)
            }

            override fun onError(error: RequestError) {
                Log.d(TAG, "FuelConsumedUpdateError()")
            }
        })
    }




}