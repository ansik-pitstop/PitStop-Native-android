package com.pitstop.utils

import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationRequest

/**
 * Created by Karol Zdebel on 3/7/2018.
 */
class TripUtils {
    companion object {
        fun isLocationPriorityValid(priority: Int): Boolean{
            return when (priority){
                LocationRequest.PRIORITY_HIGH_ACCURACY -> true
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> true
                LocationRequest.PRIORITY_LOW_POWER -> true
                LocationRequest.PRIORITY_NO_POWER -> true
                else -> false
            }
        }

        fun locationPriorityToString(priority: Int): String{
            return when (priority){
                LocationRequest.PRIORITY_HIGH_ACCURACY -> "High accuracy"
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> "Balanced power accuracy"
                LocationRequest.PRIORITY_LOW_POWER -> "Low power"
                LocationRequest.PRIORITY_NO_POWER -> "No power"
                else -> "Unknown"
            }
        }

        fun activityToString(activity: Int): String{
            return when (activity) {
                DetectedActivity.ON_FOOT -> "On foot"
                DetectedActivity.ON_BICYCLE -> "On bicycle"
                DetectedActivity.IN_VEHICLE -> "In vehicle"
                DetectedActivity.RUNNING -> "Running"
                DetectedActivity.STILL -> "Still"
                DetectedActivity.TILTING -> "Tilting"
                DetectedActivity.WALKING -> "Walking"
                DetectedActivity.UNKNOWN -> "Unknown"
                else -> "Unknown"
            }
        }
    }
}