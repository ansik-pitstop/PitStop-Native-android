package com.pitstop.observer

/**
 * Created by ishan on 2017-11-15.
 */
interface FuelObserver: Observer {

    fun onFuelConsumedUpdated(fuelCOnsumed: Double)
}