package com.pitstop.ui.alarms

import com.pitstop.models.Alarm
import com.pitstop.network.RequestError

/**
 * Created by ishan on 2017-10-30.
 */
interface AlarmsView {
    fun populateAlarms(isDealershipMercedes: Boolean)
    fun noAlarmsView()
    fun showAlarmsView()
    fun setAlarmsEnabled(alarmsEnabled: Boolean)
    fun errorLoadingAlarms()
    fun toast(message: String)
    fun showLoading()
    fun onAlarmClicked(alarm: Alarm);
}