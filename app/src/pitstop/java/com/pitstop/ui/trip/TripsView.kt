package com.pitstop.ui.trip

import com.pitstop.models.Trip

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
interface TripsView {
    fun displayPastTrips(trips: List<Trip>)
    fun clearTripActivity()
    fun getTripActivityObservable(): TripActivityObservable?
    fun displayTripActivity(time: String, activity: String)
}