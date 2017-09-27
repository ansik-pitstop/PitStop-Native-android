package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.models.Car;

/**
 * Created by ishan on 2017-09-26.
 */

public class LocalSpecsStorage {

    public static final String CREATE_LOCAL_SPEC_STORAGE = "CREATE TABLE IF NOT EXISTS "
            + TABLES.LOCAL_SPECS_DATA.TABLE_NAME + "("+ TABLES.LOCAL_SPECS_DATA.KEY_CAR_ID + " INTEGER, "
            +TABLES.LOCAL_SPECS_DATA.LICENSE_PLATE + " TEXT)";

    private LocalDatabaseHelper databaseHelper;

    public LocalSpecsStorage(Context context){
        this.databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storeLicensePlate(int carID, String licensePlate){

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TABLES.LOCAL_SPECS_DATA.KEY_CAR_ID, carID);
        values.put(TABLES.LOCAL_SPECS_DATA.LICENSE_PLATE, licensePlate);

        long result = db.insert(TABLES.LOCAL_SPECS_DATA.TABLE_NAME, null, values);
    }

    public String getLicensePlate(int carID){

        String licensePlate;
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.LOCAL_SPECS_DATA.TABLE_NAME,new String[] {TABLES.LOCAL_SPECS_DATA.LICENSE_PLATE},
                TABLES.LOCAL_SPECS_DATA.KEY_CAR_ID +"=?", new String[] {Integer.toString(carID)},null,null,null);
        if(c.moveToFirst()) {
            licensePlate = c.getString(c.getColumnIndex(TABLES.LOCAL_SPECS_DATA.LICENSE_PLATE));
        }
        else
            licensePlate = "";
        c.close();

        return licensePlate;
    }




}
