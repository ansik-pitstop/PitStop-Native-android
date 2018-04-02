package com.pitstop.ui.trip_k

import android.location.Location

/**
 * Created by Karol Zdebel on 2/28/2018.
 */
interface TripActivityListener {
    fun onTripStart(location: Location)
    fun onTripUpdate(location: Location)
    fun onTripEnd(location: Location)
}