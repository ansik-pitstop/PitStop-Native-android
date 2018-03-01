package com.pitstop.ui.trip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import kotlinx.android.synthetic.main.fragment_trips.*

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class TripsFragment: Fragment(),TripsView {

    private var tripsPresenter: TripsPresenter? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //Trip info receiver
        val intentFilter = IntentFilter()
        intentFilter.addAction(ActivityService.TRIP_START)
        intentFilter.addAction(ActivityService.TRIP_END)
        intentFilter.addAction(ActivityService.TRIP_UPDATE)
        activity.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if (tripsPresenter != null)
                    tripsPresenter?.onTripActivityReceived(p1)
            }
        },intentFilter)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (tripsPresenter != null) tripsPresenter?.unsubscribe()
    }

    override fun displayPastTrips(trips: List<Trip>) {
        Log.d(tag,"displayPastTrips()")
    }

    override fun clearTripActivity() {
        Log.d(tag,"clearTripActivity()")
    }

    override fun getTripActivityObservable(): TripActivityObservable? {
        Log.d(tag,"getTripActivityObservable()")
        return if (activity != null) (activity as MainActivity).tripActivityObservable
        else null
    }

    override fun displayTripActivity(time: String, activity: String) {
        activity_updates.append("$time > $activity\n")
    }
}