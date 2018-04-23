package com.pitstop.utils

import com.pitstop.bluetooth.bleDevice.Device212B
import com.pitstop.bluetooth.bleDevice.Device215B
import com.pitstop.bluetooth.bleDevice.ELM327Device
import com.pitstop.bluetooth.dataPackages.ELM327PidPackage
import com.pitstop.bluetooth.dataPackages.OBD212PidPackage
import com.pitstop.bluetooth.dataPackages.OBD215PidPackage
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.models.sensor_data.DataPoint
import com.pitstop.models.sensor_data.SensorData

/**
 * Created by Karol Zdebel on 4/18/2018.
 */
class SensorDataUtils {
    companion object {
        fun pidToSensorData(pid: PidPackage, vin: String): SensorData{
            val deviceName: String
            val rtcTime: Long
            if (pid is OBD215PidPackage){
                deviceName = Device215B.NAME
                rtcTime = pid.rtcTime.toLong()

            }else if (pid is ELM327PidPackage){
                deviceName = ELM327Device.NAME
                rtcTime = 0
            }else if (pid is OBD212PidPackage){
                deviceName = Device212B.NAME
                rtcTime = pid.rtcTime.toLong()
            }else{
                throw IllegalArgumentException()
            }

            return SensorData(pid.deviceId,vin,rtcTime,deviceName,pid.timestamp
                    , pid.pids.map { DataPoint(it.key,it.value) }.toSet())
        }

        fun pidCollectionToSensorDataCollection(pids: Collection<PidPackage>, vin: String): Collection<SensorData>{
            val retData = mutableSetOf<SensorData>()
            pids.forEach({
                retData.add(pidToSensorData(it,vin))
            })
            return retData
        }

        fun sensorDataToDataPointList(sensorData: SensorData): Set<DataPoint>{
            val points = mutableSetOf<DataPoint>()
            points.add(DataPoint(DataPoint.ID_DEVICE_ID,sensorData.deviceId))
            points.add(DataPoint(DataPoint.ID_DEVICE_TYPE,sensorData.deviceType))
            points.add(DataPoint(DataPoint.ID_DEVICE_TIMESTAMP,sensorData.bluetoothDeviceTime.toString()))
            points.addAll(sensorData.data)
            return points
        }

        fun sensorDataListToDataPointList(sensorData: Collection<SensorData>): Set<Set<DataPoint>>{
            val points = mutableSetOf<Set<DataPoint>>()
            sensorData.forEach{
                points.add(sensorDataToDataPointList(it))
            }
            return points
        }
    }
}