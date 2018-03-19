package com.pitstop.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.location.Location
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pitstop.models.Trip
import com.pitstop.models.TripLocation
import java.util.*

/**
 * Created by Karol Zdebel on 3/2/2018.
 */
class LocalTripStorage(context: Context) {

    private val TAG = javaClass.simpleName
    private var databaseHelper: LocalDatabaseHelper = LocalDatabaseHelper.getInstance(context)
    private val gson = Gson()

    companion object {
        val CREATE_TABLE_TRIPS = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.TRIP.TABLE_NAME + "("
                + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
                + TABLES.TRIP.KEY_TRIP_ID + " INTEGER,"
                + TABLES.TRIP.KEY_TIME + " INTEGER,"
                + TABLES.TRIP.KEY_LONGITUDE + " REAL,"
                + TABLES.TRIP.KEY_LATITUDE + " REAL,"
                + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER,"
                + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")")
    }

    fun store(locations: List<Location>): Long {
        val db = databaseHelper?.writableDatabase
        var rows = 0L
        locations.forEach({
            rows += db.insert(TABLES.TRIP.TABLE_NAME, null
                    , objectToContentValues(locations[0].time,it))
        })

        return rows
    }

    fun getAllTrips(): List<List<Location>> {
        Log.d(TAG,"getAllTrips()")
        val trips = ArrayList<List<Location>>()
        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.TRIP.TABLE_NAME, null, null, null, null, null, null)

        if (c.moveToFirst()) {
            Log.d(TAG,"c.moveToFirst()")
            var curTripId = -1L
            var curTrip = arrayListOf<Location>()
            while (!c.isAfterLast) {
                val tripId = c.getLong(c.getColumnIndex(TABLES.TRIP.KEY_TRIP_ID))
                Log.d(TAG,"got tripId: $tripId")
                if (curTripId != tripId && curTripId != -1L){
                    //New trip
                    Log.d(TAG,"new trip")
                    trips.add(curTrip)
                    curTrip = arrayListOf()
                }
                curTrip.add(cursorToLocation(c))
                c.moveToNext()
                curTripId = tripId
            }
            trips.add(curTrip)
            Log.d(TAG,"exiting afterlast loop")
        }
        c.close()
        return trips
    }

    fun getTrip(parseId: String): Trip? {

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.MANUAL_TRIP.TABLE_NAME, null,
                TABLES.COMMON.KEY_OBJECT_ID + "=?", arrayOf(parseId), null, null, null)
        var trip: Trip? = null
        if (c.moveToFirst()) {
            trip = cursorToTripWithPath(c)
        }
        c.close()
        return trip
    }

    fun deleteAllTrips() {
        val db = databaseHelper.writableDatabase

        db.delete(TABLES.MANUAL_TRIP.TABLE_NAME, null, null)
    }

    private fun cursorToLocation(c: Cursor): Location {
        val longitude = c.getDouble(c.getColumnIndex(TABLES.TRIP.KEY_LONGITUDE))
        val latitude = c.getDouble(c.getColumnIndex(TABLES.TRIP.KEY_LATITUDE))
        val time = c.getLong(c.getColumnIndex(TABLES.TRIP.KEY_TIME))

        val location = Location("")
        location.longitude = longitude
        location.latitude = latitude
        location.time = time

        return location
    }

    private fun cursorToTripWithPath(c: Cursor): Trip {
        val trip = Trip()
        val locListType = object : TypeToken<List<TripLocation>>() {

        }.type
        trip.setTripId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)))
        trip.start = gson.fromJson(c.getString(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_START)), TripLocation::class.java)
        trip.end = gson.fromJson(c.getString(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_END)), TripLocation::class.java)
        trip.startAddress = c.getString(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_START_ADDRESS))
        trip.endAddress = c.getString(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_END_ADDRESS))
        trip.totalDistance = c.getDouble(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_TOTAL_DISTANCE))
        trip.path = gson.fromJson<Any>(c.getString(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_PATH)), locListType) as List<TripLocation>
        return trip
    }

    private fun objectToContentValues(tripId: Long, location: Location): ContentValues {
        val values = ContentValues()
        values.put(TABLES.TRIP.KEY_TRIP_ID, tripId)
        values.put(TABLES.TRIP.KEY_LONGITUDE, location.longitude)
        values.put(TABLES.TRIP.KEY_LATITUDE, location.latitude)
        values.put(TABLES.TRIP.KEY_TIME, location.time)
        return values
    }

    fun removeAll() {
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.MANUAL_TRIP.TABLE_NAME, null, null)
    }

    fun deleteTrip(tripId: Int) {
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.TRIP.TABLE_NAME, TABLES.TRIP.KEY_TRIP_ID+ "=?",
                arrayOf(tripId.toString()))

    }
}