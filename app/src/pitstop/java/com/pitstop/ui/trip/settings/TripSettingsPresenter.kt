package com.pitstop.ui.trip.settings

import android.util.Log
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
    }

    fun onReadyForLoad(){
        Log.d(TAG,"onReadyForLoad()")
        if (tripParameterSetter != null && view != null){
            view?.showActivityUpdateInterval(tripParameterSetter!!.getActivityUpdateInterval())
            view?.showLocationUpdateInterval(tripParameterSetter!!.getLocationUpdateInterval())
            view?.showLocationUpdatePriority(tripParameterSetter!!.getLocationUpdatePriority())
            view?.showThresholdEnd(tripParameterSetter!!.getEndThreshold())
            view?.showThresholdStart(tripParameterSetter!!.getStartThreshold())
            view?.showTrigger(tripParameterSetter!!.getActivityTrigger())
        }
    }

    fun onUpdateSelected(){
        Log.d(TAG,"onUpdate()")
        if (tripParameterSetter != null && view != null){
            tripParameterSetter?.setActivityTrigger(view!!.getTrigger())
            tripParameterSetter?.setLocationUpdatePriority(view!!.getLocationUpdatePriority())

            try{
                val locationUpdateInterval = view!!.getLocationUpdateInterval().toInt()
                if (locationUpdateInterval < 1000){
                    view?.displayError("Invalid location update interval, input a number greater than 1000")
                }
            }catch(e: NumberFormatException){
                view?.displayError("Invalid location update interval, input a number greater than 1000")
            }

            try{
                val activityUpdateInterval = view!!.getActivityUpdateInterval().toInt()
                if (activityUpdateInterval <= 0){
                    view?.displayError("Invalid location update interval, input a number greater than 0")
                }
            }catch(e: NumberFormatException){
                view?.displayError("Invalid location update interval, input a number greater than 0")
            }

            try{
                val endThreshold = view!!.getThresholdEnd().toInt()
                if (endThreshold < 0 || endThreshold > 100){
                    view?.displayError("Invalid trip end threshold, input a number between 0 and 100")
                }
            }catch(e: NumberFormatException){
                view?.displayError("Invalid trip end threshold, input a number between 0 and 100")
            }

            try{
                val startThreshold = view!!.getThresholdStart().toInt()
                if (startThreshold < 0 || startThreshold > 100){
                    view?.displayError("Invalid trip start threshold, input a number between 0 and 100")
                }
            }catch(e: NumberFormatException){
                view?.displayError("Invalid trip start threshold, input a number between 0 and 100")
            }

        }
    }

    fun subscribe(view: TripSettingsView){
        Log.d(TAG,"subscribe()")
        this.view = view
    }

    fun unsubscribe(){
        Log.d(TAG,"unsubscribe()")
        this.view = null
    }
}