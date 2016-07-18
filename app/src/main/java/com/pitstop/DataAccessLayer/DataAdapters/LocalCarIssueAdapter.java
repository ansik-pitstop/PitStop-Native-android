package com.pitstop.DataAccessLayer.DataAdapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.DataAccessLayer.DTOs.CarIssueDetail;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 04/04/2016.
 */
public class LocalCarIssueAdapter {
    // CAR_ISSUES table create statement
    public static final String CREATE_TABLE_CAR_ISSUES = "CREATE TABLE "
            + TABLES.CAR_ISSUES.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.CAR_ISSUES.KEY_CAR_ID + " INTEGER, "
            + TABLES.CAR_ISSUES.KEY_STATUS+ " TEXT, "
            + TABLES.CAR_ISSUES.KEY_TIMESTAMP + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_ISSUE_TYPE + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_PRIORITY + " INTEGER, "
            + TABLES.CAR_ISSUES.KEY_ITEM + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_DESCRIPTION + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_ACTION+ " TEXT, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalCarIssueAdapter(Context context) {
        databaseHelper = new LocalDatabaseHelper(context);
    }

    public void storeCarIssue(CarIssue carIssue) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = carIssueObjectToContentValues(carIssue);

        db.insert(TABLES.CAR_ISSUES.TABLE_NAME, null, values);
        db.close();
    }

    public void storeCarIssues(List<CarIssue> carIssues) {
        for(CarIssue carIssue : carIssues) {
            storeCarIssue(carIssue);
        }
    }

    public int updateCarIssue(CarIssue carIssue) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = carIssueObjectToContentValues(carIssue);

        int rows = db.update(TABLES.CAR_ISSUES.TABLE_NAME, values,
                TABLES.CAR_ISSUES.KEY_CAR_ISSUE_ID+"=?",
                new String[] {String.valueOf(carIssue.getId())});
        db.close();
        return rows;
    }

    public ArrayList<CarIssue> getAllCarIssues(int carId) {

        ArrayList<CarIssue> carIssues = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,
                TABLES.CAR_ISSUES.KEY_CAR_ID+"=?",new String[]{String.valueOf(carId)},null,null,null);
        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                carIssues.add(cursorToCarIssue(c));
                c.moveToNext();
            }
        }

        db.close();
        return carIssues;
    }

    private List<CarIssue> getAllCarIssues() {
        List<CarIssue> carIssues = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,null,null,null,null,null);
        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                carIssues.add(cursorToCarIssue(c));
                c.moveToNext();
            }
        }

        db.close();
        return carIssues;
    }


    public void deleteCarIssue(CarIssue issue) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.CAR_ISSUES.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                new String[] { String.valueOf(issue.getId()) });

        db.close();
    }

    public void deleteAllCarIssues() {
        List<CarIssue> carIssueEntries = getAllCarIssues();

        for(CarIssue issue : carIssueEntries) {
            deleteCarIssue(issue);
        }
    }

    private CarIssue cursorToCarIssue(Cursor c) {
        CarIssue carIssue = new CarIssue();
        carIssue.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)));

        carIssue.setCarId(c.getInt(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_CAR_ID)));
        carIssue.setStatus(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_STATUS)));
        carIssue.setIssueType(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ISSUE_TYPE)));
        carIssue.setPriority(c.getInt(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_PRIORITY)));

        CarIssueDetail carIssueDetail = new CarIssueDetail();
        carIssueDetail.setItem(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ITEM)));
        carIssueDetail.setDescription(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_DESCRIPTION)));
        carIssueDetail.setAction(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ACTION)));
        carIssue.setIssueDetail(carIssueDetail);

        return carIssue;
    }


    private ContentValues carIssueObjectToContentValues(CarIssue carIssue) {
        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_OBJECT_ID, carIssue.getId());
        values.put(TABLES.CAR_ISSUES.KEY_CAR_ID, carIssue.getCarId());
        values.put(TABLES.CAR_ISSUES.KEY_STATUS, carIssue.getStatus());
        values.put(TABLES.CAR_ISSUES.KEY_ISSUE_TYPE, carIssue.getIssueType());
        values.put(TABLES.CAR_ISSUES.KEY_PRIORITY, carIssue.getPriority());
        values.put(TABLES.CAR_ISSUES.KEY_ITEM, carIssue.getIssueDetail().getItem());
        values.put(TABLES.CAR_ISSUES.KEY_DESCRIPTION, carIssue.getIssueDetail().getDescription());
        values.put(TABLES.CAR_ISSUES.KEY_ACTION, carIssue.getIssueDetail().getAction());

        return values;
    }
}


