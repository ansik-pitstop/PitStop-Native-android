package com.pitstop.ui.trip

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.models.Trip

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class TripsFragment: Fragment(),TripsView {
    lateinit var tripsPresenter: TripsPresenter

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootview = inflater!!.inflate(R.layout.fragment_trips, null)
        return rootview
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun displayPastTrips(trips: List<Trip>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clearTripActivity() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}