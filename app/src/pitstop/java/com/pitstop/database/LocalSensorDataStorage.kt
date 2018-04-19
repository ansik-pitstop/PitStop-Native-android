package com.pitstop.database

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.pitstop.models.sensor_data.DataPoint
import com.pitstop.models.sensor_data.SensorData

/**
 * Created by Karol Zdebel on 4/19/2018.
 */
class LocalSensorDataStorage(context: Context) {

    private val TAG = LocalSensorDataStorage::class.java.simpleName
    private val databaseHelper: LocalDatabaseHelper = LocalDatabaseHelper.getInstance(context)

    // TRIP table create statement
    companion object {
        const val CREATE_TABLE_SENSOR_DATA = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.SENSOR_DATA.TABLE_NAME + "("
                + TABLES.SENSOR_DATA.DEVICE_TIMESTAMP+ " INTEGER PRIMARY KEY, "
                + TABLES.SENSOR_DATA.RTC_TIME+ " INTEGER NOT NULL, "
                + TABLES.SENSOR_DATA.DEVICE_ID+ " TEXT, "
                + TABLES.SENSOR_DATA.DEVICE_TYPE+ " TEXT, "
                + TABLES.SENSOR_DATA.VIN + " TEXT" + ")")

        const val CREATE_TABLE_SENSOR_DATA_POINT = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.SENSOR_DATA_POINT.TABLE_NAME + "("
                + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TABLES.SENSOR_DATA.RTC_TIME + " INTEGER, "
                + TABLES.SENSOR_DATA_POINT.DATA_ID +" TEXT, "
                + TABLES.SENSOR_DATA_POINT.DATA_VALUE +" TEXT, "
                + "FOREIGN KEY ("+TABLES.SENSOR_DATA.RTC_TIME+") REFERENCES "
                +TABLES.SENSOR_DATA.TABLE_NAME+"("+TABLES.SENSOR_DATA.RTC_TIME+")" +")")

    }

    fun store(sensorData: SensorData): Int{
        Log.d(TAG,"store() sensorData.data.size = ${sensorData.data.size}")

        var rows = 0
        val db = databaseHelper.writableDatabase

        db.beginTransaction()
        val sensorDataContent = ContentValues()
        sensorDataContent.put(TABLES.SENSOR_DATA.DEVICE_TIMESTAMP,sensorData.timestamp)
        sensorDataContent.put(TABLES.SENSOR_DATA.RTC_TIME,sensorData.rtcTime)
        sensorDataContent.put(TABLES.SENSOR_DATA.DEVICE_TYPE,sensorData.deviceType)
        sensorDataContent.put(TABLES.SENSOR_DATA.DEVICE_ID,sensorData.deviceId)
        sensorDataContent.put(TABLES.SENSOR_DATA.VIN,sensorData.vin)
        if (db.insert(TABLES.SENSOR_DATA.TABLE_NAME
                ,null,sensorDataContent) > 0) rows = rows.inc()

        sensorData.data.forEach {
            val sensorDataPointContent = ContentValues()
            sensorDataPointContent.put(TABLES.SENSOR_DATA_POINT.DATA_ID,it.id)
            sensorDataPointContent.put(TABLES.SENSOR_DATA_POINT.DATA_VALUE,it.data)
            sensorDataPointContent.put(TABLES.SENSOR_DATA.RTC_TIME, sensorData.rtcTime)
            if (db.insert(TABLES.SENSOR_DATA_POINT.TABLE_NAME,null,sensorDataPointContent) > 0)
                rows = rows.inc()
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        Log.d(TAG,"stored $rows rows")
        return rows
    }

    fun getAll(): Collection<SensorData>{
        Log.d(TAG,"getAll()")
        val sensorDataSet = mutableSetOf<SensorData>()
        val db = databaseHelper.readableDatabase

        db.beginTransaction()
        val cursor = db.query(TABLES.SENSOR_DATA.TABLE_NAME,null,null
                ,null,null, null, null)
        Log.d(TAG,"Got ${cursor.count} rows from sensor data query")
        if (cursor.moveToFirst()){
            while (!cursor.isAfterLast){
                val rtcTime = cursor.getLong(cursor.getColumnIndex(TABLES.SENSOR_DATA.RTC_TIME))
                val deviceId = cursor.getString(cursor.getColumnIndex(TABLES.SENSOR_DATA.DEVICE_ID))
                val vin = cursor.getString(cursor.getColumnIndex(TABLES.SENSOR_DATA.VIN))
                val deviceType = cursor.getString(cursor.getColumnIndex(TABLES.SENSOR_DATA.DEVICE_TYPE))
                val deviceTimestamp = cursor.getLong(cursor.getColumnIndex(TABLES.SENSOR_DATA.DEVICE_TIMESTAMP))

                Log.d(TAG,"Got sensor data row rtcTime: $rtcTime vin: $vin")

                val dataPointCursor = db.query(TABLES.SENSOR_DATA_POINT.TABLE_NAME
                        ,null,TABLES.SENSOR_DATA.RTC_TIME + "=?"
                        , arrayOf(rtcTime.toString()),null,null,TABLES.SENSOR_DATA.RTC_TIME)

                Log.d(TAG,"Got ${dataPointCursor.count} data points for rtcTime: $rtcTime")
                val dataPoints = mutableSetOf<DataPoint>()
                if (dataPointCursor.moveToFirst()){
                    while (!dataPointCursor.isAfterLast){
                        val dataId = dataPointCursor.getString(dataPointCursor
                                .getColumnIndex(TABLES.SENSOR_DATA_POINT.DATA_ID))
                        val dataValue = dataPointCursor.getString(dataPointCursor
                                .getColumnIndex(TABLES.SENSOR_DATA_POINT.DATA_VALUE))
                        Log.d(TAG,"got data value: $dataValue")
                        dataPoints.add(DataPoint(dataId,dataValue))
                        dataPointCursor.moveToNext()
                    }
                }
                sensorDataSet.add(SensorData(deviceId,vin,rtcTime,deviceType,deviceTimestamp,dataPoints))
                dataPointCursor.close()
                cursor.moveToNext()
            }
        }
        cursor.close()
        db.setTransactionSuccessful()
        db.endTransaction()

        return sensorDataSet

    }

    fun deleteAll(){
        Log.d(TAG,"deleteAll()")
        databaseHelper.writableDatabase.delete(TABLES.SENSOR_DATA.TABLE_NAME
                ,null,null)
        databaseHelper.writableDatabase.delete(TABLES.SENSOR_DATA_POINT.TABLE_NAME
                ,null,null)
    }
}