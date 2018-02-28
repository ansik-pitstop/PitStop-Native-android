package com.pitstop.ui.trip

/**
 * Created by Karol Zdebel on 2/28/2018.
 */
interface TripActivityObservable {
    fun subscribeTripActivity(observer: TripActivityObserver)
    fun unsubscribeTripActivity(observer: TripActivityObserver)
}