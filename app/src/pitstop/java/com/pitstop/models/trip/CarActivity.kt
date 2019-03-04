package com.pitstop.models.trip

/**
 * Created by Karol Zdebel on 6/4/2018.
 */

data class CarActivity(val vin: String, val time: Long, val type: Int, val conf: Int){
    companion object {
        const val TYPE_DRIVING: Int = 1           //User is driving
        const val TYPE_ON_FOOT: Int = 2           //User is walking or running
        const val TYPE_STILL: Int = 3             //User is still
        const val TYPE_MANUAL_START: Int = 4      //User started trip manually using button in trip tab
        const val TYPE_MANUAL_END: Int = 5        //User ended trip manually using button in trip tab
        const val TYPE_OTHER: Int = 6             //An unknown event
        const val TYPE_STILL_TIMEOUT: Int = 7     //User has been still for a long period of time
    }
}