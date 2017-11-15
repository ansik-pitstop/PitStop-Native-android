package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;

/**
 * Created by ishan on 2017-11-14.
 */

public class LocalFuelConsumptionStorage {

    public static final String TAG = LocalFuelConsumptionStorage.class.getSimpleName();

    public static final String CREATE_LOCAL_FUEL_CONSUMPTION_STORAGE = "CREATE TABLE IF NOT EXISTS "
            + TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME + "("+ TABLES.LOCAL_FUEL_CONSUMPTION.SCANNER_ID + " TEXT PRIMARY KEY,"
            + TABLES.LOCAL_FUEL_CONSUMPTION.FUEL_CONSUMED + " REAL" +")";

    private LocalDatabaseHelper databaseHelper;
    public LocalFuelConsumptionStorage(Context context){
        this.databaseHelper = LocalDatabaseHelper.getInstance(context);

    }

    public void storeFuelConsumed(String scannerID, double fuelConsumed, Repository.Callback<Double> callback){
        Log.d(TAG, "storeFuelConsumed, scanner: " + scannerID + "FuelCOnsumed: " + fuelConsumed);
        SQLiteDatabase db  = databaseHelper.getWritableDatabase();
        db.execSQL(CREATE_LOCAL_FUEL_CONSUMPTION_STORAGE);
        ContentValues values = new ContentValues();
        values.put(TABLES.LOCAL_FUEL_CONSUMPTION.SCANNER_ID, scannerID);
        values.put(TABLES.LOCAL_FUEL_CONSUMPTION.FUEL_CONSUMED, fuelConsumed);
        db.execSQL(CREATE_LOCAL_FUEL_CONSUMPTION_STORAGE);db.execSQL(CREATE_LOCAL_FUEL_CONSUMPTION_STORAGE);
        int id = (int)db.insertWithOnConflict(TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (id == -1) {
            db.update(TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME, values,
                    TABLES.LOCAL_FUEL_CONSUMPTION.SCANNER_ID + "= '" + scannerID + "'", null);
        }
        callback.onSuccess(fuelConsumed);
        printAllRecords();
    }

    public void getFuelConsumed(String Scannerid, Repository.Callback<Double> callback){
        Log.d(TAG, "getFuelConsumed: " + Scannerid);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] values = {Scannerid};
        db.execSQL(CREATE_LOCAL_FUEL_CONSUMPTION_STORAGE);
        if (doesTableExist(db, TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME)){
            Cursor c = db.query(TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME, null, TABLES.LOCAL_FUEL_CONSUMPTION.SCANNER_ID + "=?", values, null, null, null);
            if (c.moveToFirst()) {
                callback.onSuccess(c.getDouble(c.getColumnIndex(TABLES.LOCAL_FUEL_CONSUMPTION.FUEL_CONSUMED)));
            } else {
                Log.d(TAG, "failure");
                c.close();
                callback.onSuccess((double)0.0);
            }
        }
    }

    public void printAllRecords(){
        Log.d(TAG, "printAllRecords");
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String scannerid = cursor.getString(cursor.getColumnIndex(TABLES.LOCAL_FUEL_CONSUMPTION.SCANNER_ID));
                double fuelConsumed = cursor.getDouble(cursor.getColumnIndex(TABLES.LOCAL_FUEL_CONSUMPTION.FUEL_CONSUMED));
                Log.d(TAG, "scannerID: " + scannerid  + " ,fuelConsumed: " + Double.toString(fuelConsumed));
                cursor.moveToNext();
            }
        }

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



}
