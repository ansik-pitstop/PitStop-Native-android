package com.pitstop.observer

/**
 * Created by ishan on 2017-11-15.
 */
interface FuelObservable: Subject {
    fun notifyFuelConsumedUpdate(fuelConsumed: Double)
}