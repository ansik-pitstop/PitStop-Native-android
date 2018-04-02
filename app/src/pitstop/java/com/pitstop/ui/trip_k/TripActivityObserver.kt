package com.pitstop.ui.trip_k

import com.pitstop.models.trip_k.PendingLocation

/**
 * Created by Karol Zdebel on 2/28/2018.
 */
interface TripActivityObserver {
    fun onTripStart()
    fun onTripUpdate()
    fun onTripEnd(trip: List<PendingLocation>)
}