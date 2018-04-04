package com.pitstop.models.trip_k

/**
 * Created by Karol Zdebel on 3/19/2018.
 */
data class TripData(val id: Long, val completed: Boolean, val vin: String, val locations: Set<LocationData>)