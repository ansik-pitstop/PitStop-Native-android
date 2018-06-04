package com.pitstop.database

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.pitstop.models.trip.CarActivity

/**
 * Created by Karol Zdebel on 6/4/2018.
 */
class LocalActivityStorage(context: Context) {

    private val TAG = javaClass.simpleName
    private var databaseHelper: LocalDatabaseHelper = LocalDatabaseHelper.getInstance(context)

    companion object {
        val CREATE_ACTIVITY_DATA_TABLE = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.ACTIVITY_DATA.TABLE_NAME + "("
                + TABLES.ACTIVITY_DATA.KEY_TIME+" TIMESTAMP,"
                + TABLES.ACTIVITY_DATA.KEY_TYPE+ " INTEGER,"
                + TABLES.ACTIVITY_DATA.KEY_CONFIDENCE+ " INTEGER,"
                + TABLES.ACTIVITY_DATA.KEY_VIN+ " TEXT," + ")")
    }

    fun store(activityList: List<CarActivity>): Int{
        Log.d(TAG,"store() activities size = ${activityList.size}")
        val db = databaseHelper.writableDatabase
        var rows = 0

        activityList.forEach({
            val contentValues = ContentValues()
            contentValues.put(TABLES.ACTIVITY_DATA.KEY_TIME, it.time)
            contentValues.put(TABLES.ACTIVITY_DATA.KEY_TYPE, it.type)
            contentValues.put(TABLES.ACTIVITY_DATA.KEY_CONFIDENCE, it.conf)
            contentValues.put(TABLES.ACTIVITY_DATA.KEY_VIN, it.vin)
            if (db.insert(TABLES.PENDING_TRIP_DATA.TABLE_NAME,null,contentValues) > 0 )
                rows = rows.inc()
        })

        return rows
    }

    fun getAll(): List<CarActivity>{
        Log.d(TAG,"getAll()")

        val db = databaseHelper.writableDatabase
        val c = db.query(TABLES.ACTIVITY_DATA.TABLE_NAME,null,null,null
                ,null,null,null)
        val result = arrayListOf<CarActivity>()
        if (c.moveToFirst()){
            while (!c.isAfterLast){
                val time = c.getLong(c.getColumnIndex(TABLES.ACTIVITY_DATA.KEY_TIME))
                val type = c.getInt(c.getColumnIndex(TABLES.ACTIVITY_DATA.KEY_TYPE))
                val conf = c.getInt(c.getColumnIndex(TABLES.ACTIVITY_DATA.KEY_CONFIDENCE))
                val vin = c.getString(c.getColumnIndex(TABLES.ACTIVITY_DATA.KEY_VIN))
                result.add(CarActivity(vin,time,type,conf))
                c.moveToNext()
            }
        }
        return result
    }

    fun remove(carActivity: CarActivity): Int{
        Log.d(TAG,"remove()")
        val db = databaseHelper.writableDatabase
        return db.delete(TABLES.ACTIVITY_DATA.TABLE_NAME
                , TABLES.ACTIVITY_DATA.KEY_TIME+" = ?"
                , arrayOf(carActivity.time.toString()))
    }

    fun remove(carActivity: List<CarActivity>): Int{
        Log.d(TAG,"remove()")
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        var rows = 0
        try{
            carActivity.forEach{
                rows += db.delete(TABLES.ACTIVITY_DATA.TABLE_NAME
                        ,TABLES.ACTIVITY_DATA.KEY_TIME+" = ?"
                        , arrayOf(it.time.toString()))
            }
            db.setTransactionSuccessful()
        }finally{
            db.endTransaction()
        }

        return rows
    }

    fun removeAll(): Int{
        Log.d(TAG,"removeAll()")
        val db = databaseHelper.writableDatabase
        return db.delete(TABLES.ACTIVITY_DATA.TABLE_NAME
                , null,null)
    }
}