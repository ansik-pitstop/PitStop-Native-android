package com.pitstop.ui.trip

import io.reactivex.Observable

/**
 * Created by Karol Zdebel on 7/25/2018.
 */
interface TripManualController {

    fun startTripManual()
    fun endTripManual()
    fun getTripState(): Observable<Boolean>

}