package com.pitstop.ui.trip

import android.location.Location

/**
 * Created by Karol Zdebel on 2/28/2018.
 */
interface TripActivityObserver {
    fun onTripStart()
    fun onTripUpdate(trip: List<Location>)
    fun onTripEnd(trip: List<Location>)
}