package com.pitstop.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.pitstop.models.trip.DataPoint
import com.pitstop.models.trip.LocationData
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
                + TABLES.PENDING_TRIP_DATA.KEY_ID+ " TEXT,"
                + TABLES.PENDING_TRIP_DATA.KEY_DATA+ " TEXT"
                + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")")
    }

    fun store(trip: TripData): Long {
        Log.d(TAG,"store() trip size = ${trip.locations.size}")
        val db = databaseHelper?.writableDatabase
        var rows = 0L

        trip.locations.forEach({locationData ->
            locationData.data.forEach {
                val contentValues = ContentValues()
                contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_ID, it.id)
                contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_DATA, it.data)
                contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID, locationData.id)
                contentValues.put(TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID, trip.id)
                rows += db.insert(TABLES.PENDING_TRIP_DATA.TABLE_NAME,null,contentValues)
            }
        })

        return rows
    }

    fun get(): Set<TripData> {
        Log.d(TAG,"get()")
        val trips = mutableSetOf<TripData>()
        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.PENDING_TRIP_DATA.TABLE_NAME, null, null
                , null, null, null
                , TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID+
                ","+TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID)

        if (c.moveToFirst()) {
            Log.d(TAG,"c.moveToFirst()")
            var curTripId = -1L
            var curLocationId = -1L

            var curTrip = mutableSetOf<LocationData>()
            var curLocation = mutableSetOf<DataPoint>()
            while (!c.isAfterLast) {
                val tripId = c.getLong(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID))
                val locationId = c.getLong(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID))
                Log.d(TAG,"got tripId: $tripId")
                Log.d(TAG,"got locationId: $locationId")
                if (curTripId != tripId && curTripId != -1L){
                    //New trip
                    Log.d(TAG,"new trip")
                    curTrip.add(LocationData(curLocationId,curLocation))
                    curLocation = mutableSetOf()
                    trips.add(TripData(curTripId,curTrip))
                    curTrip = mutableSetOf()
                }else if (curLocationId != locationId && curLocationId != -1L){
                    //New location
                    Log.d(TAG, "new location")
                    curTrip.add(LocationData(curLocationId,curLocation))
                    curLocation = mutableSetOf()
                }
                curLocation.add(cursorToDataPoint(c))
                curTripId = tripId
                curLocationId = locationId
                c.moveToNext()
            }
            curTrip.add(LocationData(curLocationId,curLocation))
            trips.add(TripData(curTripId,curTrip))
            Log.d(TAG,"exiting afterlast loop")
        }
        c.close()
        return trips
    }

    fun delete(locations: List<LocationData>): Int{
        Log.d(TAG,"delete() locations size: ${locations.size}")
        val db = databaseHelper.writableDatabase
        val idArr = Array(locations.size, {locations[it].id.toString()})
        val rows = db.delete(TABLES.PENDING_TRIP_DATA.TABLE_NAME
                , TABLES.COMMON.KEY_OBJECT_ID + "=?", idArr)
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

    private fun cursorToDataPoint(c: Cursor):DataPoint{
        val id = c.getString(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_ID))
        val data = c.getString(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_DATA))
        return DataPoint(id,data)
    }
}