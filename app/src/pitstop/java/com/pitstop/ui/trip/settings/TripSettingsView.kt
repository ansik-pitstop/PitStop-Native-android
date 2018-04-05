package com.pitstop.ui.trip.settings

import com.pitstop.ui.trip.TripParameterSetter

/**
 * Created by Karol Zdebel on 3/7/2018.
 */
interface TripSettingsView {
    fun showTrigger(trigger: Int)
    fun showLocationUpdatePriority(priority: Int)
    fun showLocationUpdateInterval(interval: Long)
    fun showActivityUpdateInterval(interval: Long)
    fun showThresholdStart(threshold: Int)
    fun showThresholdEnd(threshold: Int)
    fun showStillActivityTimeout(timeout: String)
    fun getTrigger(): Int
    fun getLocationUpdatePriority(): Int
    fun getLocationUpdateInterval(): String
    fun getActivityUpdateInterval(): String
    fun getThresholdStart(): String
    fun getThresholdEnd(): String
    fun getStillActivityTimeout(): String
    fun displayError(err: String)
    fun getTripParameterSetter(): TripParameterSetter?
    fun displayToast(msg: String)
}