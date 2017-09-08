package com.pitstop.ui.Notifications

import android.util.Log
import android.view.View
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetUserNotificationUseCase
import com.pitstop.utils.MixpanelHelper

/**
 * Created by ishan on 2017-09-05.
 */

class NotificationPresenter(val useCaseComponent: UseCaseComponent, val mixpanelHelper: MixpanelHelper){

    private val TAG = javaClass.simpleName
    private var view : NotificationView? = null


    fun subscribe(view : NotificationView){
        Log.d(TAG, "subscribed")
        this.view = view;

    }

    fun unsubscribe(){
        Log.d(TAG, "unsubscribed")
        this.view = null;

    }

    fun onRefresh(){

        Log.d("notificationPresenter", "refreshed")
    }

    fun onUpdateNeeded(){

    }


    fun onNotificationClicked(title: String){



    }





}


