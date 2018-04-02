package com.pitstop.ui.trip

import android.location.Location

/**
 * Created by Karol Zdebel on 2/28/2018.
 */
data class TripActivity(val type: TripType, val location: List<Location>?) {
    enum class TripType{
        START,UPDATE,END
    }
}