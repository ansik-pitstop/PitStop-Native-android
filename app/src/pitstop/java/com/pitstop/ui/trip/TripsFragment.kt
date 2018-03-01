package com.pitstop.ui.trip

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.models.Trip
import com.pitstop.ui.main_activity.MainActivity

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class TripsFragment: Fragment(),TripsView {
    private var tripsPresenter: TripsPresenter? = null
    private var tripActivityObservable: TripActivityObservable? = null

    fun setTripActivityObservable(tripActivityObservable: TripActivityObservable){
        Log.d(tag,"setTripActivityObservable()")
        this.tripActivityObservable = tripActivityObservable
        if (tripsPresenter != null)
            tripsPresenter?.onTripActivityObservableReady(tripActivityObservable)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootview = inflater!!.inflate(R.layout.fragment_trips, null)
        return rootview
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (tripsPresenter == null){
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(context)).build()
            tripsPresenter = TripsPresenter(useCaseComponent)
        }
        tripsPresenter?.subscribe(this)
        if (tripActivityObservable != null){
            tripsPresenter?.onTripActivityObservableReady(tripActivityObservable!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (tripsPresenter != null) tripsPresenter?.unsubscribe()
    }

    override fun displayPastTrips(trips: List<Trip>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearTripActivity() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTripActivityObservable(): TripActivityObservable? {
        return if (activity != null) (activity as MainActivity).tripActivityObservable
        else null
    }
}