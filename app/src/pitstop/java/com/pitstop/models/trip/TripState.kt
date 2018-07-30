package com.pitstop.models.trip

/**
 * Created by Karol Zdebel on 7/10/2018.
 */

enum class TripStateType(val value: Int){
    TRIP_DRIVING_HARD(0)
    , TRIP_DRIVING_SOFT(1)
    , TRIP_STILL_SOFT(2)
    , TRIP_STILL_HARD(3)
    , TRIP_END_SOFT(4)
    , TRIP_END_HARD(5)
    , TRIP_NONE(6)
    , TRIP_MANUAL(7)
    , TRIP_MANUAL_END(8)
}

data class TripState(val tripStateType: TripStateType, val time: Long)