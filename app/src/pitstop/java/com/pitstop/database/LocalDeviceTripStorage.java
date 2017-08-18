package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.models.Trip215;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 8/18/2017.
 */

public class LocalDeviceTripStorage {

    private final String TAG = getClass().getSimpleName();

    public static final String CREATE_TABLE_APPOINTMENT = "CREATE TABLE IF NOT EXISTS "
            + TABLES.TRIP.TABLE_NAME + "("
            + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.TRIP_DEVICE.KEY_TRIP_ID + " INTEGER, "
            + TABLES.TRIP_DEVICE.KEY_MILEAGE + " DOUBLE, "
            + TABLES.TRIP_DEVICE.KEY_RTC + " INTEGER, "
            + TABLES.TRIP_DEVICE.KEY_DEVICE_ID + " TEXT, "
            + TABLES.TRIP_DEVICE.KEY_TRIP_ID_RAW + " INTEGER, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalDeviceTripStorage(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storeDeviceTrip(Trip215 trip215){
        Log.d(TAG,"storeDeviceTrip() tripIdRaw:"+trip215.getTripIdRaw());
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = tripObjectToContentValues(trip215);

        db.insert(TABLES.TRIP.TABLE_NAME, null, values);
    }

    public List<Trip215> getAllTrips(){
        Log.d(TAG,"getAllTrips()");
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLES.TRIP_DEVICE.TABLE_NAME;

        Cursor c = db.rawQuery(selectQuery,null);
        List<Trip215> trips = new ArrayList<>();
        if(c.moveToFirst()) {
            do {
                int tripId = c.getInt(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_TRIP_ID));
                long tripIdRaw = c.getLong(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_TRIP_ID_RAW));
                double mileage = c.getDouble(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_MILEAGE));
                long rtc = c.getLong(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_RTC));
                String deviceId = c.getString(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_DEVICE_ID));
                trips.add(new Trip215(tripId, tripIdRaw, mileage, rtc, deviceId));
            } while (c.moveToNext());
        }
        return trips;
    }

    public void removeAllTrips(){
        Log.d(TAG,"removeAllTrips()");
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        try{
            db.delete(TABLES.TRIP_DEVICE.TABLE_NAME, null, null);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private ContentValues tripObjectToContentValues(Trip215 trip) {
        ContentValues values = new ContentValues();

        values.put(TABLES.TRIP_DEVICE.KEY_TRIP_ID,trip.getTripId());
        values.put(TABLES.TRIP_DEVICE.KEY_MILEAGE,trip.getMileage());
        values.put(TABLES.TRIP_DEVICE.KEY_RTC,trip.getRtcTime());
        values.put(TABLES.TRIP_DEVICE.KEY_DEVICE_ID,trip.getScannerName());
        values.put(TABLES.TRIP_DEVICE.KEY_TRIP_ID_RAW,trip.getTripIdRaw());

        return values;
    }

}
