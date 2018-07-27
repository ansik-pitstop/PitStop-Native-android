package com.pitstop.database

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.pitstop.models.sensor_data.trip.LocationData
import com.pitstop.models.sensor_data.trip.LocationDataFormatted
import com.pitstop.models.sensor_data.trip.PendingLocation
import com.pitstop.models.sensor_data.trip.TripData

/**
 * Created by Karol Zdebel on 3/19/2018.
 */

class LocalPendingTripStorage(private val databaseHelper: LocalDatabaseHelper) {
    private val TAG = javaClass.simpleName

    companion object {
        val CREATE_PENDING_TRIP_LOCATIONS_TABLE = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.PENDING_TRIP_DATA_LOCATIONS.TABLE_NAME + "("
                + TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LOCATION_ID +" LONG,"
                + TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID + " LONG,"
                + TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LONGITUDE+ " REAL,"
                + TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LATITUDE+ " REAL,"
                + TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_TIME+ " LONG,"
                + TABLES.PENDING_TRIP_DATA.KEY_VIN+ " TEXT,"
                + TABLES.COMMON.KEY_CREATED_AT + " DATETIME,"
                + " FOREIGN KEY ("+TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID+") REFERENCES "
                +TABLES.PENDING_TRIP_DATA.TABLE_NAME+"("+TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID+")"
                + ")")

        val CREATE_PENDING_TRIP_DATA_TABLE = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.PENDING_TRIP_DATA.TABLE_NAME + "("
                + TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID + " LONG PRIMARY KEY,"
                + TABLES.PENDING_TRIP_DATA.KEY_START_TIMESTAMP + " INTEGER,"
                + TABLES.PENDING_TRIP_DATA.KEY_END_TIMESTAMP + " INTEGER,"
                + TABLES.PENDING_TRIP_DATA.KEY_VIN +" VIN"
                + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")")
    }

    //Store trip
    fun store(trip: TripData): Long {
        Log.d(TAG,"store() trip size = ${trip.locations.size}")
        val db = databaseHelper.writableDatabase
        var rows = 0L

        db.beginTransaction()

        try{
            val tripContentValues = ContentValues()
            tripContentValues.put(TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID,trip.id)
            tripContentValues.put(TABLES.PENDING_TRIP_DATA.KEY_START_TIMESTAMP, trip.startTimestamp)
            tripContentValues.put(TABLES.PENDING_TRIP_DATA.KEY_END_TIMESTAMP, trip.endTimestamp)
            tripContentValues.put(TABLES.PENDING_TRIP_DATA.KEY_VIN, trip.vin)
            db.insert(TABLES.PENDING_TRIP_DATA.TABLE_NAME,null,tripContentValues)

            trip.locations.forEach({
                val contentValues = ContentValues()
                contentValues.put(TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LONGITUDE, it.data.longitude)
                contentValues.put(TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LATITUDE, it.data.latitude)
                contentValues.put(TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_TIME, it.data.time)
                contentValues.put(TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LOCATION_ID, it.id)
                contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID, trip.id)
                contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_VIN, trip.vin)
                rows += db.insert(TABLES.PENDING_TRIP_DATA_LOCATIONS.TABLE_NAME,null,contentValues)
            })
            db.setTransactionSuccessful()
        }finally {
            db.endTransaction()
        }

        return rows
    }

    //Delete list of locations from database if they're present
    fun remove(locations: List<LocationDataFormatted>): Int{
        Log.d(TAG,"remove() locations size: ${locations.size}")
        val db = databaseHelper.writableDatabase
        var rows = 0
        db.beginTransaction()

        try{
            locations.forEach({locationData ->
                rows += db.delete(TABLES.PENDING_TRIP_DATA_LOCATIONS.TABLE_NAME
                        , TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LOCATION_ID + "=${locationData.id}", null)
            })

            db.setTransactionSuccessful()
        }finally{
            db.endTransaction()
        }

        Log.d(TAG,"deleted $rows rows")
        return rows
    }

    fun deleteAll() {
        Log.d(TAG,"deleteAll()")
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.PENDING_TRIP_DATA_LOCATIONS.TABLE_NAME, null, null)
        db.delete(TABLES.PENDING_TRIP_DATA.TABLE_NAME,null,null)
    }

    //Return list of trips stored in database that are completed
    //If sent is false all trips are returned whether or not they have been sent to server
    fun getAll(): List<TripData> {
        Log.d(TAG,"get()")
        val trips = mutableListOf<TripData>()
        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.PENDING_TRIP_DATA_LOCATIONS.TABLE_NAME
                        + " INNER JOIN "+TABLES.PENDING_TRIP_DATA.TABLE_NAME +" ON "
                        + TABLES.PENDING_TRIP_DATA.TABLE_NAME+"."+TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID
                        +" = " + TABLES.PENDING_TRIP_DATA_LOCATIONS.TABLE_NAME+"."+TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID
                , null, null, null, null, null
                , TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID+
                ","+ TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LOCATION_ID)

        Log.d(TAG,"cursor count: "+c.count)

        if (c.moveToFirst()) {
            Log.d(TAG,"c.moveToFirst()")
            var curTripId = -1L

            var curTrip = mutableListOf<LocationData>()
            while (!c.isAfterLast) {
                val startTimestamp = c.getInt(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_START_TIMESTAMP))
                val endTimestamp = c.getInt(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_END_TIMESTAMP))
                val tripId = c.getLong(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID))
                val locationId = c.getLong(c.getColumnIndex(TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LOCATION_ID))
                val vin = c.getString(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_VIN))
                Log.d(TAG,"gots tripId: $tripId")
                Log.d(TAG,"got locationId: $locationId")
                Log.d(TAG,"got startTimestamp: $startTimestamp, endTimestamp: $endTimestamp")

                if (curTripId != tripId){
                    //New trip
                    curTripId = tripId
                    curTrip = mutableListOf()
                    trips.add(TripData(curTripId, vin, curTrip,startTimestamp, endTimestamp))
                    Log.d(TAG,"new trip: $trips")
                }
                curTrip.add(LocationData(locationId, cursorToLocation(c)))

                c.moveToNext()

            }
        }
        c.close()
        return trips
    }

    private fun cursorToLocation(c: Cursor): PendingLocation = PendingLocation(
            latitude = c.getDouble(c.getColumnIndex(TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LATITUDE))
            , longitude = c.getDouble(c.getColumnIndex(TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_LONGITUDE))
            , time = c.getLong(c.getColumnIndex(TABLES.PENDING_TRIP_DATA_LOCATIONS.KEY_TIME))
    )
}