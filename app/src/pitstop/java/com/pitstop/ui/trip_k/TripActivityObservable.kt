package com.pitstop.ui.trip_k

/**
 * Created by Karol Zdebel on 2/28/2018.
 */
interface TripActivityObservable {
    fun subscribeTripActivity(observer: TripActivityObserver)
    fun unsubscribeTripActivity(observer: TripActivityObserver)
    fun isTripInProgress(): Boolean
    fun startTripManually(): Boolean
    fun endTripManually(): Boolean
}