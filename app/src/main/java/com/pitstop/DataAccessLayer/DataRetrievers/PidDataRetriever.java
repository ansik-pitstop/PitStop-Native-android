package com.pitstop.DataAccessLayer.DataRetrievers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.DTOs.Pid;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 4/1/2016.
 */
public class PidDataRetriever extends LocalDatabaseHelper{

    // PID_DATA table create statement
    private static final String CREATE_TABLE_PID_DATA = "CREATE TABLE "
            + TABLES.PID.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.PID.KEY_DATANUM + " TEXT,"
            + TABLES.PID.KEY_TIMESTAMP + " TEXT,"
            + TABLES.PID.KEY_RTCTIME + " TEXT,"
            + TABLES.PID.KEY_PIDS + " TEXT,"
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";


    public PidDataRetriever(Context context) {
        super(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_PID_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PID.TABLE_NAME);
        onCreate(db);
    }

    /**
     * Create pid data
     */
    public long createPIDData(Pid pidData) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.PID.KEY_DATANUM, pidData.getDataNumber());
        values.put(TABLES.PID.KEY_RTCTIME, pidData.getRtcTime());
        values.put(TABLES.PID.KEY_TIMESTAMP, pidData.getTimeStamp());
        values.put(TABLES.PID.KEY_PIDS, pidData.getPids());

        return db.insert(TABLES.PID.TABLE_NAME, null,values);
    }

    /**
     * Get all pid data
     */
    public List<Pid> getAllPidDataEntries() {
        List<Pid> pidDataEntries = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLES.PID.TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery,null);

        if(c.moveToFirst()) {
            do {
                Pid pidData = new Pid();
                pidData.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_ID)));
                pidData.setDataNumber(c.getString(c.getColumnIndex(TABLES.PID.KEY_DATANUM)));
                pidData.setRtcTime(c.getString(c.getColumnIndex(TABLES.PID.KEY_RTCTIME)));
                pidData.setTimeStamp(c.getString(c.getColumnIndex(TABLES.PID.KEY_TIMESTAMP)));
                pidData.setPids(c.getString(c.getColumnIndex(TABLES.PID.KEY_PIDS)));

                pidDataEntries.add(pidData);
            } while (c.moveToNext());
        }
        return pidDataEntries;
    }

    /**
     * Number of pid entries
     */
    public int getPidDataEntryCount() {
        String selectQuery = "SELECT * FROM " + TABLES.PID.TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery,null);
        return c.getCount();
    }

    /**
     * Clear all pid data entries
     */
    public void deleteAllPidDataEntries() {
        SQLiteDatabase db = this.getWritableDatabase();

        List<Pid> pidDataEntries = getAllPidDataEntries();

        for(Pid pid : pidDataEntries) {
            db.delete(TABLES.PID.TABLE_NAME, TABLES.COMMON.KEY_ID + " = ? ",
                    new String[] { String.valueOf(pid.getId()) });
        }
    }
}
