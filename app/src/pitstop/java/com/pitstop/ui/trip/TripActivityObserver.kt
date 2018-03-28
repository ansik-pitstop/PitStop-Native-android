package com.pitstop.ui.trip

import com.pitstop.models.trip.PendingLocation

/**
 * Created by Karol Zdebel on 2/28/2018.
 */
interface TripActivityObserver {
    fun onTripStart()
    fun onTripUpdate()
    fun onTripEnd(trip: List<PendingLocation>)
}