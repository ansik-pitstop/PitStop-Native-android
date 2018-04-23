package com.pitstop.models.sensor_data

/**
 * Created by Karol Zdebel on 4/19/2018.
 */
data class SensorData(val deviceId: String, val vin: String, val bluetoothDeviceTime: Long
                      , val deviceType: String, val phoneTimestamp: Long, val data: Set<DataPoint>) {
}