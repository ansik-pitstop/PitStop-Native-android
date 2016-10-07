package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.models.CarIssuePreset;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yifan on 16/9/29.
 * @deprecated For the time being there's no need to use database to store available preset issues.
 */
public class LocalPresetIssueAdapter {

    // PRESET_ISSUE table create statement
    public static final String CREATE_TABLE_PRESET_ISSUES = "CREATE TABLE "
            + TABLES.PRESET_ISSUES.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.PRESET_ISSUES.KEY_CAR_ID + " INTEGER, "
            + TABLES.PRESET_ISSUES.KEY_TYPE + " TEXT, "
            + TABLES.PRESET_ISSUES.KEY_ITEM + " TEXT, "
            + TABLES.PRESET_ISSUES.KEY_ACTION+ " TEXT, "
            + TABLES.PRESET_ISSUES.KEY_DESCRIPTION + " TEXT, "
            + TABLES.PRESET_ISSUES.KEY_PRIORITY + " INTEGER, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalPresetIssueAdapter(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storePresetIssues(List<CarIssuePreset> issues, final int carId){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        for (CarIssuePreset issue: issues){
            storePresetIssue(issue, carId, db);
        }
        db.close();
    }

    private void storePresetIssue(CarIssuePreset issue, final int carId, SQLiteDatabase db){
        ContentValues values = presetIssueObjectToContentValues(issue, carId);
        db.insert(TABLES.PRESET_ISSUES.TABLE_NAME, null, values);
    }

    private ContentValues presetIssueObjectToContentValues(CarIssuePreset presetIssue, final int carId){
        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_OBJECT_ID, presetIssue.getId());
        values.put(TABLES.PRESET_ISSUES.KEY_CAR_ID, carId);
        values.put(TABLES.PRESET_ISSUES.KEY_TYPE, presetIssue.getType());
        values.put(TABLES.PRESET_ISSUES.KEY_ITEM, presetIssue.getItem());
        values.put(TABLES.PRESET_ISSUES.KEY_ACTION, presetIssue.getAction());
        values.put(TABLES.PRESET_ISSUES.KEY_DESCRIPTION, presetIssue.getDescription());
        values.put(TABLES.PRESET_ISSUES.KEY_PRIORITY, presetIssue.getPriority());

        return values;
    }

    public List<CarIssuePreset> getAllCarPresetIssues(final int carId){
        ArrayList<CarIssuePreset> presetIssues = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.PRESET_ISSUES.TABLE_NAME, null,
                TABLES.PRESET_ISSUES.KEY_CAR_ID+"=?", new String[]{String.valueOf(carId)}, null, null, null);

        if (c.moveToFirst()){
            while(!c.isAfterLast()){
                presetIssues.add(cursorToPresetIssue(c));
                c.moveToNext();
            }
        }

        db.close();
        return presetIssues;
    }

    private CarIssuePreset cursorToPresetIssue(Cursor c){
        return new CarIssuePreset.Builder(c.getString(c.getColumnIndex(TABLES.PRESET_ISSUES.KEY_TYPE)),
                c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)))
                .setItem(c.getString(c.getColumnIndex(TABLES.PRESET_ISSUES.KEY_ITEM)))
                .setAction(c.getString(c.getColumnIndex(TABLES.PRESET_ISSUES.KEY_ACTION)))
                .setDescription(c.getString(c.getColumnIndex(TABLES.PRESET_ISSUES.KEY_DESCRIPTION)))
                .setPriority(c.getInt(c.getColumnIndex(TABLES.PRESET_ISSUES.KEY_PRIORITY))).build();
    }

    public void deleteAllRows(){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.PRESET_ISSUES.TABLE_NAME, null, null);

        db.close();
    }


}
