package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.models.Pid;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ben Wu on today
 */
public class LocalPidResult4Helper {

    // PID_DATA table create statement
    public static final String CREATE_TABLE_PID_DATA = "CREATE TABLE IF NOT EXISTS "
            + TABLES.PID.TABLE_NAME_RESULT_4 + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.PID.KEY_DATANUM + " TEXT,"
            + TABLES.PID.KEY_TIMESTAMP + " TEXT,"
            + TABLES.PID.KEY_RTCTIME + " TEXT,"
            + TABLES.PID.KEY_PIDS + " TEXT,"
            + TABLES.PID.KEY_TRIP_ID + " INTEGER,"
            + TABLES.PID.KEY_MILEAGE + " REAL,"
            + TABLES.PID.KEY_CALCULATED_MILEAGE + " REAL,"
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;


    public LocalPidResult4Helper(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    /**
     * Create pid data
     */
    public void createPIDData(Pid pidData) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.PID.KEY_DATANUM, pidData.getDataNumber());
        values.put(TABLES.PID.KEY_RTCTIME, pidData.getRtcTime());
        values.put(TABLES.PID.KEY_TIMESTAMP, pidData.getTimeStamp());
        values.put(TABLES.PID.KEY_TRIP_ID, pidData.getTripId());
        values.put(TABLES.PID.KEY_PIDS, pidData.getPids());
        values.put(TABLES.PID.KEY_MILEAGE, pidData.getMileage());
        values.put(TABLES.PID.KEY_CALCULATED_MILEAGE, pidData.getCalculatedMileage());

        db.insert(TABLES.PID.TABLE_NAME_RESULT_4, null, values);

    }

    /**
     * Get all pid data
     */
    public List<Pid> getAllPidDataEntries() {
        List<Pid> pidDataEntries = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLES.PID.TABLE_NAME_RESULT_4;

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery,null);

        if(c.moveToFirst()) {
            do {
                Pid pidData = new Pid();
                pidData.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_ID)));
                pidData.setDataNumber(c.getString(c.getColumnIndex(TABLES.PID.KEY_DATANUM)));
                pidData.setRtcTime(c.getString(c.getColumnIndex(TABLES.PID.KEY_RTCTIME)));
                pidData.setTimeStamp(c.getString(c.getColumnIndex(TABLES.PID.KEY_TIMESTAMP)));
                pidData.setTripId(c.getInt(c.getColumnIndex(TABLES.PID.KEY_TRIP_ID)));
                pidData.setPids(c.getString(c.getColumnIndex(TABLES.PID.KEY_PIDS)));
                pidData.setMileage(c.getDouble(c.getColumnIndex(TABLES.PID.KEY_MILEAGE)));
                pidData.setCalculatedMileage(c.getDouble(c.getColumnIndex(TABLES.PID.KEY_CALCULATED_MILEAGE)));

                pidDataEntries.add(pidData);
            } while (c.moveToNext());
        }
        return pidDataEntries;
    }

    /**
     * Number of pid entries
     */
    public int getPidDataEntryCount() {
        String selectQuery = "SELECT * FROM " + TABLES.PID.TABLE_NAME_RESULT_4;

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        int count = c.getCount();

        return count;
    }

    /**
     * Clear all pid data entries
     */
    synchronized public void deleteAllPidDataEntries() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        try {
            db.delete(TABLES.PID.TABLE_NAME_RESULT_4, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
