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
    fun storeTripData(locations: List<Location>) {
        val db = databaseHelper?.writableDatabase

       // val values = tripObjectToContentValues(trip)

        val result = db.insert(TABLES.MANUAL_TRIP.TABLE_NAME, null, null)


    }

    fun storeTrips(tripList: List<Trip>) {
        for (trip in tripList) {
            //storeTripData(trip)
        }
    }

    /**
     * Get all appointments
     */
    fun getAllTrips(): List<Trip> {
        val trips = ArrayList<Trip>()
        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.MANUAL_TRIP.TABLE_NAME, null, null, null, null, null, null)

        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                trips.add(cursorToTrip(c))
                c.moveToNext()
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


    private fun cursorToTrip(c: Cursor): Trip {
        val trip = Trip()
        trip.start = gson.fromJson(c.getString(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_START)), TripLocation::class.java)
        trip.end = gson.fromJson(c.getString(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_END)), TripLocation::class.java)
        trip.startAddress = c.getString(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_START_ADDRESS))
        trip.endAddress = c.getString(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_END_ADDRESS))
        trip.totalDistance = c.getDouble(c.getColumnIndex(TABLES.MANUAL_TRIP.KEY_TOTAL_DISTANCE))
        trip.setTripId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)))
        return trip
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


    private fun objectToContentValues(locations: List<Location>): ContentValues {
        val values = ContentValues()

        val gson = Gson()
        var jsonData: String
//
//        values.put(TABLES.COMMON.KEY_OBJECT_ID, trip.id)
//        jsonData = gson.toJson(trip.start)
//        values.put(TABLES.MANUAL_TRIP.KEY_START, jsonData)
//        jsonData = gson.toJson(trip.end)
//        values.put(TABLES.MANUAL_TRIP.KEY_END, jsonData)
//        values.put(TABLES.MANUAL_TRIP.KEY_START_ADDRESS, trip.startAddress)
//        values.put(TABLES.MANUAL_TRIP.KEY_END_ADDRESS, trip.endAddress)
//        values.put(TABLES.MANUAL_TRIP.KEY_TOTAL_DISTANCE, trip.totalDistance)
//        jsonData = gson.toJson(trip.path)
//        values.put(TABLES.MANUAL_TRIP.KEY_PATH, jsonData)

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