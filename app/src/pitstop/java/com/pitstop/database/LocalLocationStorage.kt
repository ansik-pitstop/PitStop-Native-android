package com.pitstop.database

import android.content.ContentValues
import android.util.Log
import com.pitstop.models.trip.CarLocation

/**
 *
 * Created by Karol Zdebel on 6/4/2018.
 */
class LocalLocationStorage(private val databaseHelper: LocalDatabaseHelper) {

    private val TAG = javaClass.simpleName

    companion object {
        val CREATE_LOCATION_DATA_TABLE = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.LOCATION_DATA.TABLE_NAME + "("
                + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TABLES.LOCATION_DATA.KEY_TIME+" TIMESTAMP,"
                + TABLES.LOCATION_DATA.KEY_LONGITUDE+ " REAL,"
                + TABLES.LOCATION_DATA.KEY_LATITUDE+ " REAL,"
                + TABLES.LOCATION_DATA.KEY_CONFIDENCE+ " REAL,"
                + TABLES.LOCATION_DATA.KEY_VIN+ " TEXT" + ")")
    }

    fun store(locations: List<CarLocation>): Int{
        Log.d(TAG,"store() location size = ${locations.size}")
        val db = databaseHelper.writableDatabase
        var rows = 0

        locations.forEach({
            val contentValues = ContentValues()
            contentValues.put(TABLES.LOCATION_DATA.KEY_TIME, it.time)
            contentValues.put(TABLES.LOCATION_DATA.KEY_LATITUDE, it.latitude)
            contentValues.put(TABLES.LOCATION_DATA.KEY_LONGITUDE, it.longitude)
            contentValues.put(TABLES.LOCATION_DATA.KEY_VIN, it.vin)
            if (db.insert(TABLES.LOCATION_DATA.TABLE_NAME,null,contentValues) > 0 ) rows = rows.inc()
        })

        return rows
    }

    fun getAll(): List<CarLocation>{
        Log.d(TAG,"getAll()")

        val db = databaseHelper.writableDatabase
        val c = db.query(TABLES.LOCATION_DATA.TABLE_NAME,null,null,null
                ,null,null,TABLES.LOCATION_DATA.KEY_TIME+" ASC")
        val result = arrayListOf<CarLocation>()
        if (c.moveToFirst()){
            while (!c.isAfterLast){
                val time = c.getLong(c.getColumnIndex(TABLES.LOCATION_DATA.KEY_TIME))
                val lat = c.getDouble(c.getColumnIndex(TABLES.LOCATION_DATA.KEY_LATITUDE))
                val long = c.getDouble(c.getColumnIndex(TABLES.LOCATION_DATA.KEY_LONGITUDE))
                val vin = c.getString(c.getColumnIndex(TABLES.LOCATION_DATA.KEY_VIN))
                result.add(CarLocation(vin,time,long,lat))
                c.moveToNext()
            }
        }
        c.close()
        return result
    }

    fun remove(location: CarLocation): Int{
        Log.d(TAG,"remove()")
        val db = databaseHelper.writableDatabase
        return db.delete(TABLES.LOCATION_DATA.TABLE_NAME
                , TABLES.LOCATION_DATA.KEY_TIME+" = ?"
                , arrayOf(location.time.toString()))
    }

    fun remove(locations: List<CarLocation>): Int{
        Log.d(TAG,"remove()")
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        var rows = 0
        try{
            locations.forEach{
                rows += db.delete(TABLES.LOCATION_DATA.TABLE_NAME
                        ,TABLES.LOCATION_DATA.KEY_TIME+" = ?"
                        , arrayOf(it.time.toString()))
            }
            db.setTransactionSuccessful()
        }finally {
            db.endTransaction()
        }

        return rows
    }

    fun removeAll(): Int{
        Log.d(TAG,"removeAll()")
        val db = databaseHelper.writableDatabase
        return db.delete(TABLES.LOCATION_DATA.TABLE_NAME
                , null,null)
    }
}