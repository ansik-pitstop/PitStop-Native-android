package com.pitstop.utils

import com.pitstop.bluetooth.bleDevice.Device212B
import com.pitstop.bluetooth.bleDevice.Device215B
import com.pitstop.bluetooth.bleDevice.ELM327Device
import com.pitstop.bluetooth.dataPackages.ELM327PidPackage
import com.pitstop.bluetooth.dataPackages.OBD212PidPackage
import com.pitstop.bluetooth.dataPackages.OBD215PidPackage
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.models.sensor_data.DataPoint
import com.pitstop.models.sensor_data.pid.PidData

/**
 * Created by Karol Zdebel on 4/18/2018.
 */
class SensorDataUtils {
    companion object {
        fun pidToSensorDataFormat(pid: PidPackage, vin: String): PidData{
            val dataPointList = arrayListOf<DataPoint>()
            pid.pids.forEach({
                dataPointList.add(DataPoint(it.key,it.value))
            })
            if (pid is OBD215PidPackage){
                return PidData(dataPointList.toSet(),pid.deviceId,vin,Device215B.NAME)

            }else if (pid is ELM327PidPackage){
                return PidData(dataPointList.toSet(),pid.deviceId,vin,ELM327Device.NAME)
            }else if (pid is OBD212PidPackage){
                return PidData(dataPointList.toSet(),pid.deviceId,vin,Device212B.NAME)
            }else{
                throw IllegalArgumentException()
            }
        }

        fun pidListToSensorDataFormat(pids: List<PidPackage>, vin: String): Set<PidData>{
            val pidData = arrayListOf<PidData>()
            pids.forEach({
                pidData.add(pidToSensorDataFormat(it,vin))
            })
            return pidData.toSet()
        }
    }
}