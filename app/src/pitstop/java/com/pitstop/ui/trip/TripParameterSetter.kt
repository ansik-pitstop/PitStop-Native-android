package com.pitstop.ui.trip

/**
 * Created by Karol Zdebel on 3/7/2018.
 */
interface TripParameterSetter {
    fun setStartThreshold(threshold: Int): Boolean
    fun getStartThreshold(): Int
    fun setEndThreshold(threshold: Int): Boolean
    fun getEndThreshold(): Int
    fun setLocationUpdateInterval(interval: Long)
    fun getLocationUpdateInterval(): Long
    fun setLocationUpdatePriority(priority: Int)
    fun getLocationUpdatePriority(): Int
    fun setActivityUpdateInterval(interval: Int)
    fun getActivityUpdateInterval(): Int
    fun setActivityTrigger(trigger: Int)
    fun getActivityTrigger(): Int
}