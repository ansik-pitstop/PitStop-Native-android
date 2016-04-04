package com.pitstop.DataAccessLayer.DataRetrievers;

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
public class CarDataRetriever extends LocalDatabaseHelper {

    // CAR table create statement
    private static final String CREATE_TABLE_CAR = "CREATE TABLE "
            + TABLES.CAR.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.CAR.KEY_VIN + " TEXT, "
            + TABLES.CAR.KEY_MILEAGE + " INTEGER, "
            + TABLES.CAR.KEY_ENGINE + " TEXT, "
            + TABLES.CAR.KEY_DEALERSHIP_ID + " TEXT, "
            + TABLES.CAR.KEY_TRIM + " TEXT, "
            + TABLES.CAR.KEY_MAKE + " TEXT, "
            + TABLES.CAR.KEY_MODEL+ " TEXT, "
            + TABLES.CAR.KEY_YEAR + " INTEGER, "
            + TABLES.CAR.KEY_OWNER_ID+ " TEXT, "
            + TABLES.CAR.KEY_SCANNER_ID + " TEXT, "
            + TABLES.CAR.KEY_NUM_SERVICES + " INTEGER, "
            + TABLES.CAR.KEY_IS_DASHBOARD_CAR + " INTEGER, "
            + TABLES.COMMON.KEY_PARSE_ID + " TEXT, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    public CarDataRetriever(Context context) {
        super(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_CAR);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR.TABLE_NAME);
        onCreate(db);
        //super.onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Store car data
     */
    public long storeCarData(Car car) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_PARSE_ID, car.getParseId());
        values.put(TABLES.CAR.KEY_MAKE, car.getMake());
        values.put(TABLES.CAR.KEY_MODEL, car.getModel());
        values.put(TABLES.CAR.KEY_YEAR, car.getYear());
        values.put(TABLES.CAR.KEY_MILEAGE, car.getTotalMileage());
        values.put(TABLES.CAR.KEY_TRIM, car.getTrim());
        values.put(TABLES.CAR.KEY_ENGINE, car.getEngine());
        values.put(TABLES.CAR.KEY_VIN, car.getVin());
        values.put(TABLES.CAR.KEY_SCANNER_ID, car.getScanner());
        values.put(TABLES.CAR.KEY_OWNER_ID, car.getOwnerId());
        values.put(TABLES.CAR.KEY_DEALERSHIP_ID, car.getShopId());
        values.put(TABLES.CAR.KEY_NUM_SERVICES, car.getNumberOfServices());
        values.put(TABLES.CAR.KEY_IS_DASHBOARD_CAR, car.isCurrentCar() ? 1 : 0);

        return db.insert(TABLES.CAR.TABLE_NAME,null, values);
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

        String selectQuery = "SELECT * FROM " + TABLES.CAR.TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery,null);

        if(c.moveToFirst()) {
            Car car = new Car();
            car.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_ID)));
            car.setParseId(c.getString(c.getColumnIndex(TABLES.COMMON.KEY_PARSE_ID)));

            car.setMake(c.getString(c.getColumnIndex(TABLES.CAR.KEY_MAKE)));
            car.setModel(c.getString(c.getColumnIndex(TABLES.CAR.KEY_MODEL)));
            car.setYear(c.getInt(c.getColumnIndex(TABLES.CAR.KEY_YEAR)));
            car.setTotalMileage(c.getInt(c.getColumnIndex(TABLES.CAR.KEY_MILEAGE)));
            car.setTrim(c.getString(c.getColumnIndex(TABLES.CAR.KEY_TRIM)));
            car.setEngine(c.getString(c.getColumnIndex(TABLES.CAR.KEY_ENGINE)));
            car.setVin(c.getString(c.getColumnIndex(TABLES.CAR.KEY_VIN)));
            car.setScanner(c.getString(c.getColumnIndex(TABLES.CAR.KEY_SCANNER_ID)));
            car.setOwnerId(c.getString(c.getColumnIndex(TABLES.CAR.KEY_OWNER_ID)));
            car.setShopId(c.getString(c.getColumnIndex(TABLES.CAR.KEY_DEALERSHIP_ID)));
            car.setNumberOfServices(c.getInt(c.getColumnIndex(TABLES.CAR.KEY_NUM_SERVICES)));
            car.setCurrentCar(c.getInt(c.getColumnIndex(TABLES.CAR.KEY_IS_DASHBOARD_CAR)) == 1);

            cars.add(car);
        }
        return cars;
    }

    /**
     * Update car
     */
    public int updateCar(Car car) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_PARSE_ID, car.getParseId());
        values.put(TABLES.CAR.KEY_MAKE, car.getMake());
        values.put(TABLES.CAR.KEY_MODEL, car.getModel());
        values.put(TABLES.CAR.KEY_YEAR, car.getYear());
        values.put(TABLES.CAR.KEY_MILEAGE, car.getTotalMileage());
        values.put(TABLES.CAR.KEY_TRIM, car.getTrim());
        values.put(TABLES.CAR.KEY_ENGINE, car.getEngine());
        values.put(TABLES.CAR.KEY_VIN, car.getVin());
        values.put(TABLES.CAR.KEY_SCANNER_ID, car.getScanner());
        values.put(TABLES.CAR.KEY_OWNER_ID, car.getOwnerId());
        values.put(TABLES.CAR.KEY_DEALERSHIP_ID, car.getShopId());
        values.put(TABLES.CAR.KEY_NUM_SERVICES, car.getNumberOfServices());
        values.put(TABLES.CAR.KEY_IS_DASHBOARD_CAR, car.isCurrentCar() ? 1 : 0);

        return db.update(TABLES.CAR.TABLE_NAME,values, TABLES.COMMON.KEY_PARSE_ID + " = ?",
                new String[] { String.valueOf(car.getParseId()) });
    }

    /** Delete all cars*/
    public void deleteAllCars() {
        SQLiteDatabase db = this.getWritableDatabase();

        List<Car> carEntries = getAllCars();

        for(Car car : carEntries) {
            db.delete(TABLES.CAR.TABLE_NAME, TABLES.COMMON.KEY_ID + " = ? ",
                    new String[] { String.valueOf(car.getId()) });
        }
    }
}
