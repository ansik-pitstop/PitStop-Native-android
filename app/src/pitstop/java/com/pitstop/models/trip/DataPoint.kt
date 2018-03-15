package com.pitstop.models.trip

/**
 * Created by Karol Zdebel on 3/15/2018.
 */
data class DataPoint(val id: String, val data: String){
    companion object {
        const val ID_LATITUDE = "latitude"
        const val ID_LONGITUDE = "longtitude"
        const val ID_TRIP_INDICATOR = "tripIndicator"
        const val ID_DEVICE_TIMESTAMP = "deviceTimestamp"
        const val ID_TRIP_ID = "tripId"
        const val ID_VIN = "vin"
        const val ID_START_LOCATION = "startLocation"
        const val ID_END_LOCATION = "endLocation"
        const val ID_START_STREET_LOCATION = "startStreetLocation"
        const val ID_END_STREET_LOCATION = "endStreetLocation"
        const val ID_START_CITY_LOCATION = "startCityLocation"
        const val ID_END_CITY_LOCATION = "endCityLocation"
        const val ID_MILEAGE_TRIP = "mileage_trip"
        const val ID_START_TIMESTAMP = "start_timestamp"
        const val ID_END_TIMESTAMP = "end_timestamp"
    }
}