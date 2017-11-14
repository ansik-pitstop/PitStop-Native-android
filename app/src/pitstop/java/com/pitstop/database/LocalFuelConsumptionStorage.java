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
            + TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME + "("+ TABLES.LOCAL_FUEL_CONSUMPTION.CAR_ID + " INTEGER PRIMARY KEY,"
            + TABLES.LOCAL_FUEL_CONSUMPTION.FUEL_CONSUMED + " REAL" +")";

    private LocalDatabaseHelper databaseHelper;
    public LocalFuelConsumptionStorage(Context context){
        this.databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storeFuelConsumed(int CarId, double fuelConsumed, Repository.Callback<Double> callback){
        Log.d(TAG, "storeFuelConsumed, CarID: " + Integer.toString(CarId) + "FuelCOnsumed: " + fuelConsumed);
        SQLiteDatabase db  = databaseHelper.getWritableDatabase();
        db.execSQL(CREATE_LOCAL_FUEL_CONSUMPTION_STORAGE);
        ContentValues values = new ContentValues();
        values.put(TABLES.LOCAL_FUEL_CONSUMPTION.CAR_ID, CarId);
        values.put(TABLES.LOCAL_FUEL_CONSUMPTION.FUEL_CONSUMED, fuelConsumed);
        long result = db.insert(TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME, null, values);
        callback.onSuccess(fuelConsumed);
        printAllRecords();
    }

    public void getFuelConsumed(int carID, Repository.Callback<Double> callback){
        Log.d(TAG, "getFuelConsumed: " + carID);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] values = {String.valueOf(carID)};
        if (doesTableExist(db, TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME)){
            Cursor c = db.query(TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME, null, TABLES.LOCAL_FUEL_CONSUMPTION.CAR_ID + "=?", values, null, null, null);
            if (c.moveToFirst()) {
                callback.onSuccess(c.getDouble(c.getColumnIndex(TABLES.LOCAL_FUEL_CONSUMPTION.FUEL_CONSUMED)));
            } else {
                Log.d(TAG, "failure");
                c.close();
                callback.onError(RequestError.getUnknownError());
            }
        }
    }

    public void printAllRecords(){
        Log.d(TAG, "printAllRecords");
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from " + TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                int carId = cursor.getInt(cursor.getColumnIndex(TABLES.LOCAL_FUEL_CONSUMPTION.CAR_ID));
                double fuelConsumed = cursor.getDouble(cursor.getColumnIndex(TABLES.LOCAL_FUEL_CONSUMPTION.FUEL_CONSUMED));
                Log.d(TAG, "carId: " + Integer.toString(carId)  + " ,fuelConsumed: " + Double.toString(fuelConsumed));
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
