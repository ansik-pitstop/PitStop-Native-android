package com.pitstop.ui.services

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.utils.MixpanelHelper

/**
 * Created by Karol Zdebel on 2/1/2018.
 */
public class MainServicesPresenter(val usecaseComponent: UseCaseComponent
                                   , val mixpanelHelper: MixpanelHelper){

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

}