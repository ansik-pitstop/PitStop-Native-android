package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.models.ObdScanner;

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
        databaseHelper = LocalDatabaseHelper.getInstance(context);
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

        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                scanners.add(cursorToScanner(c));
                c.moveToNext();
            }
        }

        db.close();
        return scanners;
    }

    public boolean isCarExist(int carId) {
        boolean exist = false;
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                if (c.getInt(c.getColumnIndex(TABLES.SCANNER.KEY_CAR_ID)) == carId) {
                    exist = true;
                }
                c.moveToNext();
            }
        }
        return exist;
    }

    public int updateScanner(ObdScanner scanner) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = scannerObjectToContentValues(scanner);

        int rows = db.update(TABLES.SCANNER.TABLE_NAME, values, TABLES.SCANNER.KEY_SCANNER_ID + "=?",
                new String[]{String.valueOf(scanner.getScannerId())});

        db.close();

        return rows;
    }

    public int updateScannerByCarId(ObdScanner scanner) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = scannerObjectToContentValues(scanner);

        int rows = db.update(TABLES.SCANNER.TABLE_NAME, values, TABLES.SCANNER.KEY_CAR_ID + "=?",
                new String[]{String.valueOf(scanner.getCarId())});

        db.close();

        return rows;
    }

    public ObdScanner getScannerByName(String scannerName) {
        ObdScanner scanner = new ObdScanner();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null, TABLES.SCANNER.KEY_DEVICE_NAME + "=?",
                new String[]{scannerName}, null, null, null);

        if (c.moveToFirst()) {
            scanner = cursorToScanner(c);
        }

        db.close();
        return scanner;
    }

    public ObdScanner getScannerByScannerId(String scannerId) {
        ObdScanner scanner = new ObdScanner();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, TABLES.SCANNER.KEY_SCANNER_ID + "=?",
                new String[]{scannerId}, null, null, null);

        try {
            if (c.moveToFirst()) {
                scanner = cursorToScanner(c);
            }
        } finally {
            if (c != null) c.close();
            if (db.isOpen()) db.close();
        }
        if (db.isOpen()) db.close();

        return scanner;
    }

    public ObdScanner getScannerByCarId(int carId) {
        ObdScanner scanner = new ObdScanner();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, TABLES.SCANNER.KEY_CAR_ID + "=?",
                new String[]{String.valueOf(carId)}, null, null, null);

        try {
            if (c.moveToFirst()) {
                scanner = cursorToScanner(c);
            }
        } finally {
            if (c != null) c.close();
            if (db.isOpen()) db.close();
        }
        if (db.isOpen()) db.close();

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

    public boolean anyCarLackScanner() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        int numberOfScanners = 0;
        int numberOfCars = 0;
        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, null, null, null, null, null);
        try {
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    ObdScanner scanner = cursorToScanner(c);
                    if (scanner.getScannerId() != null && !"".equals(scanner.getScannerId())) {
                        numberOfScanners++;
                    }
                    numberOfCars++;
                    c.moveToNext();
                }
            }
        } finally {
            if (c != null) c.close();
            if (db.isOpen()) db.close();
        }
        if (db.isOpen()) db.close();
        return numberOfCars != numberOfScanners;
    }

    public boolean deviceNameExists(String deviceName) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, null, null, null, null, null);
        try {
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    String storedName = c.getString(c.getColumnIndex(TABLES.SCANNER.KEY_DEVICE_NAME));
                    if (storedName != null && storedName.equals(deviceName)) {
                        return true;
                    }
                    c.moveToNext();
                }
            }
        } finally {
            if (c != null) c.close();
            if (db.isOpen()) db.close();
        }
        if (db.isOpen()) db.close();
        return false;
    }

    public boolean scannerIdExists(String scannerId) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, null, null, null, null, null);
        try {
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    String storedID = c.getString(c.getColumnIndex(TABLES.SCANNER.KEY_SCANNER_ID));
                    if (storedID != null && storedID.equals(scannerId)) {
                        return true;
                    }
                    c.moveToNext();
                }
            }
        } finally {
            if (c != null) c.close();
            if (db.isOpen()) db.close();
        }
        if (db.isOpen()) db.close();
        return false;
    }

    public int getTableSize() {
        int size = 0;
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, null, null, null, null, null);
        try {
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    size++;
                    c.moveToNext();
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
            if (db.isOpen()) {
                db.close();
            }
        }
        if (db.isOpen()) db.close();
        return size;
    }

    public boolean carHasDevice(int pickedCarId) {
        boolean result = false;
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, new String[]{TABLES.SCANNER.KEY_CAR_ID, TABLES.SCANNER.KEY_SCANNER_ID},
                TABLES.SCANNER.KEY_CAR_ID + " = " + pickedCarId,
                null, null, null, null);
        try {
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    String scannerId = c.getString(c.getColumnIndex(TABLES.SCANNER.KEY_SCANNER_ID));
                    int carId = c.getInt(c.getColumnIndex(TABLES.SCANNER.KEY_CAR_ID));
                    if (scannerId != null && scannerId.length() > 0 && pickedCarId == carId) {
                        result = true;
                    }
                    c.moveToNext();
                }
            }
        } finally {
            if (c != null) {
                c.close();
            }
            if (db.isOpen()) {
                db.close();
            }
        }
        if (db.isOpen()) db.close();
        return result;
    }

    /**
     * Check if any car has scanner, the scanner has scanner Id, but does not have scanner name
     *
     * @return
     */
    public boolean anyScannerLackName() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        int numberOfScanners = 0;
        int numberOfDeviceNames = 0;
        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, null, null, null, null, null);
        try {
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    ObdScanner scanner = cursorToScanner(c);
                    if (scanner.getScannerId() != null && !"".equals(scanner.getScannerId())) {
                        numberOfScanners++;
                        if (scanner.getDeviceName() != null && !"".equals(scanner.getDeviceName())) {
                            numberOfDeviceNames++;
                        }
                    }
                    c.moveToNext();
                }
            }
        } finally {
            if (c != null) c.close();
            if (db.isOpen()) db.close();
        }
        if (db.isOpen()) db.close();
        return numberOfDeviceNames != numberOfScanners;
    }


    /**
     * @param scannerId
     * @param scannerName
     * @return true if scanner is found and its name is updated, false if scanner is not found
     */
    public boolean updateScannerName(String scannerId, String scannerName){
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.SCANNER.TABLE_NAME, null, null, null, null, null, null);
        try {
            if (c.moveToFirst()) {
                while (!c.isAfterLast()) {
                    String storedID = c.getString(c.getColumnIndex(TABLES.SCANNER.KEY_SCANNER_ID));
                    if (storedID != null && storedID.equals(scannerId)) {
                        ObdScanner scanner = cursorToScanner(c);
                        scanner.setDeviceName(scannerName);
                        updateScanner(scanner);
                        return true;
                    }
                    c.moveToNext();
                }
            }
        } finally {
            if (c != null) c.close();
            if (db.isOpen()) db.close();
        }
        if (db.isOpen()) db.close();
        return false;
    }

    public void deleteAllRows() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.SCANNER.TABLE_NAME, null, null);

        db.close();
    }
}
