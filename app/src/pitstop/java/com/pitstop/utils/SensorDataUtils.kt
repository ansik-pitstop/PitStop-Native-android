package com.pitstop.utils

import com.pitstop.bluetooth.bleDevice.Device215B
import com.pitstop.bluetooth.bleDevice.ELM327Device
import com.pitstop.bluetooth.dataPackages.ELM327PidPackage
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
                rtcTime = try{
                    pid.rtcTime.toLong()
                }catch(e: Exception){
                    0L
                }

            }else if (pid is ELM327PidPackage){
                deviceName = ELM327Device.NAME
                rtcTime = 0
            }else{
                throw IllegalArgumentException()
            }

            //First add pids
            val dataSet = pid.pids.map { DataPoint(it.key,it.value) }.toMutableSet()

            //Add mileage if its a 215B device
            if (pid is OBD215PidPackage){
                dataSet.add(DataPoint(DataPoint.ID_MILEAGE_OBD215B,pid.mileage))
            }

            return SensorData(pid.deviceId,vin,rtcTime,deviceName,pid.timestamp, dataSet)
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
            if (!sensorData.deviceId.isNullOrEmpty())
                points.add(DataPoint(DataPoint.ID_DEVICE_ID,sensorData.deviceId!!))
            points.add(DataPoint(DataPoint.ID_DEVICE_TYPE,sensorData.deviceType))
            points.add(DataPoint(DataPoint.ID_DEVICE_TIMESTAMP,sensorData.bluetoothDeviceTime.toString()))
            if (!sensorData.vin.isNullOrEmpty())
                points.add(DataPoint(DataPoint.ID_VIN,sensorData.vin!!))
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