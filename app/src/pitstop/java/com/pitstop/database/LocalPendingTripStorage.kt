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
                + TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID +" INTEGER PRIMARY KEY,"
                + TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID + " INTEGER,"
                + TABLES.PENDING_TRIP_DATA.KEY_ID+ " TEXT,"
                + TABLES.PENDING_TRIP_DATA.KEY_DATA+ " TEXT,"+ ")")
    }

    fun store(trip: TripData): Long {
        Log.d(TAG,"store()")
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

    fun get(): List<TripData> {
        Log.d(TAG,"get()")
        val trips = mutableListOf<TripData>()
        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.PENDING_TRIP_DATA.TABLE_NAME, null, null
                , null, null, null, TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID)

        if (c.moveToFirst()) {
            Log.d(TAG,"c.moveToFirst()")
            var curTripId = -1
            var curLocationId = -1

            var curTrip = arrayListOf<LocationData>()
            var curLocation = arrayListOf<DataPoint>()
            while (!c.isAfterLast) {
                val tripId = c.getInt(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_TRIP_ID))
                val locationId = c.getInt(c.getColumnIndex(TABLES.PENDING_TRIP_DATA.KEY_LOCATION_ID))
                Log.d(TAG,"got tripId: $tripId")
                if (curTripId != tripId && curTripId != -1){
                    //New trip
                    Log.d(TAG,"new trip")
                    curTrip.add(LocationData(locationId,curLocation))
                    curLocation = arrayListOf()
                    trips.add(TripData(curTripId,curTrip))
                    curTrip = arrayListOf()
                }else if (curLocationId != locationId && locationId != -1){
                    //New location
                    Log.d(TAG, "new location")
                    curTrip.add(LocationData(locationId,curLocation))
                    curLocation = arrayListOf()
                }
                curLocation.add(cursorToDataPoint(c))
                curTripId = tripId
                curLocationId = locationId
                c.moveToNext()
            }
            curTrip.add(curTripId, LocationData(curLocationId,curLocation))
            trips.add(TripData(curTripId,curTrip))
            Log.d(TAG,"exiting afterlast loop")
        }
        c.close()
        return trips
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