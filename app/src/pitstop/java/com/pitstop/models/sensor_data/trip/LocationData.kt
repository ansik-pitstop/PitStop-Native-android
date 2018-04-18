package com.pitstop.models.sensor_data.trip

import com.pitstop.models.sensor_data.DataPoint

/**
 * Created by Karol Zdebel on 3/19/2018.
 */
data class LocationData(val id: Long, val data: PendingLocation)

data class LocationDataFormatted(val id: Long, val data: Set<DataPoint>)