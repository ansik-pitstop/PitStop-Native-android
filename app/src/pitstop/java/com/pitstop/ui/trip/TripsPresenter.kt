package com.pitstop.ui.trip

import android.util.Log
import com.pitstop.dependency.UseCaseComponent

/**
 * Created by Karol Zdebel on 2/27/2018.
 */
class TripsPresenter(val useCaseComponent: UseCaseComponent
                     , val tripActivityObservable: TripActivityObservable): TripActivityObserver {

    private val tag = javaClass.simpleName
    private var view: TripsView? = null

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

    override fun onTripActivity(tripActivity: TripActivity) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}