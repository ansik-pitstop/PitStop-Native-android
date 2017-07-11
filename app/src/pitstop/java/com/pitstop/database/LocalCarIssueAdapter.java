package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 04/04/2016.
 */
public class LocalCarIssueAdapter {
    // CAR_ISSUES table create statement
    public static final String CREATE_TABLE_CAR_ISSUES = "CREATE TABLE "
            + TABLES.CAR_ISSUES.TABLE_NAME + "("
            + TABLES.COMMON.KEY_ID              + " INTEGER PRIMARY KEY,"
            + TABLES.CAR_ISSUES.KEY_CAR_ID      + " INTEGER, "
            + TABLES.CAR_ISSUES.KEY_STATUS      + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_TIMESTAMP   + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_ISSUE_TYPE  + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_PRIORITY    + " INTEGER, "
            + TABLES.CAR_ISSUES.KEY_ITEM        + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_DESCRIPTION + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_ACTION      + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_SYMPTOMS    + " TEXT, "
            + TABLES.CAR_ISSUES.KEY_CAUSES      + " TEXT, "
            + TABLES.COMMON.KEY_OBJECT_ID       + " INTEGER"
            + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalCarIssueAdapter(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storeCarIssue(CarIssue carIssue) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = carIssueObjectToContentValues(carIssue);

        db.insert(TABLES.CAR_ISSUES.TABLE_NAME, null, values);

    }

    public void storeCarIssues(List<Car> cars) {
        for(Car car : cars) {
            for(CarIssue issue : car.getIssues()) {
                storeCarIssue(issue);
            }
        }
    }

    public int updateCarIssue(CarIssue carIssue) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = carIssueObjectToContentValues(carIssue);

        int rows = db.update(TABLES.CAR_ISSUES.TABLE_NAME, values,
                TABLES.COMMON.KEY_OBJECT_ID+"=?",
                new String[] {String.valueOf(carIssue.getId())});

        return rows;
    }

    public CarIssue getCarIssue(int issueId){

        CarIssue carIssue = null;
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,
                TABLES.COMMON.KEY_ID+"=?",new String[]{String.valueOf(issueId)},null,null,null);

        if(c.moveToFirst()) {
            carIssue = cursorToCarIssue(c);
        }

        c.close();
        return carIssue;
    }

    public List<CarIssue> getAllUpcomingCarIssues() {
        List<CarIssue> carIssues = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null
                ,TABLES.CAR_ISSUES.KEY_ISSUE_TYPE+"=?",new String[]{CarIssue.ISSUE_NEW},null,null,null);
        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                carIssues.add(cursorToCarIssue(c));
                c.moveToNext();
            }
        }
        c.close();
        return carIssues;
    }

    public List<CarIssue> getAllDoneCarIssues() {
        List<CarIssue> carIssues = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null
                ,TABLES.CAR_ISSUES.KEY_ISSUE_TYPE+"=?",new String[]{CarIssue.ISSUE_DONE},null,null,null);
        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                carIssues.add(cursorToCarIssue(c));
                c.moveToNext();
            }
        }

        c.close();
        return carIssues;
    }

    public List<CarIssue> getAllCurrentCarIssues() {
        List<CarIssue> carIssues = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null
                ,TABLES.CAR_ISSUES.KEY_ISSUE_TYPE+"=?",new String[]{CarIssue.ISSUE_PENDING},null,null,null);
        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                carIssues.add(cursorToCarIssue(c));
                c.moveToNext();
            }
        }

        c.close();
        return carIssues;
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

        c.close();
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

        c.close();
        return carIssues;
    }


    public void deleteCarIssue(CarIssue issue) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.CAR_ISSUES.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                new String[] { String.valueOf(issue.getId()) });


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
        carIssue.setDoneAt(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_TIMESTAMP)));

        carIssue.setItem(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ITEM)));
        carIssue.setDescription(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_DESCRIPTION)));
        carIssue.setAction(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ACTION)));
        carIssue.setSymptoms(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_SYMPTOMS)));
        carIssue.setCauses(c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_CAUSES)));

        return carIssue;
    }


    private ContentValues carIssueObjectToContentValues(CarIssue carIssue) {
        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_OBJECT_ID, carIssue.getId());
        values.put(TABLES.CAR_ISSUES.KEY_CAR_ID, carIssue.getCarId());
        values.put(TABLES.CAR_ISSUES.KEY_STATUS, carIssue.getStatus());
        values.put(TABLES.CAR_ISSUES.KEY_ISSUE_TYPE, carIssue.getIssueType());
        values.put(TABLES.CAR_ISSUES.KEY_PRIORITY, carIssue.getPriority());
        values.put(TABLES.CAR_ISSUES.KEY_TIMESTAMP, carIssue.getDoneAt());

        values.put(TABLES.CAR_ISSUES.KEY_ITEM, carIssue.getItem());
        values.put(TABLES.CAR_ISSUES.KEY_DESCRIPTION, carIssue.getDescription());
        values.put(TABLES.CAR_ISSUES.KEY_ACTION, carIssue.getAction());
        values.put(TABLES.CAR_ISSUES.KEY_SYMPTOMS, carIssue.getSymptoms());
        values.put(TABLES.CAR_ISSUES.KEY_CAUSES, carIssue.getCauses());

        return values;
    }

    public void deleteAllRows(){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.CAR_ISSUES.TABLE_NAME, null, null);


    }

}


