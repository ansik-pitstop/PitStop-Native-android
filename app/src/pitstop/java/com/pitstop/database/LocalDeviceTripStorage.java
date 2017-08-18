package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.TripInfoPackage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 8/18/2017.
 */

public class LocalDeviceTripStorage {

    private final String TAG = getClass().getSimpleName();

    private final String TYPE_START = "type_start";
    private final String TYPE_END = "type_end";

    public static final String CREATE_TABLE_DEVICE_TRIP = "CREATE TABLE IF NOT EXISTS "
            + TABLES.TRIP_DEVICE.TABLE_NAME + "("
            + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.TRIP_DEVICE.KEY_MILEAGE + " DOUBLE, "
            + TABLES.TRIP_DEVICE.KEY_RTC + " INTEGER, "
            + TABLES.TRIP_DEVICE.KEY_TERMINAL_RTC + " INTEGER, "
            + TABLES.TRIP_DEVICE.KEY_DEVICE_ID + " TEXT, "
            + TABLES.TRIP_DEVICE.KEY_TRIP_ID_RAW + " INTEGER, "
            + TABLES.TRIP_DEVICE.KEY_TRIP_TYPE + " TEXT, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalDeviceTripStorage(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storeDeviceTrip(TripInfoPackage trip){
        Log.d(TAG,"storeDeviceTripEnd() tripIdRaw:"+trip.tripId);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = tripObjectToContentValues(trip);

        db.insert(TABLES.TRIP_DEVICE.TABLE_NAME, null, values);
    }

    public List<TripInfoPackage> getAllTrips(){
        Log.d(TAG,"getAllTrips()");
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLES.TRIP_DEVICE.TABLE_NAME;

        Cursor c = db.rawQuery(selectQuery,null);
        List<TripInfoPackage> trips = new ArrayList<>();
        if(c.moveToFirst()) {
            do {
                TripInfoPackage tripInfoPackage = new TripInfoPackage();
                tripInfoPackage.tripId = c.getInt(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_TRIP_ID_RAW));
                tripInfoPackage.mileage = c.getDouble(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_MILEAGE));
                tripInfoPackage.rtcTime = c.getLong(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_RTC));
                tripInfoPackage.deviceId = c.getString(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_DEVICE_ID));
                tripInfoPackage.terminalRtcTime = c.getInt(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_TERMINAL_RTC));
                tripInfoPackage.flag = c.getString(c.getColumnIndex(TABLES.TRIP_DEVICE.KEY_TRIP_TYPE))
                        .equals(TYPE_START) ? TripInfoPackage.TripFlag.START : TripInfoPackage.TripFlag.END;

                trips.add(tripInfoPackage);
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

    public void deleteAllRows(){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.TRIP_DEVICE.TABLE_NAME, null, null);
    }

    private ContentValues tripObjectToContentValues(TripInfoPackage trip) {
        ContentValues values = new ContentValues();

        values.put(TABLES.TRIP_DEVICE.KEY_MILEAGE,trip.mileage);
        values.put(TABLES.TRIP_DEVICE.KEY_RTC,trip.rtcTime);
        values.put(TABLES.TRIP_DEVICE.KEY_DEVICE_ID,trip.deviceId);
        values.put(TABLES.TRIP_DEVICE.KEY_TRIP_ID_RAW,trip.tripId);
        values.put(TABLES.TRIP_DEVICE.KEY_TERMINAL_RTC, trip.terminalRtcTime);

        if (trip.flag == TripInfoPackage.TripFlag.START){
            values.put(TABLES.TRIP_DEVICE.KEY_TRIP_TYPE, TYPE_START);
        }
        else if (trip.flag == TripInfoPackage.TripFlag.END){
            values.put(TABLES.TRIP_DEVICE.KEY_TRIP_TYPE, TYPE_END);
        }


        return values;
    }

}
