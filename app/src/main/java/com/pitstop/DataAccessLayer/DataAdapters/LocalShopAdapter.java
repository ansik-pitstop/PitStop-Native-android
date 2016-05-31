package com.pitstop.DataAccessLayer.DataAdapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/31/2016.
 */
public class LocalShopAdapter {

    //SHOP table create statement
    public static final String CREATE_TABLE_DEALERSHIP = "CREATE TABLE IF NOT EXISTS "
            + TABLES.SHOP.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.SHOP.KEY_NAME + " TEXT, "
            + TABLES.SHOP.KEY_ADDRESS + " TEXT, "
            + TABLES.SHOP.KEY_PHONE + " TEXT, "
            + TABLES.SHOP.KEY_EMAIL + " TEXT, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalShopAdapter(Context context) {
        databaseHelper = new LocalDatabaseHelper(context);
    }

    /**
     * Dealership
     */
    public void storeDealership(Dealership dealership) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_OBJECT_ID, dealership.getId());
        values.put(TABLES.SHOP.KEY_NAME, dealership.getName());
        values.put(TABLES.SHOP.KEY_ADDRESS, dealership.getAddress());
        values.put(TABLES.SHOP.KEY_PHONE, dealership.getPhone());
        values.put(TABLES.SHOP.KEY_EMAIL, dealership.getEmail());

        db.insert(TABLES.SHOP.TABLE_NAME, null, values);
        db.close();
    }

    public void storeDealerships(List<Dealership> dealerships) {
        for(Dealership dealership : dealerships) {
            storeDealership(dealership);
        }
    }

    public Dealership getDealership(int shopId) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        Cursor c = db.query(TABLES.SHOP.TABLE_NAME, null,
                TABLES.COMMON.KEY_OBJECT_ID +"=?",new String[]{String.valueOf(shopId)},null,null,null);
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

        Cursor c = db.query(TABLES.SHOP.TABLE_NAME, null,null,null,null,null,null);
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
            db.delete(TABLES.SHOP.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                    new String[] { String.valueOf(dealership.getId()) });
        }

        db.close();
    }

    private Dealership cursorToDealership(Cursor c) {
        Dealership dealership = new Dealership();
        dealership.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)));

        dealership.setName(c.getString(c.getColumnIndex(TABLES.SHOP.KEY_NAME)));
        dealership.setAddress(c.getString(c.getColumnIndex(TABLES.SHOP.KEY_ADDRESS)));
        dealership.setPhoneNumber(c.getString(c.getColumnIndex(TABLES.SHOP.KEY_PHONE)));
        dealership.setEmail(c.getString(c.getColumnIndex(TABLES.SHOP.KEY_EMAIL)));

        return dealership;
    }
}
