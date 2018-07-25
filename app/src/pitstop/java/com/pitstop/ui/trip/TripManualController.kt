package com.pitstop.ui.trip

import com.pitstop.models.trip.TripState
import io.reactivex.Observable

/**
 * Created by Karol Zdebel on 7/25/2018.
 */
interface TripManualController {

    fun startTripManual()
    fun endTripManual()
    fun getTripState(): Observable<TripState>

}