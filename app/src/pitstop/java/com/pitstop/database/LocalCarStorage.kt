package com.pitstop.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.pitstop.models.Car
import java.util.*

/**
 * Created by Paul Soladoye on 3/31/2016.
 */
class LocalCarStorage(context: Context) {

    private val databaseHelper = LocalDatabaseHelper.getInstance(context)

    /**
     * Get all cars
     */
    val allCars: List<Car>
        get() {
            val cars = ArrayList<Car>()

            val db = databaseHelper.readableDatabase
            val c = db.query(TABLES.CAR.TABLE_NAME, null, null, null, null, null, null)

            if (c.moveToFirst()) {
                while (!c.isAfterLast) {
                    cars.add(cursorToCar(c))
                    c.moveToNext()
                }
            }
            c.close()
            return cars
        }

    /**
     * Get Dashboard Car
     */

    val dashboardCar: Car?
        get() {

            val db = databaseHelper.readableDatabase

            val c = db.query(TABLES.CAR.TABLE_NAME, null,
                    TABLES.CAR.KEY_IS_DASHBOARD_CAR + "=?", arrayOf("1"), null, null, null)
            var car: Car? = null
            if (c.moveToFirst()) {
                car = cursorToCar(c)
            }

            c.close()
            return car
        }

    /**
     * Store car data
     */
    fun storeCarData(car: Car) {
        val db = databaseHelper.writableDatabase

        val values = carObjectToContentValues(car)

        val result = db.insert(TABLES.CAR.TABLE_NAME, null, values)

    }

    fun storeCars(carList: List<Car>) {
        for (car in carList) {
            storeCarData(car)
        }
    }

    /**
     * Get car by parse id
     */

