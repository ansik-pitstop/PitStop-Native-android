package com.pitstop.ui.trip.settings

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.pitstop.R
import com.pitstop.ui.trip.TripParameterSetter
import kotlinx.android.synthetic.main.fragment_trip_settings.*


/**
 * Created by Karol Zdebel on 3/7/2018.
 */
class TripSettingsFragment: Fragment(), TripSettingsView {

    private val TAG = javaClass.simpleName

    private var presenter: TripSettingsPresenter? = null
    private var tripsParameterSetter: TripParameterSetter? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_trip_settings, null)
        return rootView
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (presenter == null) presenter = TripSettingsPresenter()
        presenter?.subscribe(this)
        if (tripsParameterSetter != null) presenter!!.setTripParameterSetter(tripsParameterSetter!!)

        update_button.setOnClickListener({presenter?.onUpdateSelected()})

        presenter?.onReadyForLoad()
    }

    override fun onDestroyView() {
        if (presenter != null) presenter?.unsubscribe()
        super.onDestroyView()
    }

    fun onTripParameterSetterReady(tripParameterSetter: TripParameterSetter){
        Log.d(TAG,"onTripParameterSetterReady()")
        this.tripsParameterSetter = tripsParameterSetter
        if (presenter != null){
            presenter?.setTripParameterSetter(tripParameterSetter)
            presenter?.onReadyForLoad()
        }
    }

    override fun getTripParameterSetter(): TripParameterSetter? = tripsParameterSetter

    override fun showLocationUpdatePriority(priority: Int) {
        Log.d(TAG,"showLocationUpdatePriority() priority: "+priority)
        location_priority.setSelection(priority)
    }

    override fun showLocationUpdateInterval(interval: Long) {
        location_update_interval.setText(interval.toString())
    }

    override fun showActivityUpdateInterval(interval: Long) {
        activity_update_interval.setText(interval.toString())
    }

    override fun showThresholdStart(threshold: Int) {
        trip_start_threshold.setText(threshold.toString())
    }

    override fun showThresholdEnd(threshold: Int) {
        trip_end_threshold.setText(threshold.toString())
    }

    override fun showStillActivityTimeout(timeout: String) {
        still_activity_timeout.setText(timeout)
    }

    override fun showMinimumLocationAccuracy(acc: Int) {
        minimum_location_accuracy.setText(acc.toString())
    }

    override fun getLocationUpdatePriority(): Int {
        return location_priority.selectedItemPosition
    }

    override fun getLocationUpdateInterval(): String {
        return location_update_interval.text.toString()
    }

    override fun getActivityUpdateInterval(): String{
        return activity_update_interval.text.toString()
    }

    override fun getThresholdStart(): String {
        return trip_start_threshold.text.toString()
    }

    override fun getThresholdEnd(): String {
        return trip_end_threshold.text.toString()
    }

    override fun getStillActivityTimeout(): String {
        return still_activity_timeout.text.toString()
    }

    override fun getMinimumLocationAccuracy(): String {
        return minimum_location_accuracy.text.toString()
    }

    override fun displayError(err: String) {
        Toast.makeText(context,err,Toast.LENGTH_LONG).show()
    }

    override fun displayToast(msg: String) {
        Toast.makeText(context,msg,Toast.LENGTH_SHORT).show()
    }
}