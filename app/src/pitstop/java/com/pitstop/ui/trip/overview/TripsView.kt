package com.pitstop.ui.trip.overview

import android.location.Location
import com.pitstop.ui.trip.TripActivityObservable

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
interface TripsView {
    fun displayPastTrips(trips: List<List<Location>>)
    fun clearTripActivity()
    fun getTripActivityObservable(): TripActivityObservable?
    fun displayTripActivity(time: String, activity: String)
}