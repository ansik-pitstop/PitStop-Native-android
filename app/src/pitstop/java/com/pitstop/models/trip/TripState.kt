package com.pitstop.models.trip

/**
 * Created by Karol Zdebel on 7/10/2018.
 */

enum class TripStateType(val value: Int){
    TRIP_DRIVING_HARD(0)       //High confidence user is driving
    , TRIP_DRIVING_SOFT(1)     //Moderate confidence user is driving, you  cannot enter this state from the TRIP_DRIVING_HARD state
    , TRIP_STILL_SOFT(2)       //User was previously in a TRIP_DRIVING_SOFT state and is now still but we don't know the trip is over yet
    , TRIP_STILL_HARD(3)       //User was previously in a TRIP_DRIVING_HARD state and is now still but we don't know the trip is over yet
    , TRIP_END_SOFT(4)         //User was in a TRIP_DRIVING_SOFT state and were very confident trip has ended
    , TRIP_END_HARD(5)         //User was in a TRIP_DRIVING_HARD state and were very confidence trip has ended
    , TRIP_NONE(6)             //No trip is currently recording
    , TRIP_MANUAL(7)           //Manual trip start event triggered directly through user interaction with app
    , TRIP_MANUAL_END(8)       //Manual trip end event triggered directly through user interaction with the app
}

data class TripState(val tripStateType: TripStateType, val time: Long)