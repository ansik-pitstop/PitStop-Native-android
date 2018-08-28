package com.pitstop.models.sensor_data

/**
 * Created by Karol Zdebel on 3/15/2018.
 */
data class DataPoint(val id: String, val data: String){
    companion object {
        //Location point latitude
        const val ID_LATITUDE = "latitude"
        //Location point longitude
        const val ID_LONGITUDE = "longitude"
        //Whether the data points is a trip indicator packet which marks the end of a trip
        const val ID_TRIP_INDICATOR = "tripIndicator"
        //Time on the phone when the data point was received
        const val ID_DEVICE_TIMESTAMP = "deviceTimestamp"
        //The id of the trip usually represented by the first deviceTimestamp
        const val ID_TRIP_ID = "tripId"
        //VIN of the vehicle associated with the data point
        const val ID_VIN = "vin"
        //Starting location of the entire trip
        const val ID_START_LOCATION = "startLocation"
        //Ending location of the entire trip
        const val ID_END_LOCATION = "endLocation"
        //Street which the trip began on
        const val ID_START_STREET_LOCATION = "startStreetLocation"
        //Street which the trip ended on
        const val ID_END_STREET_LOCATION = "endStreetLocation"
        //City which the trip began in
        const val ID_START_CITY_LOCATION = "startCityLocation"
        //City which the trip ended in
        const val ID_END_CITY_LOCATION = "endCityLocation"
        //The length of the trip in km, should be a double
        const val ID_MILEAGE_TRIP = "mileage_trip"
        //The odometer being read from the OBD2 device, this value does not need to match the vehicle odometer
        const val ID_MILEAGE_OBD215B = "deviceOdometer"
        //Unix time representing NOT the start of the trip but after which locations are considered to be a part of this trip
        const val ID_START_TIMESTAMP = "start_timestamp"
        //Unix time representing NOT the end of the trip but before which locations are considered to be a part of this trip
        const val ID_END_TIMESTAMP = "end_timestamp"
        //Unix time representing the start of the trip
        const val ID_DRIVE_START ="drive_start"
        //Unix time representing the end of the trip
        const val ID_DRIVE_END ="drive_end"
        //Starting latitude of the trip
        const val ID_START_LATITUDE = "start_latitude"
        //Ending latitude of the trip
        const val ID_END_LATITUDE = "end_latitude"
        //Starting longitude of the trip
        const val ID_START_LONGTITUDE = "start_longitude"
        //Ending longitude of the trip
        const val ID_END_LONGITUDE = "end_longitude"
        //The type of device from which the data was streamed
        const val ID_DEVICE_TYPE = "deviceType"
        //The id of the bluetooth device associated with the vehicle that the data is streaming from
        const val ID_DEVICE_ID = "deviceId"
        //The rtc time on the bluetooth device at the time the data was streamed
        const val ID_RTC = "bluetoothDeviceTime"

    }
}