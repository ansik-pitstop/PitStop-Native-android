package com.pitstop.ui.services

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.utils.MixpanelHelper

/**
 * Created by Karol Zdebel on 2/1/2018.
 */
public class MainServicesPresenter(private val usecaseComponent: UseCaseComponent
                                   , private val mixpanelHelper: MixpanelHelper){

    private val tag = javaClass.simpleName
    private var view: MainServicesView? = null

    fun subscribe(view :MainServicesView){
        Log.d(tag,"subscribe()")
        this.view = view
    }

    fun unsubscribe(){
        Log.d(tag,"unsubscribe()")
        this.view = null
    }

    //Find out which view (#1, #2, or #3) is appropriate to display and command the view to do so
    //Should be called when the view is created, typically after the subscribe() call is made
    fun loadView(){
        Log.d(tag,"loadView()")
    }

    //Should prompt view to open dialog
    fun onMileageUpdateClicked(){
        Log.d(tag,"onMileageUpdateClicked()")
    }

    //Launch update mileage use case
    fun onMileageUpdateInput(mileage: Double){
        Log.d(tag,"onMileageUpdateInput() mileage: "+mileage)
    }

    //Invoke beginRequestService() on view
    fun onRequestAppointmentClicked(){
        Log.d(tag,"onRequestAppointmentClicked()");

    }


}