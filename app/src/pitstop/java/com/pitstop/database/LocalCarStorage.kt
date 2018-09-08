package com.pitstop.database

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.pitstop.models.Car
import com.pitstop.models.PendingUpdate
import java.util.*

/**
 * Created by Paul Soladoye on 3/31/2016.
 */
class LocalCarStorage(private val databaseHelper: LocalDatabaseHelper) {

    private val TAG = javaClass.simpleName

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

        val CREATE_TABLE_PENDING_UPDATES = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.CAR_PENDING.TABLE_NAME
                + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TABLES.CAR_PENDING.KEY_CAR_ID + " INTEGER, "
                + TABLES.CAR_PENDING.KEY_TYPE + " TEXT, "
                + TABLES.CAR_PENDING.KEY_VALUE + " TEXT, "
                + TABLES.COMMON.KEY_TIMESTAMP + " TIMESTAMP, "
                + " FOREIGN KEY ("+TABLES.COMMON.KEY_ID+") REFERENCES "
                +TABLES.CAR_PENDING.TABLE_NAME+"("+TABLES.CAR_PENDING.KEY_CAR_ID+")" +")")
    }

    fun storePendingUpdate(pendingUpdate: PendingUpdate): Boolean{
        val db = databaseHelper.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(TABLES.CAR_PENDING.KEY_TYPE,pendingUpdate.type)
        contentValues.put(TABLES.CAR_PENDING.KEY_VALUE,pendingUpdate.value)
        contentValues.put(TABLES.CAR_PENDING.KEY_CAR_ID,pendingUpdate.id)
        contentValues.put(TABLES.COMMON.KEY_TIMESTAMP,pendingUpdate.timestamp)
        return db.insert(TABLES.CAR_PENDING.TABLE_NAME,null,contentValues) > 0
    }

    fun getPendingUpdates(): List<PendingUpdate>{
        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.CAR_PENDING.TABLE_NAME,null,null,null
                ,null,null,null)
        val pendingUpdates = arrayListOf<PendingUpdate>()
        if (c.moveToFirst()){
            while(!c.isAfterLast){
                val type = c.getString(c.getColumnIndex(TABLES.CAR_PENDING.KEY_TYPE))
                val value = c.getString(c.getColumnIndex(TABLES.CAR_PENDING.KEY_VALUE))
                val id = c.getInt(c.getColumnIndex(TABLES.CAR_PENDING.KEY_CAR_ID))
                val timestamp = c.getLong(c.getColumnIndex(TABLES.COMMON.KEY_TIMESTAMP))
                pendingUpdates.add(PendingUpdate(id,type,value,timestamp))
                c.moveToNext()
            }
        }
        c.close()
        return pendingUpdates
    }

    fun removeAllPendingUpdates(): Int{
        val db = databaseHelper.writableDatabase
        return db.delete(TABLES.CAR_PENDING.TABLE_NAME,null,null)
    }

    fun removePendingUpdate(pendingUpdate: PendingUpdate): Int{
        val db = databaseHelper.writableDatabase
        return db.delete(TABLES.CAR_PENDING.TABLE_NAME
                ,"${TABLES.COMMON.KEY_TIMESTAMP} = ?"
                , arrayOf(pendingUpdate.timestamp.toString()))
    }

    fun getAllCars(userId: Int): List<Car> {
        val cars = ArrayList<Car>()

        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.CAR.TABLE_NAME, null, null, null
                , null, null, null)

        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                cars.add(cursorToCar(c))
                c.moveToNext()
            }
        }
        c.close()
        Log.d(TAG, "getAllCars, cars: $cars")
        return cars
    }

    fun storeCarData(car: Car): Boolean {
        val db = databaseHelper.writableDatabase

        val values = carObjectToContentValues(car)

        db.insert(TABLES.CAR.TABLE_NAME, null, values)
        return true
    }

    fun deleteAndStoreCar(car: Car) {
        Log.d(TAG, "deleteAndStoreCar() car: $car")
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLES.CAR.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                    arrayOf(car.id.toString()))
            val values = carObjectToContentValues(car)
            db.insert(TABLES.CAR.TABLE_NAME, null, values)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

    }

    fun deleteAndStoreCars(carList: List<Car>) {
        Log.d(TAG, "deleteAndStoreCars() carList: $carList")
        val db = databaseHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLES.CAR.TABLE_NAME, null, null)
            for (c in carList) {
                val values = carObjectToContentValues(c)
                db.insert(TABLES.CAR.TABLE_NAME, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

    }

    fun storeCars(carList: List<Car>) {
        for (car in carList) {
            storeCarData(car)
        }
    }

    fun getCar(carId: Int): Car? {

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR.TABLE_NAME, null,
                TABLES.COMMON.KEY_OBJECT_ID + "=?", arrayOf(carId.toString()), null
                , null, null)
        var car: Car? = null
        if (c.moveToFirst()) {
            car = cursorToCar(c)
        }

        c.close()
        return car
    }


    fun getCarsByUserId(userId: Int): List<Car> {
        val cars = ArrayList<Car>()
        val db = databaseHelper.readableDatabase
        val c = db.query(TABLES.CAR.TABLE_NAME, null
                , TABLES.CAR.KEY_USER_ID +"=?", arrayOf(userId.toString())
                , null, null, null)

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

    fun updateCarMileage(carId: Int, mileage: Double): Int{
        val db = databaseHelper.writableDatabase

        val contentValues = ContentValues()
        contentValues.put(TABLES.CAR.KEY_MILEAGE, mileage)

        return db.update(TABLES.CAR.TABLE_NAME, contentValues, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                arrayOf(carId.toString()))
    }

    fun updateCar(car: Car): Int {

        val db = databaseHelper.writableDatabase

        val values = carObjectToContentValues(car)

        return db.update(TABLES.CAR.TABLE_NAME, values, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                arrayOf(car.id.toString()))
    }

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
        db.delete(TABLES.CAR_PENDING.TABLE_NAME,null,null)
        db.delete(TABLES.CAR.TABLE_NAME, null, null)
    }

    fun deleteCar(carId: Int) {
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.CAR.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                arrayOf(carId.toString()))
    }
}
