package com.pitstop.ui.trip

import android.location.Location
import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class TripsPresenter(val useCaseComponent: UseCaseComponent): TripActivityObserver {

    private val tag = javaClass.simpleName
    private var view: TripsView? = null
    private var tripActivityObservable: TripActivityObservable? = null

    fun onTripActivityObservableReady(tripActivityObservable: TripActivityObservable){
        Log.d(tag,"onTripActivityObservableReady()")
        this.tripActivityObservable = tripActivityObservable
        tripActivityObservable.subscribeTripActivity(this)
    }

    override fun onTripStart() {
        if (view != null) view?.displayTripActivity(getCurrentTime(),"Trip started")
    }

    override fun onTripUpdate(trip: List<Location>) {
        if (view != null) view?.displayTripActivity(getCurrentTime(),"Trip locations received")
    }

    override fun onTripEnd(trip: List<Location>) {
        if (view != null) view?.displayTripActivity(getCurrentTime(), "Trip ended")
    }

    fun subscribe(view: TripsView){
        Log.d(tag,"subscribe()")
        this.view = view
        if (tripActivityObservable != null)
            tripActivityObservable?.subscribeTripActivity(this)
    }

    fun unsubscribe(){
        Log.d(tag,"unsubscribe()")
        this.view = null
        if (tripActivityObservable != null)
            tripActivityObservable?.subscribeTripActivity(this)
    }

    fun onReadyForLoad(){
        Log.d(tag,"onReadyForLoad()")
        //Get trips use case
    }

    fun onClearClicked(){
        Log.d(tag,"onClearClicked()")
    }

    private fun getCurrentTime() = SimpleDateFormat("hh:mm aa",Locale.CANADA)
            .format(Date(System.currentTimeMillis()))

}