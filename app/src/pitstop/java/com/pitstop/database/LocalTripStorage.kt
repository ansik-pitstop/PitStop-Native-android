package com.pitstop.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.location.Location
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pitstop.models.Trip
import com.pitstop.models.TripLocation
import java.util.*

/**
 * Created by Karol Zdebel on 3/2/2018.
 */
class LocalTripStorage(context: Context) {

    private var databaseHelper: LocalDatabaseHelper = LocalDatabaseHelper.getInstance(context)
    private val gson = Gson()

    val CREATE_TABLE_APPOINTMENT = ("CREATE TABLE IF NOT EXISTS "
            + TABLES.TRIP.TABLE_NAME + "("
            + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.TRIP.KEY_TRIP_ID + "INTEGER"
            + TABLES.TRIP.KEY_TIME + " INTEGER, "
            + TABLES.TRIP.KEY_LONGITUDE + " REAL, "
            + TABLES.TRIP.KEY_LONGITUDE + " REAL, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")")


    /**
     * Store appointment data
     */
    fun store(locations: List<Location>) {
        val db = databaseHelper?.writableDatabase

        locations.forEach({
            db.insert(TABLES.TRIP.TABLE_NAME, null
                    , objectToContentValues(locations[0].time,it))
        })
    }

    /**
     * Get all appointments
     */
    fun getAllTrips(): List<List<Location>> {
        val trips = ArrayList<List<Location>>()
        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.TRIP.TABLE_NAME, null, null, null, null, null, null)

        if (c.moveToFirst()) {
            var curTripId = -1L
            var curTrip = arrayListOf<Location>()
            while (!c.isAfterLast) {
                val tripId = c.getLong(c.getColumnIndex(TABLES.TRIP.KEY_TRIP_ID))
                if (curTripId != tripId && curTripId != -1L){
                    //New trip
                    trips.add(curTrip)
                    curTrip = arrayListOf()
                }
                curTrip.add(cursorToLocation(c))
                c.moveToNext()
                curTripId = tripId
            }
        }
        c.close()
        return trips
    }

    /**
     * Get appointment by parse id
     */

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

    /** Delete all appointments */
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