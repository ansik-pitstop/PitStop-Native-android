package com.pitstop.models.sensor_data

/**
 * Created by Karol Zdebel on 4/19/2018.
 */
data class SensorData(val deviceId: String, val vin: String, val rtcTime: Long
                      , val deviceType: String, val timestamp: Long, val data: Set<DataPoint>) {
}