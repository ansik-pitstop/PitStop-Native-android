package com.pitstop.models.trip

/**
 * Created by Karol Zdebel on 6/4/2018.
 */

data class CarActivity(val vin: String, val time: Long, val type: Int, val conf: Int){
    companion object {
        val TYPE_DRIVING: Int = 1
        val TYPE_ON_FOOT: Int = 2
        val TYPE_STILL: Int = 3
        val TYPE_MANUAL_START: Int = 4
        val TYPE_MANUAL_END: Int = 5
        val TYPE_OTHER: Int = 6
        val TYPE_STILL_TIMEOUT: Int = 7
    }
}