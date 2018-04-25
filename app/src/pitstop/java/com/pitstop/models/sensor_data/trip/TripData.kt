package com.pitstop.models.sensor_data.trip

/**
 * Created by Karol Zdebel on 3/19/2018.
 */
data class TripData(val id: Long, val completed: Boolean, val vin: String, val locations: Set<LocationData>)