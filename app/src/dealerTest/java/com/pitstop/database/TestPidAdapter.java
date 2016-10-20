package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.models.Pid;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yifan on 16/10/14.
 */

public class TestPidAdapter {

    // PID_DATA table create statement
    public static final String CREATE_TABLE_PID_DATA = "CREATE TABLE IF NOT EXISTS "
            + TABLES.PID.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.PID.KEY_DATANUM + " TEXT,"
            + TABLES.PID.KEY_RTCTIME + " TEXT,"
            + TABLES.PID.KEY_PIDS + " TEXT,"
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private TestDatabaseHelper databaseHelper;

    public TestPidAdapter(Context context) {
        databaseHelper = TestDatabaseHelper.getInstance(context);
    }

    /**
     * Create pid data
     */
    public void createPIDData(Pid pidData) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.PID.KEY_DATANUM, pidData.getDataNumber());
        values.put(TABLES.PID.KEY_RTCTIME, pidData.getRtcTime());
        values.put(TABLES.PID.KEY_PIDS, pidData.getPids());

        db.insert(TABLES.PID.TABLE_NAME, null, values);
        db.close();
    }

    public void createPIDData(String dataNum, String rtcTime, int tripId, String pids){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.PID.KEY_DATANUM, dataNum);
        values.put(TABLES.PID.KEY_RTCTIME, rtcTime);
        values.put(TABLES.PID.KEY_PIDS, pids);

        db.insert(TABLES.PID.TABLE_NAME, null, values);
        db.close();
    }

    /**
     * Get all pid data
     */
    public List<Pid> getAllPidDataEntries() {
        List<Pid> pidDataEntries = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLES.PID.TABLE_NAME;

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery,null);

        if(c.moveToFirst()) {
            do {
                Pid pidData = new Pid();
                pidData.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_ID)));
                pidData.setDataNumber(c.getString(c.getColumnIndex(TABLES.PID.KEY_DATANUM)));
                pidData.setRtcTime(c.getString(c.getColumnIndex(TABLES.PID.KEY_RTCTIME)));
                pidData.setPids(c.getString(c.getColumnIndex(TABLES.PID.KEY_PIDS)));

                pidDataEntries.add(pidData);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return pidDataEntries;
    }

    /**
     * Number of pid entries
     */
    public int getPidDataEntryCount() {
        String selectQuery = "SELECT * FROM " + TABLES.PID.TABLE_NAME;

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);
        int count = c.getCount();
        c.close();
        db.close();
        return count;
    }

    /**
     * Clear all pid data entries
     */
    synchronized public void deleteAllPidDataEntries() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        try {
            db.delete(TABLES.PID.TABLE_NAME, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(db != null && db.isOpen()) {
                db.close();
            }
        }
    }

}
