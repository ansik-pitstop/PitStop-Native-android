package com.pitstop.models.trip

/**
 * Created by Karol Zdebel on 6/4/2018.
 */

data class CarActivity(val vin: String, val time: Long, val type: Int, val conf: Int){
    companion object {
        val TYPE_DRIVING = 1
        val TYPE_ON_FOOT = 2
        val TYPE_STILL = 3
        val TYPE_MANUAL_START = 4
        val TYPE_MANUAL_END = 5
        val TYPE_OTHER = 6
        val TYPE_STILL_TIMEOUT = 7
    }
}