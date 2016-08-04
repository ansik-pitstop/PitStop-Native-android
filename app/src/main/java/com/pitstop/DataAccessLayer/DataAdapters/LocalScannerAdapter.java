package com.pitstop.DataAccessLayer.DataAdapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.DTOs.ObdScanner;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ben Wu on 2016-08-04.
 */
public class LocalScannerAdapter {
    public static final String CREATE_TABLE_CAR_ISSUES = "CREATE TABLE "
            + TABLES.SCANNER.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.SCANNER.KEY_CAR_ID + " INTEGER, "
            + TABLES.SCANNER.KEY_DEVICE_NAME + " TEXT, "
            + TABLES.SCANNER.KEY_SCANNER_ID + " TEXT, "
            + TABLES.SCANNER.KEY_DATANUM + " TEXT" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalScannerAdapter(Context context) {
        databaseHelper = new LocalDatabaseHelper(context);
    }

    private List<ObdScanner> getAllScanners() {
        List<ObdScanner> carIssues = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,null,null,null,null,null);
        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                carIssues.add(cursorToScanner(c));
                c.moveToNext();
            }
        }

        db.close();
        return carIssues;
    }

    private ObdScanner cursorToScanner(Cursor c) {
        ObdScanner scanner = new ObdScanner();

        scanner.setCarId(c.getInt(c.getColumnIndex(TABLES.SCANNER.KEY_CAR_ID)));
        scanner.setDatanum(c.getString(c.getColumnIndex(TABLES.SCANNER.KEY_DATANUM)));
        scanner.setDeviceName(c.getString(c.getColumnIndex(TABLES.SCANNER.KEY_DEVICE_NAME)));
        scanner.setScannerId(c.getString(c.getColumnIndex(TABLES.SCANNER.KEY_SCANNER_ID)));

        return scanner;
    }


    private ContentValues scannerObjectToContentValues(ObdScanner scanner) {
        ContentValues values = new ContentValues();

        values.put(TABLES.SCANNER.KEY_CAR_ID, scanner.getCarId());
        values.put(TABLES.SCANNER.KEY_DATANUM, scanner.getDatanum());
        values.put(TABLES.SCANNER.KEY_DEVICE_NAME, scanner.getDeviceName());
        values.put(TABLES.SCANNER.KEY_SCANNER_ID, scanner.getScannerId());

        return values;
    }
}
