package com.pitstop.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.pitstop.models.trip.LocationData
import com.pitstop.models.trip.LocationDataFormatted
import com.pitstop.models.trip.PendingLocation
import com.pitstop.models.trip.TripData

/**
 * Created by Karol Zdebel on 3/19/2018.
 */

class LocalPendingTripStorage(private val context: Context) {
    private val TAG = javaClass.simpleName
    private var databaseHelper: LocalDatabaseHelper = LocalDatabaseHelper.getInstance(context)

    companion object {
        val CREATE_PENDING_TRIP_TABLE = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.PENDING_TRIP_DATA.TABLE_NAME + "("
                + TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID +" LONG,"
                + TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID + " LONG,"
                + TABLES.PENDING_TRIP_DATA.KEY_LONGITUDE+ " REAL,"
                + TABLES.PENDING_TRIP_DATA.KEY_LATITUDE+ " REAL,"
                + TABLES.PENDING_TRIP_DATA.KEY_TIME+ " LONG,"
                + TABLES.PENDING_TRIP_DATA.KEY_VIN+ " TEXT,"
                + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")")
    }

    //Store trip
    fun store(trip: TripData): Long {
        Log.d(TAG,"store() trip size = ${trip.locations.size}")
        val db = databaseHelper?.writableDatabase
        var rows = 0L

        trip.locations.forEach({
            val contentValues = ContentValues()
            contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_LONGITUDE, it.data.longitude)
            contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_LATITUDE, it.data.latitude)
            contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_TIME, it.data.time)
            contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID, it.id)
            contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID, trip.id)
            contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_VIN, trip.vin)
            rows += db.insert(TABLES.PENDING_TRIP_DATA.TABLE_NAME,null,contentValues)
        })

        return rows
    }

    //Return list of trips stored in database
    fun get(): List<TripData> {
        Log.d(TAG,"get()")
        val trips = mutableListOf<TripData>()
        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.PENDING_TRIP_DATA.TABLE_NAME, null, null
                , null, null, null
                , TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID+
                ","+TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID)

        if (c.moveToFirst()) {
            Log.d(TAG,"c.moveToFirst()")
            var curTripId = -1L

            var curTrip = mutableSetOf<LocationData>()
            while (!c.isAfterLast) {
                val tripId = c.getLong(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID))
                val locationId = c.getLong(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID))
                val vin = c.getString(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_VIN))
                Log.d(TAG,"got tripId: $tripId")
                Log.d(TAG,"got locationId: $locationId")
                if (curTripId != tripId && curTripId != -1L){
                    //New trip
                    trips.add(TripData(curTripId,vin,curTrip))
                    Log.d(TAG,"new trip: $curTrip")
                    curTrip = mutableSetOf()
                }
                curTrip.add(LocationData(locationId,cursorToLocation(c)))
                curTripId = tripId
                if (c.isLast){
                    trips.add(TripData(curTripId,vin,curTrip))
                    Log.d(TAG,"exiting afterlast loop")
                }
                c.moveToNext()

            }
        }
        c.close()
        return trips
    }

    //Delete list of locations from database if they're present
    fun delete(locations: List<LocationDataFormatted>): Int{
        Log.d(TAG,"delete() locations size: ${locations.size}")
        val db = databaseHelper.writableDatabase
        var rows = 0
        db.beginTransaction()
        locations.forEach({locationData ->
            val idArr = Array(1, {locationData.id.toString()})
            Log.d(TAG,"idArr: ${idArr[0]}")
            rows += db.delete(TABLES.PENDING_TRIP_DATA.TABLE_NAME
                    , TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID + "=?", idArr)
        })
        db.setTransactionSuccessful()
        db.endTransaction()
        Log.d(TAG,"deleted $rows rows")
        return rows
    }

    //Deletes the oldest data points past 10k rows
    fun cleanDatabase(): Int{
        Log.d(TAG,"deleteOldData()")
        val db = databaseHelper.writableDatabase
        val count = db.rawQuery("DELETE t FROM ${TABLES.PENDING_TRIP_DATA.TABLE_NAME} AS t" +
                " JOIN ( SELECT ${TABLES.COMMON.KEY_CREATED_AT} AS ts" +
                " FROM ${TABLES.PENDING_TRIP_DATA.TABLE_NAME}" +
                " ORDER BY ts ASC" +
                " LIMIT 1 OFFSET 10000" +
                " ) tlimit" +
                " ON t.${TABLES.COMMON.KEY_CREATED_AT} > tlimit.ts",null).count
        Log.d(TAG,"deleted $count old data points")
        return count
    }

    fun deleteAll() {
        Log.d(TAG,"deleteAll()")
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.PENDING_TRIP_DATA.TABLE_NAME, null, null)
    }

    private fun cursorToLocation(c: Cursor):PendingLocation = PendingLocation(
                latitude = c.getDouble(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_LATITUDE))
                , longitude = c.getDouble(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_LONGITUDE))
                , time = c.getLong(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_TIME))
        )
}