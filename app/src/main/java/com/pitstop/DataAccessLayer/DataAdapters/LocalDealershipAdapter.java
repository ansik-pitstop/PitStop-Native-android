package com.pitstop.DataAccessLayer.DataAdapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;
import com.pitstop.MainActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/31/2016.
 */
public class LocalDealershipAdapter {

    //DEALERSHIP table create statement
    public static final String CREATE_TABLE_DEALERSHIP = "CREATE TABLE IF NOT EXISTS "
            + TABLES.DEALERSHIP.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.DEALERSHIP.KEY_NAME + " TEXT, "
            + TABLES.DEALERSHIP.KEY_ADDRESS + " TEXT, "
            + TABLES.DEALERSHIP.KEY_PHONE + " TEXT, "
            + TABLES.DEALERSHIP.KEY_EMAIL + " TEXT, "
            + TABLES.COMMON.KEY_PARSE_ID + " TEXT, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalDealershipAdapter(Context context) {
        databaseHelper = new LocalDatabaseHelper(context);
    }

    /**
     * Dealership
     */
    public void storeDealership(Dealership dealership) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_PARSE_ID, dealership.getParseId());
        values.put(TABLES.DEALERSHIP.KEY_NAME, dealership.getName());
        values.put(TABLES.DEALERSHIP.KEY_ADDRESS, dealership.getAddress());
        values.put(TABLES.DEALERSHIP.KEY_PHONE, dealership.getPhone());
        values.put(TABLES.DEALERSHIP.KEY_EMAIL, dealership.getEmail());

        db.insert(TABLES.DEALERSHIP.TABLE_NAME, null, values);

        db.close();
    }

    public void storeDealerships(List<Dealership> dealerships) {
        for(Dealership dealership : dealerships) {
            storeDealership(dealership);
        }
    }

    public Dealership getDealership(String shopId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        Cursor c = db.query(TABLES.DEALERSHIP.TABLE_NAME, null,
                TABLES.COMMON.KEY_PARSE_ID+"=?",new String[]{shopId},null,null,null);
        if(c.getCount() == 0) {
            return null;
        }

        c.moveToFirst();
        Dealership dealership = cursorToDealership(c);
        db.close();

        return dealership;
    }

    public List<Dealership> getAllDealerships() {
        List<Dealership> dealerships = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.DEALERSHIP.TABLE_NAME, null,null,null,null,null,null);
        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                dealerships.add(cursorToDealership(c));
                c.moveToNext();
            }
        }

        db.close();
        return dealerships;
    }

    /**
     *  Delete all dealerships
     */
    public void deleteAllDealerships() {
        List<Dealership> dealerships = getAllDealerships();

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        for(Dealership dealership : dealerships) {
            db.delete(TABLES.DEALERSHIP.TABLE_NAME, TABLES.COMMON.KEY_ID + "=?",
                    new String[] { String.valueOf(dealership.getId()) });
        }

        db.close();
    }

    private Dealership cursorToDealership(Cursor c) {
        Dealership dealership = new Dealership();
        dealership.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_ID)));
        dealership.setParseId(c.getString(c.getColumnIndex(TABLES.COMMON.KEY_PARSE_ID)));

        dealership.setName(c.getString(c.getColumnIndex(TABLES.DEALERSHIP.KEY_NAME)));
        dealership.setAddress(c.getString(c.getColumnIndex(TABLES.DEALERSHIP.KEY_ADDRESS)));
        dealership.setPhoneNumber(c.getString(c.getColumnIndex(TABLES.DEALERSHIP.KEY_PHONE)));
        dealership.setEmail(c.getString(c.getColumnIndex(TABLES.DEALERSHIP.KEY_EMAIL)));

        return dealership;
    }
}
