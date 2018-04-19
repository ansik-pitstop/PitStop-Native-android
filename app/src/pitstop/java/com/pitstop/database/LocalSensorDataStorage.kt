package com.pitstop.database

import com.pitstop.models.sensor_data.SensorData
import android.content.Context
import android.util.Log
import com.pitstop.models.sensor_data.DataPoint

/**
 * Created by Karol Zdebel on 4/19/2018.
 */
class LocalSensorDataStorage(context: Context) {

    private val TAG = LocalSensorDataStorage::class.simpleName
    private val databaseHelper: LocalDatabaseHelper = LocalDatabaseHelper.getInstance(context)

    // TRIP table create statement
    val CREATE_TABLE_SENSOR_DATA = ("CREATE TABLE IF NOT EXISTS "
            + TABLES.SENSOR_DATA.TABLE_NAME + "("
            + TABLES.SENSOR_DATA.DEVICE_TIMESTAMP+ " INTEGER PRIMARY KEY, "
            + TABLES.SENSOR_DATA.RTC_TIME+ " INTEGER NOT NULL, "
            + TABLES.SENSOR_DATA.DEVICE_ID+ " TEXT, "
            + TABLES.SENSOR_DATA.DEVICE_TYPE+ " TEXT, "
            + TABLES.SENSOR_DATA.VIN + " TEXT" + ")")

    val CREATE_TABLE_SENSOR_DATA_POINT = ("CREATE TALBE IF NOT EXISTS "
            + TABLES.SENSOR_DATA_POINT.TABLE_NAME + "("
            + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TABLES.SENSOR_DATA.RTC_TIME + " INTEGER, "
            + TABLES.SENSOR_DATA_POINT.DATA_ID +" TEXT, "
            + TABLES.SENSOR_DATA_POINT.DATA_VALUE +" TEXT, "
            + "FOREIGN KEY ("+TABLES.SENSOR_DATA_POINT.TABLE_NAME+") REFERENCES "
            +TABLES.SENSOR_DATA.TABLE_NAME+"("+TABLES.SENSOR_DATA.RTC_TIME+")" +")")

            )

    fun store(sensorData: SensorData){
        Log.d(TAG,"store() sensorData.data.size = ${sensorData.data.size}")
    }

    fun getAll(): Collection<SensorData>{
        Log.d(TAG,"getAll()")
        val query = String.format("SELECT * FROM %s INNER JOIN %s ON %s.%s = %s.%s GROUP BY %s ORDER BY %s"
                ,TABLES.SENSOR_DATA.TABLE_NAME,TABLES.SENSOR_DATA_POINT.TABLE_NAME
                , TABLES.SENSOR_DATA.TABLE_NAME, TABLES.SENSOR_DATA.RTC_TIME
                , TABLES.SENSOR_DATA_POINT.TABLE_NAME, TABLES.SENSOR_DATA.RTC_TIME
                , TABLES.SENSOR_DATA.RTC_TIME, TABLES.SENSOR_DATA.RTC_TIME)
        val sensorDataSet = mutableSetOf<SensorData>()
        val cursor = databaseHelper.readableDatabase.query(TABLES.SENSOR_DATA.TABLE_NAME,null,null
                ,null,TABLES.SENSOR_DATA.VIN, null, TABLES.SENSOR_DATA.RTC_TIME)

        if (cursor.moveToFirst()){
            while (!cursor.isAfterLast){
                val rtcTime = cursor.getLong(cursor.getColumnIndex(TABLES.SENSOR_DATA.RTC_TIME))
                val deviceId = cursor.getString(cursor.getColumnIndex(TABLES.SENSOR_DATA.DEVICE_ID))
                val vin = cursor.getString(cursor.getColumnIndex(TABLES.SENSOR_DATA.VIN))
                val deviceType = cursor.getString(cursor.getColumnIndex(TABLES.SENSOR_DATA.DEVICE_TYPE))
                val deviceTimestamp = cursor.getLong(cursor.getColumnIndex(TABLES.SENSOR_DATA.DEVICE_TIMESTAMP))

                val dataPointCursor = databaseHelper.readableDatabase.query(TABLES.SENSOR_DATA_POINT.TABLE_NAME
                    ,null,TABLES.SENSOR_DATA.RTC_TIME+"=?", arrayOf(rtcTime.toString())
                ,TABLES.SENSOR_DATA.RTC_TIME,null,null)

                val dataPoints = mutableSetOf<DataPoint>()
                if (dataPointCursor.moveToFirst()){
                    while (!dataPointCursor.isAfterLast){
                        val dataId = dataPointCursor.getString(cursor.getColumnIndex(TABLES.SENSOR_DATA_POINT.DATA_ID))
                        val dataValue = cursor.getString(cursor.getColumnIndex(TABLES.SENSOR_DATA_POINT.DATA_VALUE))
                        dataPoints.add(DataPoint(dataId,dataValue))
                    }
                }
                sensorDataSet.add(SensorData(deviceId,vin,rtcTime,deviceType,deviceTimestamp,dataPoints))
                dataPointCursor.close()
            }
        }

        cursor.close()

    }

    fun deleteAll(){
        Log.d(TAG,"deleteAll()")
        databaseHelper.writableDatabase.delete(TABLES.SENSOR_DATA.TABLE_NAME
                ,null,null)
    }
}