package com.pitstop.repositories

import android.util.Log
import com.google.gson.Gson
import com.pitstop.database.LocalSensorDataStorage
import com.pitstop.models.sensor_data.SensorData
import com.pitstop.retrofit.PitstopSensorDataApi
import com.pitstop.utils.SensorDataUtils

/**
 * Created by Karol Zdebel on 4/18/2018.
 */
class SensorDataRepository(private val local: LocalSensorDataStorage
                           , private val remote: PitstopSensorDataApi): Repository {

    private val TAG = SensorDataRepository::class.simpleName
    private val CHUNK_SIZE = 10
    private val gson = Gson()

    fun store(sensorData: SensorData){
        Log.d(TAG,"store()")
        local.store(sensorData)
    }

    fun dump(){
        Log.d(TAG,"dump()")
        local.getAll().chunked(CHUNK_SIZE).forEach {
            remote.store(gson.toJsonTree(SensorDataUtils.sensorDataListToDataPointList(it)))
        }
    }

    fun storeDump(sensorData: SensorData){
        store(sensorData)
        dump()
    }

    fun deleteAll(){
        local.deleteAll()
    }

    fun delete(){

    }
}