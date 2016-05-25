package com.pitstop.DataAccessLayer.DataAdapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/31/2016.
 */
public class LocalCarAdapter {

    // CAR table create statement
    public static final String CREATE_TABLE_CAR = "CREATE TABLE IF NOT EXISTS "
            + TABLES.CAR.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.CAR.KEY_VIN + " TEXT, "
            + TABLES.CAR.KEY_MILEAGE + " INTEGER, "
            + TABLES.CAR.KEY_ENGINE + " TEXT, "
            + TABLES.CAR.KEY_SHOP_ID + " TEXT, "
            + TABLES.CAR.KEY_TRIM + " TEXT, "
            + TABLES.CAR.KEY_MAKE + " TEXT, "
            + TABLES.CAR.KEY_MODEL+ " TEXT, "
            + TABLES.CAR.KEY_YEAR + " INTEGER, "
            + TABLES.CAR.KEY_USER_ID + " TEXT, "
            + TABLES.CAR.KEY_SCANNER_ID + " TEXT, "
            + TABLES.CAR.KEY_NUM_SERVICES + " INTEGER, "
            + TABLES.CAR.KEY_IS_DASHBOARD_CAR + " INTEGER, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalCarAdapter(Context context) {
        databaseHelper = new LocalDatabaseHelper(context);
    }

    /**
     * Store car data
     */
    public void storeCarData(Car car) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = carObjectToContentValues(car);

        long result = db.insert(TABLES.CAR.TABLE_NAME, null, values);

        db.close();
    }

    public void storeCars(List<Car> carList) {
        for(Car car : carList) {
            storeCarData(car);
        }
    }

    /**
     * Get all cars
     */
    public List<Car> getAllCars() {
        List<Car> cars = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.CAR.TABLE_NAME, null,null,null,null,null,null);

        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                cars.add(cursorToCar(c));
                c.moveToNext();
            }
        }
        db.close();
        return cars;
    }

    /**
     * Get car by parse id
     */

    public Car getCar(String parseId) {

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR.TABLE_NAME,null,
                TABLES.COMMON.KEY_OBJECT_ID +"=?", new String[] {parseId},null,null,null);
        Car car = null;
        if(c.moveToFirst()) {
            car = cursorToCar(c);
        }

        db.close();
        return car;
    }

    /**
     * Get car by id
     */

    public Car getCar(int carId) {

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.CAR.TABLE_NAME,null,
                TABLES.COMMON.KEY_OBJECT_ID +"=?", new String[] {String.valueOf(carId)},null,null,null);
        Car car = null;
        if(c.moveToFirst()) {
            car = cursorToCar(c);
        }

        db.close();
        return car;
    }

    /**
     * Update car
     */
    public int updateCar(Car car) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = carObjectToContentValues(car);

        int rows = db.update(TABLES.CAR.TABLE_NAME,values, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                new String[] { car.getParseId() });

        db.close();

        return rows;
    }

    /** Delete all cars*/
    public void deleteAllCars() {
        List<Car> carEntries = getAllCars();

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        for(Car car : carEntries) {
            db.delete(TABLES.CAR.TABLE_NAME, TABLES.COMMON.KEY_ID +"=?",
                    new String[] { String.valueOf(car.getId()) });
        }

        db.close();
    }

    private Car cursorToCar(Cursor c) {
        Car car = new Car();
        car.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)));

        car.setMake(c.getString(c.getColumnIndex(TABLES.CAR.KEY_MAKE)));
        car.setModel(c.getString(c.getColumnIndex(TABLES.CAR.KEY_MODEL)));
        car.setYear(c.getInt(c.getColumnIndex(TABLES.CAR.KEY_YEAR)));
        car.setTotalMileage(c.getInt(c.getColumnIndex(TABLES.CAR.KEY_MILEAGE)));
        car.setTrim(c.getString(c.getColumnIndex(TABLES.CAR.KEY_TRIM)));
        car.setEngine(c.getString(c.getColumnIndex(TABLES.CAR.KEY_ENGINE)));
        car.setVin(c.getString(c.getColumnIndex(TABLES.CAR.KEY_VIN)));
        car.setScanner(c.getString(c.getColumnIndex(TABLES.CAR.KEY_SCANNER_ID)));
        car.setOwnerId(c.getString(c.getColumnIndex(TABLES.CAR.KEY_USER_ID)));
        car.setShopId(c.getInt(c.getColumnIndex(TABLES.CAR.KEY_SHOP_ID)));
        car.setNumberOfServices(c.getInt(c.getColumnIndex(TABLES.CAR.KEY_NUM_SERVICES)));
        car.setCurrentCar(c.getInt(c.getColumnIndex(TABLES.CAR.KEY_IS_DASHBOARD_CAR)) == 1);
        return car;
    }


    private ContentValues carObjectToContentValues(Car car) {
        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_OBJECT_ID, car.getId());
        values.put(TABLES.CAR.KEY_MAKE, car.getMake());
        values.put(TABLES.CAR.KEY_MODEL, car.getModel());
        values.put(TABLES.CAR.KEY_YEAR, car.getYear());
        values.put(TABLES.CAR.KEY_MILEAGE, car.getTotalMileage());
        values.put(TABLES.CAR.KEY_TRIM, car.getTrim());
        values.put(TABLES.CAR.KEY_ENGINE, car.getEngine());
        values.put(TABLES.CAR.KEY_VIN, car.getVin());
        values.put(TABLES.CAR.KEY_SCANNER_ID, car.getScanner());
        values.put(TABLES.CAR.KEY_USER_ID, car.getOwnerId());
        values.put(TABLES.CAR.KEY_SHOP_ID, car.getShopId());
        values.put(TABLES.CAR.KEY_NUM_SERVICES, car.getNumberOfServices());
        values.put(TABLES.CAR.KEY_IS_DASHBOARD_CAR, car.isCurrentCar() ? 1 : 0);

        return values;
    }
}
