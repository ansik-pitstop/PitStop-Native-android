package com.pitstop.ui.alarms

import com.pitstop.dependency.UseCaseComponent
import com.pitstop.models.Alarm
import com.pitstop.models.Dealership
import com.pitstop.utils.MixpanelHelper
import java.util.ArrayList

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsPresenter(val useCaseComponent: UseCaseComponent, val mixpanelHelper: MixpanelHelper) {

    val alarmList = ArrayList<Alarm>()
    var alarmsView : AlarmsView? = null;

    fun subscribe(view: AlarmsView){
        this.alarmsView = view;
    }

    fun unsubscribe(){
        this.alarmsView = null
    }

    fun onUpdateNeeded() {

    }


}