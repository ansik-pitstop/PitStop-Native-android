package com.pitstop.models.trip

/**
 * Created by Karol Zdebel on 7/10/2018.
 */

enum class TripStateType(val value: Int){
    TRIP_DRIVING_HARD(0)
    , TRIP_DRIVING_SOFT(1)
    , TRIP_STILL(2)
    , TRIP_END_SOFT(3)
    , TRIP_END_HARD(4)
    , TRIP_NONE(5)
}

data class TripState(val tripStateType: TripStateType, val time: Long)