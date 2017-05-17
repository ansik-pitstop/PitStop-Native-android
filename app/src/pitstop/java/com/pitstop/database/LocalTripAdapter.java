package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pitstop.models.Appointment;
import com.pitstop.models.Trip;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matthew on 2017-05-16.
 */

public class LocalTripAdapter {
    // APPOINTMENT table create statement
    public static final String CREATE_TABLE_APPOINTMENT = "CREATE TABLE IF NOT EXISTS "
            + TABLES.TRIP.TABLE_NAME + "("
            + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.TRIP.KEY_START + " TEXT, "
            + TABLES.TRIP.KEY_END + " TEXT, "
            + TABLES.TRIP.KEY_START_ADDRESS + " TEXT, "
            + TABLES.TRIP.KEY_END_ADDRESS + " TEXT, "
            + TABLES.TRIP.KEY_TOTAL_DISTANCE + " DOUBLE, "
            + TABLES.TRIP.KEY_PATH + " TEXT, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalTripAdapter(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    /**
     * Store appointment data
     */
    public void storeTripData(Trip trip) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = tripObjectToContentValues(trip);

        long result = db.insert(TABLES.TRIP.TABLE_NAME, null, values);


        db.close();
    }

    public void storeTrips(List<Trip> tripList) {
        for(Trip trip : tripList) {
            storeTripData(trip);
        }
    }

    /**
     * Get all appointments
     */
    public List<Trip> getAllTrips() {
        List<Trip> trips = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.TRIP.TABLE_NAME, null,null,null,null,null,null);

        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                trips.add(cursorToTrip(c));
                c.moveToNext();
            }
        }
        db.close();
        return trips;
    }

    /**
     * Get appointment by parse id
     */

    public Trip getTrip(String parseId) {

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.TRIP.TABLE_NAME,null,
                TABLES.COMMON.KEY_OBJECT_ID +"=?", new String[] {parseId},null,null,null);
        Trip trip = null;
        if(c.moveToFirst()) {
            trip = cursorToTrip(c);
        }

        db.close();
        return trip;
    }




    /**
     * Update appointment
     */
    public int updateTrip(Trip trip) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = tripObjectToContentValues(trip);

        int rows = db.update(TABLES.TRIP.TABLE_NAME,values, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                new String[] { String.valueOf(trip.getId()) });

        db.close();

        return rows;
    }

    /** Delete all appointments*/
    public void deleteAllTrips() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.TRIP.TABLE_NAME, null, null);

        db.close();
    }

    private Trip cursorToTrip(Cursor c) {
        Trip trip = new Trip();
        Gson gson = new Gson();
        Type locListType = new TypeToken<List<Location>>(){}.getType();
        trip.setTripId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)));
        trip.setStart(gson.fromJson(c.getString(c.getColumnIndex(TABLES.TRIP.KEY_START)),Location.class));
        trip.setEnd(gson.fromJson(c.getString(c.getColumnIndex(TABLES.TRIP.KEY_END)),Location.class));
        trip.setStartAddress(c.getString(c.getColumnIndex(TABLES.TRIP.KEY_START_ADDRESS)));
        trip.setEndAddress(c.getString(c.getColumnIndex(TABLES.TRIP.KEY_END_ADDRESS)));
        trip.setTotalDistance(c.getDouble(c.getColumnIndex(TABLES.TRIP.KEY_TOTAL_DISTANCE)));
        trip.setTripId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)));
        trip.setPath((List<Location>)gson.fromJson(c.getString(c.getColumnIndex(TABLES.TRIP.KEY_PATH)),locListType));//probably wrong

        return trip;
    }


    private ContentValues tripObjectToContentValues(Trip trip) {
        ContentValues values = new ContentValues();

        Gson gson = new Gson();
        String jsonData;



        values.put(TABLES.COMMON.KEY_OBJECT_ID,trip.getId());
        jsonData = gson.toJson(trip.getStart());
        values.put(TABLES.TRIP.KEY_START,jsonData);
        jsonData = gson.toJson(trip.getEnd());
        values.put(TABLES.TRIP.KEY_END,jsonData);
        values.put(TABLES.TRIP.KEY_START_ADDRESS,trip.getStartAddress());
        values.put(TABLES.TRIP.KEY_END_ADDRESS,trip.getEndAddress());
        values.put(TABLES.TRIP.KEY_TOTAL_DISTANCE,trip.getTotalDistance());
        jsonData = gson.toJson(trip.getPath());
        values.put(TABLES.TRIP.KEY_PATH,jsonData);

        return values;
    }

    public void deleteAllRows(){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.TRIP.TABLE_NAME, null, null);

        db.close();
    }

    public void deleteTrip(Trip trip){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(TABLES.TRIP.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                new String[]{String.valueOf(trip.getId())});
        db.close();
    }


}
