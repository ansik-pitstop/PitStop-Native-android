package com.pitstop.ui.trip

import android.location.Location

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
interface TripsView {
    fun displayPastTrips(trips: List<List<Location>>)
    fun clearTripActivity()
    fun getTripActivityObservable(): TripActivityObservable?
    fun displayTripActivity(time: String, activity: String)
}