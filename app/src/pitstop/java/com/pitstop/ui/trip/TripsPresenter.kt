package com.pitstop.ui.trip

import android.content.Intent
import android.location.Location
import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class TripsPresenter(val useCaseComponent: UseCaseComponent) {

    private val tag = javaClass.simpleName
    private var view: TripsView? = null

    fun onTripActivityReceived(intent: Intent?){
        Log.d(tag, "onReceive action: {$intent?.action}")
        when(intent?.action){
            ActivityService.TRIP_UPDATE -> {
                val trip: ArrayList<Location>
                        = ArrayList(intent?.getParcelableArrayListExtra(ActivityService.TRIP_EXTRA))
                if (view != null) view?.displayTripActivity(getCurrentTime(),"Trip locations received")
            }
            ActivityService.TRIP_START -> {
                if (view != null) view?.displayTripActivity(getCurrentTime(),"Trip started")
            }
            ActivityService.TRIP_END -> {
                val trip: ArrayList<Location>
                        = ArrayList(intent?.getParcelableArrayListExtra(ActivityService.TRIP_EXTRA))
                if (view != null) view?.displayTripActivity(getCurrentTime(), "Trip ended")
            }
        }
    }

    fun subscribe(view: TripsView){
        Log.d(tag,"subscribe()")
        this.view = view
    }

    fun unsubscribe(){
        Log.d(tag,"unsubscribe()")
        this.view = null
    }

    fun onReadyForLoad(){
        Log.d(tag,"onReadyForLoad()")
        //Get trips use case
    }

    fun onClearClicked(){
        Log.d(tag,"onClearClicked()")
    }

    private fun getCurrentTime() = SimpleDateFormat("hh:mm aa",Locale.CANADA)
            .format(Date(System.currentTimeMillis()))

}