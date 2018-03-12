package com.pitstop.ui.trip.settings

import android.util.Log
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationRequest
import com.pitstop.ui.trip.TripParameterSetter

/**
 * Created by Karol Zdebel on 3/7/2018.
 */
class TripSettingsPresenter {

    private val TAG = javaClass.simpleName

    private var tripParameterSetter: TripParameterSetter? = null
    private var view: TripSettingsView? = null

    fun setTripParameterSetter(tripParameterSetter: TripParameterSetter){
        Log.d(TAG,"setTripParameterSetter()")
        this.tripParameterSetter = tripParameterSetter
        onReadyForLoad()
    }

    fun onReadyForLoad(){
        Log.d(TAG,"onReadyForLoad()")
        if (tripParameterSetter != null && view != null){
            when (tripParameterSetter!!.getActivityTrigger()){
                DetectedActivity.IN_VEHICLE -> view?.showTrigger(0)
                DetectedActivity.ON_FOOT -> view?.showTrigger(1)
                DetectedActivity.ON_BICYCLE -> view?.showTrigger(2)
            }
            when (tripParameterSetter!!.getLocationUpdatePriority()){
                LocationRequest.PRIORITY_HIGH_ACCURACY -> view?.showLocationUpdatePriority(0)
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> view?.showLocationUpdatePriority(1)
                LocationRequest.PRIORITY_LOW_POWER -> view?.showLocationUpdatePriority(2)
                LocationRequest.PRIORITY_NO_POWER -> view?.showLocationUpdatePriority(3)
            }
            view?.showActivityUpdateInterval(tripParameterSetter!!.getActivityUpdateInterval())
            view?.showLocationUpdateInterval(tripParameterSetter!!.getLocationUpdateInterval())
            view?.showThresholdEnd(tripParameterSetter!!.getEndThreshold())
            view?.showThresholdStart(tripParameterSetter!!.getStartThreshold())
        }
    }

    fun onUpdateSelected(){
        Log.d(TAG,"onUpdate()")
        if (tripParameterSetter != null && view != null){

            val trigger = view!!.getTrigger()
            when (trigger){
                0 -> tripParameterSetter?.setActivityTrigger(DetectedActivity.IN_VEHICLE)
                1 -> tripParameterSetter?.setActivityTrigger(DetectedActivity.ON_FOOT)
                2 -> tripParameterSetter?.setActivityTrigger(DetectedActivity.ON_BICYCLE)
            }

            val priority = view!!.getLocationUpdatePriority()
            when (priority){
                0 -> tripParameterSetter?.setLocationUpdatePriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                1 -> tripParameterSetter?.setLocationUpdatePriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                2 -> tripParameterSetter?.setLocationUpdatePriority(LocationRequest.PRIORITY_LOW_POWER)
                3 -> tripParameterSetter?.setLocationUpdatePriority(LocationRequest.PRIORITY_NO_POWER)
            }

            try{
                val locationUpdateInterval = view!!.getLocationUpdateInterval().toLong()
                if (locationUpdateInterval < 1000){
                    view?.displayError("Invalid location update interval, input a number greater than 1000")
                }else{
                    tripParameterSetter?.setLocationUpdateInterval(locationUpdateInterval)
                }
            }catch(e: NumberFormatException){
                view?.displayError("Invalid location update interval, input a number greater than 1000")
            }

            try{
                val activityUpdateInterval = view!!.getActivityUpdateInterval().toLong()
                if (activityUpdateInterval <= 0){
                    view?.displayError("Invalid location update interval, input a number greater than 0")
                }else{
                    tripParameterSetter?.setActivityUpdateInterval(activityUpdateInterval)
                }
            }catch(e: NumberFormatException){
                view?.displayError("Invalid location update interval, input a number greater than 0")
            }

            try{
                val endThreshold = view!!.getThresholdEnd().toInt()
                if (endThreshold < 0 || endThreshold > 100){
                    view?.displayError("Invalid trip end threshold, input a number between 0 and 100")
                }else{
                    tripParameterSetter?.setEndThreshold(endThreshold)
                }
            }catch(e: NumberFormatException){
                view?.displayError("Invalid trip end threshold, input a number between 0 and 100")
            }

            try{
                val startThreshold = view!!.getThresholdStart().toInt()
                if (startThreshold < 0 || startThreshold > 100){
                    view?.displayError("Invalid trip start threshold, input a number between 0 and 100")
                }else{
                    tripParameterSetter?.setStartThreshold(startThreshold)
                }
            }catch(e: NumberFormatException){
                view?.displayError("Invalid trip start threshold, input a number between 0 and 100")
            }

        }

        view?.displayToast("Update processed, make take a moment to update")
    }

    fun subscribe(view: TripSettingsView){
        Log.d(TAG,"subscribe()")
        this.view = view
        if (tripParameterSetter == null && view.getTripParameterSetter() != null){
            tripParameterSetter = view.getTripParameterSetter()
            onReadyForLoad()
        }else{
            tripParameterSetter = view.getTripParameterSetter()
        }
    }

    fun unsubscribe(){
        Log.d(TAG,"unsubscribe()")
        this.view = null
    }
}