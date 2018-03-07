package com.pitstop.ui.trip

/**
 * Created by Karol Zdebel on 3/7/2018.
 */
interface TripParameterSetter {
    fun setStartThreshold(threshold: Int): Boolean
    fun getStartThreshold(): Int
    fun setEndThreshold(threshold: Int): Boolean
    fun getEndThreshold(): Int
    fun setLocationUpdateInterval(interval: Long): Boolean
    fun getLocationUpdateInterval(): Long
    fun setLocationUpdatePriority(priority: Int): Boolean
    fun getLocationUpdatePriority(): Int
    fun setActivityUpdateInterval(interval: Long): Boolean
    fun getActivityUpdateInterval(): Long
    fun setActivityTrigger(trigger: Int): Boolean
    fun getActivityTrigger(): Int
}