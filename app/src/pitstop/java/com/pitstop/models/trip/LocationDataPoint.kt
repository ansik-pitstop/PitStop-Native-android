package com.pitstop.models.trip

/**
 * Created by Karol Zdebel on 3/15/2018.
 */
data class LocationDataPoint(val longitude: DataPoint, val latitude: DataPoint
                             , val tripIndicator: DataPoint, val deviceTimestamp: DataPoint? = null
                             , val tripId: DataPoint, val vin: DataPoint, val startLocation: DataPoint? = null
                             , val startStreetLocation: DataPoint? = null, val endStreetLocation: DataPoint? = null
                             , val startCityLocation: DataPoint? = null, val endLocation: DataPoint? = null
                             , val endCityLocation: DataPoint? = null, val mileage_trip: DataPoint? = null
                             , val start_longitude: DataPoint? = null, val start_latitude: DataPoint? = null
                             , val end_longitude: DataPoint? = null, val end_latitude: DataPoint? = null
                             , val start_timestamp: DataPoint? = null, val end_timestamp: DataPoint? = null)