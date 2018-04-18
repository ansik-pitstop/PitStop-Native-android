package com.pitstop.utils

import com.pitstop.bluetooth.bleDevice.Device212B
import com.pitstop.bluetooth.bleDevice.Device215B
import com.pitstop.bluetooth.bleDevice.ELM327Device
import com.pitstop.bluetooth.dataPackages.ELM327PidPackage
import com.pitstop.bluetooth.dataPackages.OBD212PidPackage
import com.pitstop.bluetooth.dataPackages.OBD215PidPackage
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.models.sensor_data.DataPoint

/**
 * Created by Karol Zdebel on 4/18/2018.
 */
class SensorDataUtils {
    companion object {
        fun pidToSensorDataFormat(pid: PidPackage, vin: String): Set<DataPoint>{
            val dataPointList = mutableSetOf<DataPoint>()
            dataPointList.add(DataPoint(DataPoint.ID_VIN,vin))
            dataPointList.add(DataPoint(DataPoint.ID_DEVICE_ID,pid.deviceId))
            pid.pids.forEach({
                dataPointList.add(DataPoint(it.key,it.value))
            })
            if (pid is OBD215PidPackage){
                dataPointList.add(DataPoint(DataPoint.ID_DEVICE_TYPE,Device215B.NAME))

            }else if (pid is ELM327PidPackage){
                dataPointList.add(DataPoint(DataPoint.ID_DEVICE_TYPE,ELM327Device.NAME))
            }else if (pid is OBD212PidPackage){
                dataPointList.add(DataPoint(DataPoint.ID_DEVICE_TYPE,Device212B.NAME))
            }else{
                throw IllegalArgumentException()
            }
            return dataPointList
        }

        fun pidListToSensorDataFormat(pids: List<PidPackage>, vin: String): List<Set<DataPoint>>{
            val retData = arrayListOf<Set<DataPoint>>()
            pids.forEach({
                retData.add(pidToSensorDataFormat(it,vin))
            })
            return retData
        }
    }
}