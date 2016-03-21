package com.pitstop.DataAccessLayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pitstop.DataAccessLayer.DTOs.Pid;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/16/2016.
 */
public class LocalDatabaseHelper extends SQLiteOpenHelper {
    // Logcat tag
    private static final String LOG = "LocalDatabaseHelper";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "PITSTOP_DB";

    // Table Names
    private static final String TABLE_PID_DATA = "pidData";

    // Common column names
    private static final String KEY_ID = "id";
    private static final String KEY_CREATED_AT = "createdAt";

    // PID_DATA Table - column names
    private static final String KEY_PID_DATA_DATANUM = "dataNum";
    private static final String KEY_PID_DATA_RTCTIME = "rtcTime";
    private static final String KEY_PID_DATA_TIMESTAMP = "timestamp";
    private static final String KEY_PID_DATA_PIDS = "pids";

    // CAR TABLE - column names
    private static final String KEY_CAR_VIN = "vin";
    private static final String KEY_CAR_DEALERSHIP_ID = "shopId";
    private static final String KEY_CAR_MILEAGE = "mileage";


    // Table Create Statements
    // PID_DATA table create statement
    private static final String CREATE_TABLE_PID_DATA = "CREATE TABLE "
            + TABLE_PID_DATA + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_PID_DATA_DATANUM + " TEXT,"
            + KEY_PID_DATA_TIMESTAMP + " TEXT,"
            + KEY_PID_DATA_RTCTIME + " TEXT,"
            + KEY_PID_DATA_PIDS + " TEXT,"
            + KEY_CREATED_AT + " DATETIME" + ")";

    // CAR table create statement
    private static final String CREATE_TABLE_CAR = "CREATE TABLE "
            + TABLE_PID_DATA + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_PID_DATA_DATANUM + " TEXT,"
            + KEY_PID_DATA_TIMESTAMP + " TEXT,"
            + KEY_PID_DATA_RTCTIME + " TEXT,"
            + KEY_PID_DATA_PIDS + " TEXT,"
            + KEY_CREATED_AT + " DATETIME" + ")";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_PID_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PID_DATA);

        // create new tables
        onCreate(db);
    }

    /**
     * Create pid data
     */
    public long createPIDData(Pid pidData) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PID_DATA_DATANUM,pidData.getDataNumber());
        values.put(KEY_PID_DATA_RTCTIME,pidData.getRtcTime());
        values.put(KEY_PID_DATA_TIMESTAMP, pidData.getTimeStamp());
        values.put(KEY_PID_DATA_PIDS, pidData.getPids());

        return db.insert(TABLE_PID_DATA, null,values);
    }

    /**
     * Get all pid data
     */
    public List<Pid> getAllPidDataEntries() {
        List<Pid> pidDataEntries = new ArrayList<>();

        String selectQuery = "SELECT * FROM " + TABLE_PID_DATA;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery,null);

        if(c.moveToFirst()) {
            do {
                Pid pidData = new Pid();
                pidData.setId(c.getInt(c.getColumnIndex(KEY_ID)));
                pidData.setDataNumber(c.getString(c.getColumnIndex(KEY_PID_DATA_DATANUM)));
                pidData.setRtcTime(c.getString(c.getColumnIndex(KEY_PID_DATA_RTCTIME)));
                pidData.setTimeStamp(c.getString(c.getColumnIndex(KEY_PID_DATA_TIMESTAMP)));
                pidData.setPids(c.getString(c.getColumnIndex(KEY_PID_DATA_PIDS)));

                pidDataEntries.add(pidData);
            } while (c.moveToNext());
        }
        return pidDataEntries;
    }

    /**
     * Number of pid entries
     */
    public int getPidDataEntryCount() {
        String selectQuery = "SELECT * FROM " + TABLE_PID_DATA;

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
            db.delete(TABLE_PID_DATA, KEY_ID + " = ? ",
                    new String[] { String.valueOf(pid.getId()) });
        }
    }

    public void closeDB() {
        SQLiteDatabase db = this.getReadableDatabase();
        if (db != null && db.isOpen())
            db.close();
    }
}
