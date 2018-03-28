package com.pitstop.ui.trip

import android.location.Location

/**
 * Created by Karol Zdebel on 2/28/2018.
 */
interface TripActivityObservable {
    fun subscribeTripActivity(observer: TripActivityObserver)
    fun unsubscribeTripActivity(observer: TripActivityObserver)
    fun isTripInProgress(): Boolean
}