package com.pitstop.ui.alarms

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.get.GetAlarmsUseCase
import com.pitstop.interactors.set.SetAlarmsEnabledUseCase
import com.pitstop.models.Alarm
import com.pitstop.network.RequestError
import com.pitstop.utils.MixpanelHelper

/**
 * Created by ishan on 2017-10-30.
 */
class AlarmsPresenter(val useCaseComponent: UseCaseComponent, val mixpanelHelper: MixpanelHelper){

    private val TAG = javaClass.simpleName
    var alarmsView: AlarmsView? = null
    var currCarGot: Boolean = false
    var carId: Int = 0
    var alarmsMap : HashMap<String, ArrayList<Alarm>> = HashMap();
    var alarmsEnabled : Boolean = false;
    private var updating: Boolean = false;

    fun subscribe(view: AlarmsView) {
        this.alarmsView = view
    }

    fun unsubscribe() {
        this.alarmsView = null
    }

    fun onUpdateNeeded() {
        Log.d(TAG, "onUpdateNeeded")
        if (updating) return;
        updating = true
        alarmsView?.showLoading();

        useCaseComponent.alarmsUseCase.execute(object: GetAlarmsUseCase.Callback{
            override fun onAlarmsGot(alarms: HashMap<String, ArrayList<Alarm>>,  alarmsEnabled: Boolean) {
                updating = false;
                if (alarmsView == null) return;
                alarmsView?.setAlarmsEnabled(alarmsEnabled)
                if (alarms.isEmpty()) alarmsView?.noAlarmsView();
                else {
                    alarmsMap.clear();
                    for (key in alarms.keys){
                        alarmsMap[key] = alarms[key]!!;
                    }
                    alarmsView?.showAlarmsView()
                    alarmsView?.populateAlarms();
                    alarmsView?.hideLoading()
                }
            }
            override fun onError(error: RequestError) {
                updating = false;
                if (alarmsView == null) return
                alarmsView?.hideLoading()
                alarmsView?.errorLoadingAlarms();
            }
        })

    }

    fun enableAlarms() {
        Log.d(TAG, "enableAlarms")
        if (alarmsEnabled) return
        this.alarmsEnabled = true;
        alarmsView?.setAlarmsEnabled(true)
        useCaseComponent.setAlarmsEnableduseCase.execute(alarmsEnabled, object : SetAlarmsEnabledUseCase.Callback{
            override fun onAlarmsEnabledSet() {
            }

            override fun onError(error: RequestError) {
                if (alarmsView == null) return
                alarmsView?.toast("An error occurred, please check internet connection.")
            }
        })
    }

    fun disableAlarms() {
        Log.d(TAG, "disableAlarms")
        if (!alarmsEnabled)return
        this.alarmsEnabled = false;
        alarmsView?.setAlarmsEnabled(false)
        useCaseComponent.setAlarmsEnableduseCase.execute(alarmsEnabled, object : SetAlarmsEnabledUseCase.Callback{
            override fun onAlarmsEnabledSet() {
            }

            override fun onError(error: RequestError) {
                alarmsView?.toast("An error occurred, please try again")
            }
        })
    }

    fun refreshAlarms() {
        if (updating) return;
        updating = true
        useCaseComponent.alarmsUseCase.execute(object: GetAlarmsUseCase.Callback{
            override fun onAlarmsGot(alarms: HashMap<String, ArrayList<Alarm>>,  alarmsEnabled: Boolean) {
                updating = false;
                if (alarmsView == null) return;
                alarmsView?.setAlarmsEnabled(alarmsEnabled)
                if (alarms.isEmpty()) alarmsView?.noAlarmsView();
                else {
                    alarmsMap.clear();
                    for (key in alarms.keys){
                        alarmsMap[key] = alarms[key]!!;
                    }
                    alarmsView?.showAlarmsView()
                    alarmsView?.populateAlarms();
                }
            }
            override fun onError(error: RequestError) {
                updating = false;
                if (alarmsView == null) return
                alarmsView?.errorLoadingAlarms();
            }
        })

    }
}