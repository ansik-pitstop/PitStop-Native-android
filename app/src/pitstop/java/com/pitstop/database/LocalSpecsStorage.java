package com.pitstop.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;

/**
 * Created by ishan on 2017-09-26.
 */
public class LocalSpecsStorage {

    private static final String TAG = LocalSpecsStorage.class.getSimpleName();

    public static final String CREATE_LOCAL_SPEC_STORAGE = "CREATE TABLE IF NOT EXISTS "
            + TABLES.LOCAL_SPECS_DATA.TABLE_NAME + "("+ TABLES.LOCAL_SPECS_DATA.KEY_CAR_ID + " INTEGER PRIMARY KEY,"
            +TABLES.LOCAL_SPECS_DATA.LICENSE_PLATE + " TEXT" +")";

    private LocalDatabaseHelper databaseHelper;

    public LocalSpecsStorage(LocalDatabaseHelper databaseHelper){
        this.databaseHelper = databaseHelper;
    }

    public void storeLicensePlate(int carID, String licensePlate, Repository.Callback<String> callback){
        Log.d(TAG, "storeLicensePlate " + Integer.toString(carID) + " " + licensePlate );
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.execSQL(LocalSpecsStorage.CREATE_LOCAL_SPEC_STORAGE);
        ContentValues values = new ContentValues();
        values.put(TABLES.LOCAL_SPECS_DATA.KEY_CAR_ID, carID);
        values.put(TABLES.LOCAL_SPECS_DATA.LICENSE_PLATE, licensePlate);
        Log.d("LocalSpecsStorage", Integer.toString(carID) + " " + licensePlate);
        long result = db.insert(TABLES.LOCAL_SPECS_DATA.TABLE_NAME, null, values);
        callback.onSuccess(licensePlate);
    }

    public void getLicensePlate(int carID, Repository.Callback<String> callback){
        Log.d(TAG, "getLicensePlate " + Integer.toString(carID));
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] values = {String.valueOf(carID)};
        if (doesTableExist(db,TABLES.LOCAL_SPECS_DATA.TABLE_NAME )) {
            Cursor c = db.query(TABLES.LOCAL_SPECS_DATA.TABLE_NAME, null, TABLES.LOCAL_SPECS_DATA.KEY_CAR_ID + "=?", values, null, null, null);
            if (c.moveToFirst()) {
                callback.onSuccess(c.getString(c.getColumnIndex(TABLES.LOCAL_SPECS_DATA.LICENSE_PLATE)));
            } else {
                Log.d(TAG, "failure");
                c.close();
                callback.onError(RequestError.getUnknownError());
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

    public void deleteRecord(int carID){
        Log.d(TAG, "deleteLicensePlate " + Integer.toString(carID));
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        String[] values = {String.valueOf(carID)};
        if (doesTableExist(db, TABLES.LOCAL_SPECS_DATA.TABLE_NAME))
            db.delete(TABLES.LOCAL_SPECS_DATA.TABLE_NAME, TABLES.LOCAL_SPECS_DATA.KEY_CAR_ID + "=?", values);
    }

    public void deleteAllRows() {
        Log.d(TAG,"removeAllDealerships()");
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(TABLES.LOCAL_SPECS_DATA.TABLE_NAME, null, null);
    }
}
