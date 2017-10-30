package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.models.Alarm;
import com.pitstop.repositories.Repository;

/**
 * Created by ishan on 2017-10-30.
 */

public class LocalAlarmStorage {

    private static final String TAG = LocalAlarmStorage.class.getSimpleName();

    public static final String CREATE_LOCAL_ALARM_STORAGE =  "CREATE TABLE IF NOT EXISTS "
            + TABLES.LOCAL_ALARMS.TABLE_NAME + "("+ TABLES.LOCAL_ALARMS.ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TABLES.LOCAL_ALARMS.CAR_ID + " INTEGER,"
            + TABLES.LOCAL_ALARMS.ALARM_EVENT + " INTEGER,"
            + TABLES.LOCAL_ALARMS.ALARM_VALUE + " REAL,"
            + TABLES.LOCAL_ALARMS.RTC_TIME + " TEXT" +")";

    private LocalDatabaseHelper databaseHelper;
    public LocalAlarmStorage(Context context){
        this.databaseHelper = LocalDatabaseHelper.getInstance(context);
    }



    public void storeAlarm(Alarm alarm,  Repository.Callback<Alarm> callback){
        Log.d(TAG, "storeAlarm " + Integer.toString(alarm.getCarID()) + " " + Integer.toString(alarm.getAlarmEvent()) +
                " " + Float.toString(alarm.getAlarmValue()) + " " + alarm.getRtcTime() );
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.execSQL(CREATE_LOCAL_ALARM_STORAGE);
        ContentValues values = new ContentValues();
        values.put(TABLES.LOCAL_ALARMS.CAR_ID, alarm.getCarID());
        values.put(TABLES.LOCAL_ALARMS.ALARM_EVENT, alarm.getAlarmEvent());
        values.put(TABLES.LOCAL_ALARMS.ALARM_VALUE, alarm.getAlarmValue());
        values.put(TABLES.LOCAL_ALARMS.RTC_TIME, alarm.getRtcTime());
        long result = db.insert(TABLES.LOCAL_ALARMS.TABLE_NAME, null, values);
        callback.onSuccess(alarm);
    }


    public boolean doesTableExist(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }


    public void deleteAllRows() {
        Log.d(TAG,"deleteAllRows()");
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(TABLES.LOCAL_ALARMS.TABLE_NAME, null, null);
    }


}
