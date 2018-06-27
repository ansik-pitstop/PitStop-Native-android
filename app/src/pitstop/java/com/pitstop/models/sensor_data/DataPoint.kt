package com.pitstop.models.sensor_data

/**
 * Created by Karol Zdebel on 3/15/2018.
 */
data class DataPoint(val id: String, val data: String){
    companion object {
        const val ID_LATITUDE = "latitude"
        const val ID_LONGITUDE = "longitude"
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
        const val ID_MILEAGE_OBD215B = "device_mileage"
        const val ID_START_TIMESTAMP = "start_timestamp"
        const val ID_END_TIMESTAMP = "end_timestamp"
        const val ID_START_LATITUDE = "start_latitude"
        const val ID_END_LATITUDE = "end_latitude"
        const val ID_START_LONGTITUDE = "start_longitude"
        const val ID_END_LONGITUDE = "end_longitude"
        const val ID_DEVICE_TYPE = "deviceType"
        const val ID_DEVICE_ID = "deviceId"
        const val ID_RTC = "bluetoothDeviceTime"
    }
}