package com.pitstop.ui.trip

import android.location.Location
import android.util.Log
import com.pitstop.dependency.UseCaseComponent

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

    override fun onTripStart() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTripUpdate(trip: List<Location>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTripEnd(trip: List<Location>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}