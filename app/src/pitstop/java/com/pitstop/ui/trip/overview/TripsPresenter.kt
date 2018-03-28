package com.pitstop.ui.trip.overview

import android.location.Location
import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetTripsUseCase
import com.pitstop.network.RequestError
import com.pitstop.ui.trip.TripActivityObservable
import com.pitstop.ui.trip.TripActivityObserver
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class TripsPresenter(val useCaseComponent: UseCaseComponent): TripActivityObserver {

    private val tag = javaClass.simpleName
    private var view: TripsView? = null
    private var tripActivityObservable: TripActivityObservable? = null
    private var tripsDisplayed: ArrayList<List<Location>>? = null

    fun onTripActivityObservableReady(tripActivityObservable: TripActivityObservable){
        Log.d(tag,"onTripActivityObservableReady()")
        this.tripActivityObservable = tripActivityObservable
        tripActivityObservable.subscribeTripActivity(this)
    }

    override fun onTripStart() {
        Log.d(tag,"onTripStart()")
        if (view != null) view?.displayTripActivity(getCurrentTime(),"Trip started")
    }

    override fun onTripUpdate() {
        Log.d(tag,"onTripUpdate()")
        if (view != null) view?.displayTripActivity(getCurrentTime(),"Trip locations received")
    }

    override fun onTripEnd(trip: List<Location>) {
        Log.d(tag,"onTripEnd()")
        if (view != null){
            view?.displayTripActivity(getCurrentTime(), "Trip ended")
            tripsDisplayed?.add(trip)
            view?.refreshTrips()
        }
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
        useCaseComponent.getTripUseCase.execute(object: GetTripsUseCase.Callback{
            override fun onGotTrips(trips: List<List<Location>>) {
                tripsDisplayed = trips as ArrayList<List<Location>>
                if (view != null) view?.displayPastTrips(trips)
            }

            override fun onError(err: RequestError) {
            }

        })
    }

    fun onClearClicked(){
        Log.d(tag,"onClearClicked()")
    }

    private fun getCurrentTime() = SimpleDateFormat("hh:mm aa",Locale.CANADA)
            .format(Date(System.currentTimeMillis()))

}