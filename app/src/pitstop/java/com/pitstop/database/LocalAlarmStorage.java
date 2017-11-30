package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.models.Alarm;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;

import java.util.ArrayList;
import java.util.List;

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
            + TABLES.LOCAL_ALARMS.RTC_TIME + " INTEGER" +")";

    private LocalDatabaseHelper databaseHelper;
    public LocalAlarmStorage(Context context){
        this.databaseHelper = LocalDatabaseHelper.getInstance(context);
    }



    public void storeAlarm(Alarm alarm,  Repository.Callback<Alarm> callback){
        Log.d(TAG, "storeAlarm " + Integer.toString(alarm.getCarID()) + " " + Integer.toString(alarm.getEvent()) +
                " " + Float.toString(alarm.getValue()) + " " + alarm.getRtcTime() );
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.execSQL(CREATE_LOCAL_ALARM_STORAGE);
        ContentValues values = new ContentValues();
        values.put(TABLES.LOCAL_ALARMS.CAR_ID, alarm.getCarID());
        values.put(TABLES.LOCAL_ALARMS.ALARM_EVENT, alarm.getEvent());
        values.put(TABLES.LOCAL_ALARMS.ALARM_VALUE, alarm.getValue());
        values.put(TABLES.LOCAL_ALARMS.RTC_TIME, alarm.getRtcTime());
        long result = db.insert(TABLES.LOCAL_ALARMS.TABLE_NAME, null, values);
        callback.onSuccess(alarm);
    }

    public void getAlarms(int carId, Repository.Callback<List<Alarm>> callback){
        Log.d(TAG, "getAlarms(): " + Integer.toString(carId));
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] values = {String.valueOf(carId)};
        ArrayList<Alarm> alarmArrayList = new ArrayList<>();
        if (doesTableExist(db, TABLES.LOCAL_ALARMS.TABLE_NAME)){
            Log.d(TAG, "alarmsTableExists");
            Cursor c = db.query(TABLES.LOCAL_ALARMS.TABLE_NAME, null, TABLES.LOCAL_ALARMS.CAR_ID
                    + "=? ORDER BY " + TABLES.LOCAL_ALARMS.RTC_TIME + " ASC", values, null, null, null);
            if(c.moveToFirst()) {
                Log.d(TAG, "alarmsTableHasEntries");
                while(!c.isAfterLast()) {
                    Alarm alarm = cursorToAlarm(c);
                    alarmArrayList.add(alarm);
                    Log.d(TAG, alarm.getName() );
                    c.moveToNext();
                }
            }
            c.close();
            callback.onSuccess(alarmArrayList);
        }
        else {
            Log.d(TAG, "can't get alarms");
            callback.onError(RequestError.getUnknownError());
        }
    }

    public void getAlarmCount(int carID, Repository.Callback<Integer> callback){

        Log.d(TAG, "getAlarmCount(): " + Integer.toString(carID));
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] values = {String.valueOf(carID)};
        ArrayList<Alarm> alarmArrayList = new ArrayList<>();
        if (doesTableExist(db, TABLES.LOCAL_ALARMS.TABLE_NAME)){
            Log.d(TAG, "alarmsTableExists");
            Cursor c = db.query(TABLES.LOCAL_ALARMS.TABLE_NAME, null, TABLES.LOCAL_ALARMS.CAR_ID + "=?", values, null, null, null);
            if(c.moveToFirst()) {
                Log.d(TAG, "alarmsTableHasEntries");
                while(!c.isAfterLast()) {
                    Alarm alarm = cursorToAlarm(c);
                    alarmArrayList.add(alarm);
                    Log.d(TAG, alarm.getName() );
                    c.moveToNext();
                }
            }
            c.close();
            callback.onSuccess(alarmArrayList.size());
        }
        else {
            Log.d(TAG, "can't get alarms");
            callback.onError(RequestError.getUnknownError());
        }

    }


    public Alarm cursorToAlarm(Cursor c){
        Alarm alarm = new Alarm(c.getInt(c.getColumnIndex(TABLES.LOCAL_ALARMS.ALARM_EVENT)),
                c.getFloat(c.getColumnIndex(TABLES.LOCAL_ALARMS.ALARM_VALUE)),
                String.valueOf((int)((c.getLong(c.getColumnIndex(TABLES.LOCAL_ALARMS.RTC_TIME))*Math.random()))),
                c.getInt(c.getColumnIndex(TABLES.LOCAL_ALARMS.CAR_ID)));
        return alarm;
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
