package com.pitstop.models.trip

import android.location.Location

/**
 * Created by Karol Zdebel on 3/19/2018.
 */
data class LocationData(val id: Long, val data: Location)

data class LocationDataFormatted(val id: Long, val data: Set<DataPoint>)