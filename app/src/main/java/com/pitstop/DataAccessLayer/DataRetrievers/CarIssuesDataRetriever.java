package com.pitstop.DataAccessLayer.DataRetrievers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.DataAccessLayer.DTOs.CarIssueDetail;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 04/04/2016.
 */
public class CarIssuesDataRetriever {
    // CAR_ISSUES table create statement
    public static final String CREATE_TABLE_CAR_ISSUES = "CREATE TABLE "
            + TABLES.CAR_ISSUES.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.CAR_ISSUES.KEY_CAR_ID + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_STATUS+ " TEXT, "
            + TABLES.CAR_ISSUES.KEY_TIMESTAMP + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_ISSUE_TYPE + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_PRIORITY + " INTEGER, "
            + TABLES.CAR_ISSUES.KEY_ITEM + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_DESCRIPTION + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_ACTION+ " TEXT, "
            + TABLES.COMMON.KEY_PARSE_ID + " TEXT, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public CarIssuesDataRetriever(Context context) {
        databaseHelper = new LocalDatabaseHelper(context);
        Log.i("CarIssueDataR", "constructor");
    }

    /*@Override
    public void onCreate(SQLiteDatabase db) {
        super.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);
    }*/

    public void storeCarIssue(CarIssue carIssue) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_PARSE_ID, carIssue.getParseId());
        values.put(TABLES.CAR_ISSUES.KEY_CAR_ID, carIssue.getCarId());
        values.put(TABLES.CAR_ISSUES.KEY_STATUS, carIssue.getStatus());
        values.put(TABLES.CAR_ISSUES.KEY_ISSUE_TYPE, carIssue.getIssueType());
        values.put(TABLES.CAR_ISSUES.KEY_PRIORITY, carIssue.getPriority());
        values.put(TABLES.CAR_ISSUES.KEY_ITEM, carIssue.getIssueDetail().getItem());
        values.put(TABLES.CAR_ISSUES.KEY_DESCRIPTION, carIssue.getIssueDetail().getDescription());
        values.put(TABLES.CAR_ISSUES.KEY_ACTION, carIssue.getIssueDetail().getAction());

        db.insert(TABLES.CAR_ISSUES.TABLE_NAME, null, values);
        db.close();
    }

    public void storeCarIssues(List<CarIssue> carIssues) {
        Log.i("CarIssues", "storing car issues");
        for(CarIssue carIssue : carIssues) {
            storeCarIssue(carIssue);
        }
    }

    public List<CarIssue> getAllDtcs(String carId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        List<CarIssue> carIssues = new ArrayList<>();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,
                "carId=? and issueType=?", new String[]{carId, CarIssue.DTC}, null, null, null);
        if(c.moveToFirst()) {
            carIssues.add(createCarIssue(c));
        }

        db.close();
        return carIssues;
    }

    public List<CarIssue> getAllRecalls(String carId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        List<CarIssue> carIssues = new ArrayList<>();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,
                "carId=? and issueType=?", new String[]{carId, CarIssue.RECALL}, null, null, null);
        if(c.moveToFirst()) {
            carIssues.add(createCarIssue(c));
        }

        db.close();
        return carIssues;
    }

    public List<CarIssue> getAllServices(String carId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        List<CarIssue> carIssues = new ArrayList<>();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,
                "carId=? and issueType!=? and issueType!=?",
                new String[]{carId, CarIssue.RECALL, CarIssue.DTC}, null, null, null);
        if(c.moveToFirst()) {
            carIssues.add(createCarIssue(c));
        }

        db.close();
        return carIssues;
    }

    public List<CarIssue> getAllCarIssues(String carId) {
        Log.i("CarIssues","CarId: "+carId);
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        List<CarIssue> carIssues = new ArrayList<>();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,
                "carId=?",new String[]{carId},null,null,null);
        if(c.moveToFirst()) {
            carIssues.add(createCarIssue(c));
        }

        db.close();
        return carIssues;
    }

    private List<CarIssue> getAllCarIssues() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        List<CarIssue> carIssues = new ArrayList<>();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,null,null,null,null,null);
        if(c.moveToFirst()) {
            carIssues.add(createCarIssue(c));
        }

        db.close();
        return carIssues;
    }

    private CarIssue createCarIssue(Cursor c) {
        CarIssue carIssue = new CarIssue();
        carIssue.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_ID)));
        carIssue.setParseId(c.getString(c.getColumnIndex(TABLES.COMMON.KEY_PARSE_ID)));

        carIssue.setCarId(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_CAR_ID)));
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

    public void deleteAllCarIssues() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        List<CarIssue> carIssueEntries = getAllCarIssues();

        for(CarIssue issue : carIssueEntries) {
            db.delete(TABLES.CAR.TABLE_NAME, TABLES.COMMON.KEY_ID + " = ? ",
                    new String[] { String.valueOf(issue.getId()) });
        }

        db.close();
    }
}


