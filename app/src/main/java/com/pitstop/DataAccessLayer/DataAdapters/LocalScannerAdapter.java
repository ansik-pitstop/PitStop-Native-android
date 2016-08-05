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

    public void storeScanner(ObdScanner scanner) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = scannerObjectToContentValues(scanner);

        db.insert(TABLES.SCANNER.TABLE_NAME, null, values);

        db.close();
    }

    public List<ObdScanner> getAllScanners() {
        List<ObdScanner> scanners = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null,null,null,null,null,null);
        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                scanners.add(cursorToScanner(c));
                c.moveToNext();
            }
        }

        db.close();
        return scanners;
    }

    public int updateScanner(ObdScanner scanner) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = scannerObjectToContentValues(scanner);

        int rows = db.update(TABLES.SCANNER.TABLE_NAME, values, TABLES.SCANNER.KEY_SCANNER_ID + "=?",
                new String[] { String.valueOf(scanner.getScannerId()) });

        db.close();

        return rows;
    }

    public ObdScanner getScannerByName(String scannerName) {
        ObdScanner scanner = new ObdScanner();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null, TABLES.SCANNER.KEY_DEVICE_NAME+"=?",
                new String[] {scannerName}, null, null, null);

        if(c.moveToFirst()) {
            scanner = cursorToScanner(c);
        }

        db.close();
        return scanner;
    }

    public ObdScanner getScannerByScannerId(String scannerId) {
        ObdScanner scanner = new ObdScanner();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, TABLES.SCANNER.KEY_SCANNER_ID+"=?",
                new String[] {scannerId}, null, null, null);

        if(c.moveToFirst()) {
            scanner = cursorToScanner(c);
        }

        db.close();
        return scanner;
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
