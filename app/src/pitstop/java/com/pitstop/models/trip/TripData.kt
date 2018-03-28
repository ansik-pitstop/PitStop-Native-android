package com.pitstop.models.trip

/**
 * Created by Karol Zdebel on 3/19/2018.
 */
data class TripData(val id: Long, val vin: String, val deviceTimestamp: Long, val locations: Set<LocationData>)