    fun getCar(parseId: String): Car? {

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR.TABLE_NAME, null,
                TABLES.COMMON.KEY_OBJECT_ID + "=?", arrayOf(parseId), null, null, null)
        var car: Car? = null
        if (c.moveToFirst()) {
            car = cursorToCar(c)
        }
        c.close()
        return car
    }

    /**
     * Get car by id
     */

    fun getCar(carId: Int): Car? {

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR.TABLE_NAME, null,
                TABLES.COMMON.KEY_OBJECT_ID + "=?", arrayOf(carId.toString()), null, null, null)
        var car: Car? = null
        if (c.moveToFirst()) {
            car = cursorToCar(c)
        }

        c.close()
        return car
    }

    fun getCarRetrofit(carId: Int): com.pitstop.retrofit.Car? {

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR.TABLE_NAME, null,
                TABLES.COMMON.KEY_OBJECT_ID + "=?", arrayOf(carId.toString()), null, null, null)
        var car: com.pitstop.retrofit.Car? = null
        if (c.moveToFirst()) {
            car = cursorToCarRetrofit(c)
        }

        c.close()
        return car
    }


    fun getCarsByUserId(userId: Int): List<Car> {
        val cars = ArrayList<Car>()
        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.CAR.TABLE_NAME, null, null, null, null, null, null)

        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                if (cursorToCar(c).userId == userId) {
                    cars.add(cursorToCar(c))
                }
                c.moveToNext()
            }
        }
        c.close()
        return cars

    }

    /**
     * Get car by scanner (assumes one car per scanner)
     */

    fun getCarByScanner(scannerId: String): Car? {

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR.TABLE_NAME, null,
                TABLES.CAR.KEY_SCANNER_ID + "=?", arrayOf(scannerId.toString()), null, null, null)
        var car: Car? = null
        if (c.moveToFirst()) {
            car = cursorToCar(c)
        }

        c.close()
        return car
    }

    /**
     * Update car
     */
    fun updateCar(car: Car): Int {

        val db = databaseHelper.writableDatabase

        val values = carObjectToContentValues(car)


        return db.update(TABLES.CAR.TABLE_NAME, values, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                arrayOf(car.id.toString()))
    }

    /** Delete all cars */
    fun deleteAllCars() {
        val db = databaseHelper.writableDatabase

        db.delete(TABLES.CAR.TABLE_NAME, null, null)

    }


    private fun cursorToCar(c: Cursor): Car {
        val car = Car()
        car.id = c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID))

        car.make = c.getString(c.getColumnIndex(TABLES.CAR.KEY_MAKE))
        car.model = c.getString(c.getColumnIndex(TABLES.CAR.KEY_MODEL))
        car.year = c.getInt(c.getColumnIndex(TABLES.CAR.KEY_YEAR))
        car.totalMileage = c.getDouble(c.getColumnIndex(TABLES.CAR.KEY_MILEAGE))
        car.displayedMileage = c.getDouble(c.getColumnIndex(TABLES.CAR.KEY_DISPLAYED_MILEAGE))
        car.trim = c.getString(c.getColumnIndex(TABLES.CAR.KEY_TRIM))
        car.engine = c.getString(c.getColumnIndex(TABLES.CAR.KEY_ENGINE))
        car.vin = c.getString(c.getColumnIndex(TABLES.CAR.KEY_VIN))
        car.scannerId = c.getString(c.getColumnIndex(TABLES.CAR.KEY_SCANNER_ID))
        car.userId = c.getInt(c.getColumnIndex(TABLES.CAR.KEY_USER_ID))
        car.shopId = c.getInt(c.getColumnIndex(TABLES.CAR.KEY_SHOP_ID))
        car.numberOfServices = c.getInt(c.getColumnIndex(TABLES.CAR.KEY_NUM_SERVICES))
        car.isCurrentCar = c.getInt(c.getColumnIndex(TABLES.CAR.KEY_IS_DASHBOARD_CAR)) == 1
        return car
    }

    private fun cursorToCarRetrofit(c: Cursor): com.pitstop.retrofit.Car =
        com.pitstop.retrofit.Car(
                _id = c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID))
                ,make = c.getString(c.getColumnIndex(TABLES.CAR.KEY_MAKE))
                ,model =c.getString(c.getColumnIndex(TABLES.CAR.KEY_MODEL))
                ,year =c.getInt(c.getColumnIndex(TABLES.CAR.KEY_YEAR))
                ,totalMileage = c.getDouble(c.getColumnIndex(TABLES.CAR.KEY_MILEAGE)).toInt()
                ,trim = c.getString(c.getColumnIndex(TABLES.CAR.KEY_TRIM))
                ,engine = c.getString(c.getColumnIndex(TABLES.CAR.KEY_ENGINE))
                ,vin = c.getString(c.getColumnIndex(TABLES.CAR.KEY_VIN))
                ,userId = c.getInt(c.getColumnIndex(TABLES.CAR.KEY_USER_ID))
                ,tankSize = c.getString(c.getColumnIndex(TABLES.CAR.KEY_TANK_SIZE))
                ,highwayMileage = c.getString(c.getColumnIndex(TABLES.CAR.KEY_HIGHWAY_MILEAGE))
                ,cityMileage = c.getString(c.getColumnIndex(TABLES.CAR.KEY_CITY_MILEAGE))
                ,baseMileage = c.getInt(c.getColumnIndex(TABLES.CAR.KEY_BASE_MILEAGE))
                ,salesperson = c.getString(c.getColumnIndex(TABLES.CAR.KEY_SALES_PERSON))
        )


    private fun carObjectToContentValues(car: Car): ContentValues {
        val values = ContentValues()
        values.put(TABLES.COMMON.KEY_OBJECT_ID, car.id)
        values.put(TABLES.CAR.KEY_MAKE, car.make)
        values.put(TABLES.CAR.KEY_MODEL, car.model)
        values.put(TABLES.CAR.KEY_YEAR, car.year)
        values.put(TABLES.CAR.KEY_MILEAGE, car.totalMileage)
        values.put(TABLES.CAR.KEY_DISPLAYED_MILEAGE, car.displayedMileage)
        values.put(TABLES.CAR.KEY_TRIM, car.trim)
        values.put(TABLES.CAR.KEY_ENGINE, car.engine)
        values.put(TABLES.CAR.KEY_VIN, car.vin)
        values.put(TABLES.CAR.KEY_SCANNER_ID, car.scannerId)
        values.put(TABLES.CAR.KEY_USER_ID, car.userId)
        values.put(TABLES.CAR.KEY_SHOP_ID, car.shopId)
        values.put(TABLES.CAR.KEY_NUM_SERVICES, car.numberOfServices)
        values.put(TABLES.CAR.KEY_IS_DASHBOARD_CAR, if (car.isCurrentCar) 1 else 0)

        return values
    }

    fun deleteAllRows() {
        val db = databaseHelper.writableDatabase

        db.delete(TABLES.CAR.TABLE_NAME, null, null)


    }

    fun deleteCar(carId: Int) {
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.CAR.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                arrayOf(carId.toString()))
    }

    companion object {

        // CAR table create statement
        val CREATE_TABLE_CAR = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.CAR.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
                + TABLES.CAR.KEY_VIN + " TEXT, "
                + TABLES.CAR.KEY_MILEAGE + " REAL, "
                + TABLES.CAR.KEY_DISPLAYED_MILEAGE + " REAL, "
                + TABLES.CAR.KEY_ENGINE + " TEXT, "
                + TABLES.CAR.KEY_SHOP_ID + " INTEGER, "
                + TABLES.CAR.KEY_TRIM + " TEXT, "
                + TABLES.CAR.KEY_MAKE + " TEXT, "
                + TABLES.CAR.KEY_MODEL + " TEXT, "
                + TABLES.CAR.KEY_YEAR + " INTEGER, "
                + TABLES.CAR.KEY_USER_ID + " INTEGER, "
                + TABLES.CAR.KEY_SCANNER_ID + " TEXT, "
                + TABLES.CAR.KEY_NUM_SERVICES + " INTEGER, "
                + TABLES.CAR.KEY_IS_DASHBOARD_CAR + " INTEGER, "
                + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
                + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")")
    }

}
