package com.pitstop.ui.alarms

import com.pitstop.models.Alarm

/**
 * Created by ishan on 2017-10-30.
 */
interface AlarmsView {
    fun populateAlarms()
    fun noAlarmsView()
    fun showAlarmsView()
    fun setAlarmsEnabled(alarmsEnabled: Boolean)
    fun errorLoadingAlarms()
    fun toast(message: String)
    fun showLoading()
    fun hideLoading()
    fun onAlarmClicked(alarm: Alarm);
}