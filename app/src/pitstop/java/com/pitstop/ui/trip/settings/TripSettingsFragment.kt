package com.pitstop.ui.trip.settings

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.pitstop.R
import kotlinx.android.synthetic.main.fragment_trip_settings.*


/**
 * Created by Karol Zdebel on 3/7/2018.
 */
class TripSettingsFragment: Fragment(), TripSettingsView {

    var presenter: TripSettingsPresenter? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_trip_settings, container)
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (presenter == null) presenter = TripSettingsPresenter()
        presenter?.subscribe(this)

        val triggersArray = resources.getStringArray(R.array.detected_activities)
        val locationPrioritiesArray = resources.getStringArray(R.array.location_priority)

        triggers.adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, triggersArray)
        location_priority.adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, locationPrioritiesArray)

        presenter?.onReadyForLoad()
    }

    override fun onDestroyView() {
        if (presenter != null) presenter?.unsubscribe()
        super.onDestroyView()
    }

    override fun showTrigger(trigger: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showLocationUpdatePriority(priority: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showLocationUpdateInterval(interval: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showActivityUpdateInterval(interval: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showThresholdStart(threshold: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showThresholdEnd(threshold: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTrigger(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLocationUpdatePriority(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLocationUpdateInterval(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getActivityUpdateInterval(): String{
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getThresholdStart(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getThresholdEnd(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun displayError(err: String) {
        Toast.makeText(context,err,Toast.LENGTH_LONG).show()
    }
}