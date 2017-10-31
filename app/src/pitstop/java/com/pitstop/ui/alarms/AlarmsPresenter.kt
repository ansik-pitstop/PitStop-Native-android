package com.pitstop.ui.alarms

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetAlarmsUseCase
import com.pitstop.interactors.get.GetUserCarUseCase
import com.pitstop.models.Alarm
import com.pitstop.models.Car
import com.pitstop.models.Dealership
import com.pitstop.network.RequestError
import com.pitstop.utils.MixpanelHelper
import java.util.ArrayList

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsPresenter(val useCaseComponent: UseCaseComponent, val mixpanelHelper: MixpanelHelper) {

    private val TAG = javaClass.simpleName;
    val alarmList = ArrayList<Alarm>()
    var alarmsView : AlarmsView? = null;

    fun subscribe(view: AlarmsView){
        this.alarmsView = view;
    }

    fun unsubscribe(){
        this.alarmsView = null
    }

    fun onUpdateNeeded() {
       useCaseComponent.userCarUseCase.execute(object: GetUserCarUseCase.Callback{
           override fun onCarRetrieved(car: Car?, dealership: Dealership?) {
               Log.d(TAG, "getAlarms(): " + car?.id.toString());
               if(alarmsView == null) return
               useCaseComponent.alarmsUseCase.execute(car!!.id, object : GetAlarmsUseCase.Callback{
                   override fun onAlarmsGot(list: MutableList<Alarm>) {
                       if (alarmsView==null) return
                       alarmList.clear();
                       for(alarm in list){
                           alarmList.add(alarm);
                       }
                       alarmsView?.populateAlarms();
                   }

                   override fun onError(error: RequestError) {
                       if (alarmsView==null) return;
                       alarmsView?.errorView();

                   }


               })
           }

           override fun onNoCarSet() {
               TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
           }

           override fun onError(error: RequestError?) {
               TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
           }


       })
    }




}