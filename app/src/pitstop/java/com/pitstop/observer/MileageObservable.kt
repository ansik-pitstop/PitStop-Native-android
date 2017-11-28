package com.pitstop.observer

/**
 * Created by ishan on 2017-11-23.
 */
interface MileageObservable: Subject {
    fun requestRtcAndMileage();
}
