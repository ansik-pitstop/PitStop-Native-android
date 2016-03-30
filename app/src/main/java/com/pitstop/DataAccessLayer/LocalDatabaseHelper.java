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
    private static final String TABLE_CAR = "car";
    private static final String TABLE_DEALERSHIP = "dealership";
    private static final String TABLE_DTC = "dtc";
    private static final String TABLE_RECALL = "recall";
    private static final String TABLE_SERVICE = "service";

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
    private static final String KEY_CAR_SCANNER_ID = "scannerId";
    private static final String KEY_CAR_MAKE = "make";
    private static final String KEY_CAR_MODEL = "model";
    private static final String KEY_CAR_YEAR = "year";
    private static final String KEY_CAR_DEALERSHIP = "dealership";


    // DEALERSHIP TABLE - column names
    private static final String KEY_DEALERSHIP_ID = "dealershipId";
    private static final String KEY_DEALERSHIP_NAME = "name";
    private static final String KEY_DEALERSHIP_ADDRESS = "address";
    private static final String KEY_DEALERSHIP_PHONE = "phone";
    private static final String KEY_DEALERSHIP_EMAIL = "email";

    // DTC TABLE - column names
    private static final String KEY_DTC_CODE = "dtcCode";
    private static final String KEY_DTC_DESCRIPTION = "codeDescription";

    // RECALL TABLE - column names
    private static final String KEY_RECALL_ID = "recallId";
    private static final String KEY_RECALL_NAME = "name";
    private static final String KEY_RECALL_DESCRIPTION = "description";
    private static final String KEY_RECALL_REMEDY = "remedy";
    private static final String KEY_RECALL_RISK = "risk";
    private static final String KEY_RECALL_EFFECTIVE_DATE = "effectiveDate";
    private static final String KEY_RECALL_OEM_ID = "oemId";
    private static final String KEY_RECALL_REIMBURSEMENT = "reimbursement";
    private static final String KEY_RECALL_STATE = "state";
    private static final String KEY_RECALL_RISK_RANK = "riskRank";

    // SERVICE TABLE - column names
    private static final String KEY_SERVICE_TYPE = "serviceType";
    private static final String KEY_SERVICE_DESCRIPTION = "description";
    private static final String KEY_SERVICE_ITEM = "item";
    private static final String KEY_SERVICE_ACTION = "action";
    private static final String KEY_SERVICE_INTERVAL_MONTH = "intervalMonth";
    private static final String KEY_SERVICE_INTERVAL_MILEAGE = "intervalMileage";
    private static final String KEY_SERVICE_INTERVAL_FIXED = "intervalFixed";
    private static final String KEY_SERVICE_PRIORITY = "priority";
    private static final String KEY_SERVICE_DEALERSHIP = "dealership";


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
            + TABLE_CAR + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_CAR_VIN + " TEXT, "
            + KEY_CAR_MILEAGE + " TEXT, "
            + KEY_CAR_DEALERSHIP_ID + " TEXT, "
            + KEY_PID_DATA_PIDS + " TEXT, "
            + KEY_CREATED_AT + " DATETIME" + ")";

    //DEALERSHIP table create statement
    private static final String CREATE_TABLE_DEALERSHIP = "CREATE TABLE "
            + TABLE_DEALERSHIP + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_DEALERSHIP_ID + " INTEGER, "
            + KEY_DEALERSHIP_NAME + " TEXT, "
            + KEY_DEALERSHIP_ADDRESS + " TEXT, "
            + KEY_DEALERSHIP_PHONE + " TEXT, "
            + KEY_DEALERSHIP_EMAIL + " TEXT, "
            + KEY_CREATED_AT + " DATETIME" + ")";

    //DTC table create statement
    private static final String CREATE_TABLE_DTC = "CREATE TABLE "
            + TABLE_DTC + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + KEY_DTC_CODE + " TEXT, "
            + KEY_DTC_DESCRIPTION + " TEXT, "
            + KEY_CREATED_AT + " DATETIME" + ")";

    //RECALL table create statement
    private static final String CREATE_TABLE_RECALL = "CREATE TABLE "
            + TABLE_RECALL + "(" + KEY_ID + "INTEGER PRIMARY KEY,"
            + KEY_RECALL_ID + " INTEGER, "
            + KEY_RECALL_NAME + " TEXT, "
            + KEY_RECALL_DESCRIPTION + " TEXT, "
            + KEY_RECALL_REMEDY + " TEXT, "
            + KEY_RECALL_RISK + " TEXT, "
            + KEY_RECALL_EFFECTIVE_DATE + " TEXT, "
            + KEY_RECALL_OEM_ID + " TEXT, "
            + KEY_RECALL_REIMBURSEMENT + " TEXT, "
            + KEY_RECALL_STATE + " TEXT, "
            + KEY_RECALL_RISK_RANK + "INTEGER"
            + KEY_CREATED_AT + " DATETIME" + ")";

    //SERVICE table create statement
    private static final String CREATE_TABLE_SERVICE = "CREATE TABLE "
            + TABLE_SERVICE + "(" + KEY_ID + " INTEGER PRIMARY KEY, "
            + KEY_SERVICE_TYPE + " TEXT, "
            + KEY_SERVICE_DEALERSHIP + " TEXT, "
            + KEY_SERVICE_ITEM + " TEXT, "
            + KEY_CREATED_AT + " DATETIME" + ")";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating required tables
        db.execSQL(CREATE_TABLE_PID_DATA);
        db.execSQL(CREATE_TABLE_CAR);
        db.execSQL(CREATE_TABLE_DEALERSHIP);
        db.execSQL(CREATE_TABLE_DTC);
        db.execSQL(CREATE_TABLE_RECALL);
        db.execSQL(CREATE_TABLE_SERVICE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PID_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAR);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEALERSHIP);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DTC);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECALL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICE);

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
