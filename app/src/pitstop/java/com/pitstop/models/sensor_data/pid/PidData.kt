package com.pitstop.models.sensor_data.pid

import com.pitstop.models.sensor_data.DataPoint

/**
 * Created by Karol Zdebel on 4/18/2018.
 */
data class PidData(val pids: Set<DataPoint>, val deviceId: String?, val vin: String?, val deviceType: String?) {
    init{
        //One of the arguments must be provided
        if (deviceId == null && vin == null) throw IllegalArgumentException()
    }
}