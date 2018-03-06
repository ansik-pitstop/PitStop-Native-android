package com.pitstop.ui.trip

import android.location.Location
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.ui.main_activity.MainActivity
import kotlinx.android.synthetic.main.fragment_trips.*

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class TripsFragment: Fragment(),TripsView {

    private val TAG = javaClass.simpleName

    private var tripsPresenter: TripsPresenter? = null
    private var tripActivityObservable: TripActivityObservable? = null
    private var tripsAdapter: TripsAdapter? = null

    fun setTripActivityObservable(tripActivityObservable: TripActivityObservable){
        Log.d(TAG,"setTripActivityObservable()")
        this.tripActivityObservable = tripActivityObservable
        if (tripsPresenter != null)
            tripsPresenter?.onTripActivityObservableReady(tripActivityObservable)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //Trip info receiver

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
        tripsPresenter?.onReadyForLoad()
        if (tripActivityObservable != null){
            tripsPresenter?.onTripActivityObservableReady(tripActivityObservable!!)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (tripsPresenter != null) tripsPresenter?.unsubscribe()
    }

    override fun displayPastTrips(trips: List<List<Location>>) {
        Log.d(TAG,"displayPastTrips()")
        tripsAdapter = TripsAdapter(trips)
        past_trips.adapter = tripsAdapter
        past_trips.layoutManager = LinearLayoutManager(context)
    }

    override fun clearTripActivity() {
        Log.d(TAG,"clearTripActivity()")
    }

    override fun getTripActivityObservable(): TripActivityObservable? {
        Log.d(TAG,"getTripActivityObservable()")
        return if (activity != null) (activity as MainActivity).tripActivityObservable
        else null
    }

    override fun displayTripActivity(time: String, activity: String) {
        activity_updates.append("$time > $activity\n")
    }
}