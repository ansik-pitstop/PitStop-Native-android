package com.pitstop.database

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.pitstop.models.issue.CarIssue
import com.pitstop.models.issue.IssueDetail
import com.pitstop.models.issue.UpcomingIssue
import java.util.*

/**
 * Created by Paul Soladoye on 04/04/2016.
 */
class LocalCarIssueStorage(private val databaseHelper: LocalDatabaseHelper) {

    private val TAG = LocalCarIssueStorage::class.java.simpleName

    companion object {
        // CAR_ISSUES table create statement
        val CREATE_TABLE_CAR_ISSUES = ("CREATE TABLE "
                + TABLES.CAR_ISSUES.TABLE_NAME + "("
                + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
                + TABLES.CAR_ISSUES.KEY_CAR_ID + " INTEGER, "
                + TABLES.CAR_ISSUES.KEY_STATUS + " TEXT, "
                + TABLES.CAR_ISSUES.KEY_TIMESTAMP + " TEXT, "
                + TABLES.CAR_ISSUES.KEY_ISSUE_TYPE + " TEXT, "
                + TABLES.CAR_ISSUES.KEY_PRIORITY + " INTEGER, "
                + TABLES.CAR_ISSUES.KEY_ITEM + " TEXT, "
                + TABLES.CAR_ISSUES.KEY_DESCRIPTION + " TEXT, "
                + TABLES.CAR_ISSUES.KEY_ACTION + " TEXT, "
                + TABLES.CAR_ISSUES.KEY_SYMPTOMS + " TEXT, "
                + TABLES.CAR_ISSUES.KEY_CAUSES + " TEXT, "
                + TABLES.CAR_ISSUES.KEY_UPCOMING_ISSUE_MILEAGE + " TEXT, "
                + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER"
                + ")")
    }

    fun getAllUpcomingCarIssues(carId: Int): List<UpcomingIssue>{
        Log.d(TAG,"getAllUpcomingCarIssues()")
        val carIssues = arrayListOf<UpcomingIssue>()

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null
                , TABLES.CAR_ISSUES.KEY_STATUS + "=? AND "+TABLES.CAR_ISSUES.KEY_CAR_ID + "=?"
                , arrayOf(CarIssue.ISSUE_PENDING,carId.toString()), null, null, null)
        Log.d(TAG,"getAllUpcomingCarIssues() count=${c.count}")
        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                carIssues.add(cursorToUpcomingCarIssue(c))
                c.moveToNext()
            }
        }
        c.close()
        return carIssues
    }

    fun getAllDoneCarIssues(carId: Int): List<CarIssue>{
        Log.d(TAG,"getAllDoneCarIssues()")
        val carIssues = arrayListOf<CarIssue>()

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null
                , TABLES.CAR_ISSUES.KEY_STATUS + "=? AND "+TABLES.CAR_ISSUES.KEY_CAR_ID + "=?"
                , arrayOf(CarIssue.ISSUE_DONE,carId.toString()), null, null, null)
        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                carIssues.add(cursorToCarIssue(c))
                c.moveToNext()
            }
        }

        c.close()
        return carIssues
    }

    fun getAllCurrentCarIssues(carId: Int): List<CarIssue>{
        Log.d(TAG,"getAllCurrentCarIssues()")
        val carIssues = ArrayList<CarIssue>()

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null
                , TABLES.CAR_ISSUES.KEY_STATUS + "=? AND "+TABLES.CAR_ISSUES.KEY_CAR_ID + "=?"
                , arrayOf(CarIssue.ISSUE_NEW,carId.toString()), null, null, null)
        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                carIssues.add(cursorToCarIssue(c))
                c.moveToNext()
            }
        }

        c.close()
        return carIssues
    }

    fun getAllCarIssues(): List<CarIssue>{
        Log.d(TAG,"getAllCarIssues()")
        val carIssues = ArrayList<CarIssue>()

        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null
                , null, null, null, null, null)
        if (c.moveToFirst()) {
            while (!c.isAfterLast) {
                carIssues.add(cursorToCarIssue(c))
                c.moveToNext()
            }
        }

        c.close()
        return carIssues
    }

    fun storeCarIssue(carIssue: CarIssue): Boolean {
        Log.d(TAG,"storeCarIssue()")
        val db = databaseHelper.writableDatabase

        val values = carIssueObjectToContentValues(carIssue)

        return db.insert(TABLES.CAR_ISSUES.TABLE_NAME, null, values) > 0L

    }

    fun replaceDoneIssues(carId: Int, issueList: List<CarIssue>): Int {
        Log.d(TAG,"storeIssues()")
        val db = databaseHelper.writableDatabase

        db.beginTransaction()
        var rows = 0
        try{
            db.delete(TABLES.CAR_ISSUES.TABLE_NAME
                    , TABLES.CAR_ISSUES.KEY_STATUS
                    + "=?  AND "+TABLES.CAR_ISSUES.KEY_CAR_ID+"=?",
                    arrayOf(CarIssue.ISSUE_DONE,carId.toString()))
            issueList.forEach({
                it.carId = carId
                val values = carIssueObjectToContentValues(it)
                if (db.insert(TABLES.CAR_ISSUES.TABLE_NAME, null, values) > 0L)
                    rows += 1
            })
            db.setTransactionSuccessful()
        }finally{
            db.endTransaction()
        }

        return rows
    }

    fun replaceCurrentIssues(carId: Int, issueList: List<CarIssue>): Int {
        Log.d(TAG,"storeIssues()")
        val db = databaseHelper.writableDatabase

        db.beginTransaction()
        var rows = 0
        try{
            db.delete(TABLES.CAR_ISSUES.TABLE_NAME, TABLES.CAR_ISSUES.KEY_STATUS
                    + "=?  AND "+TABLES.CAR_ISSUES.KEY_CAR_ID+"=?",
                    arrayOf(CarIssue.ISSUE_NEW,carId.toString()))
            issueList.forEach({
                it.carId = carId
                val values = carIssueObjectToContentValues(it)
                if (db.insert(TABLES.CAR_ISSUES.TABLE_NAME, null, values) > 0L)
                    rows += 1
            })
            db.setTransactionSuccessful()
        }finally{
            db.endTransaction()
        }

        return rows
    }

    fun replaceUpcomingIssues(carId:Int, issueList: List<UpcomingIssue>): Int {
        Log.d(TAG,"replaceUpcomingIssues()")
        val db = databaseHelper.writableDatabase

        db.beginTransaction()
        var rows = 0
        try{
            val replacedRows = db.delete(TABLES.CAR_ISSUES.TABLE_NAME
                    , TABLES.CAR_ISSUES.KEY_STATUS
                    + "=? AND "+TABLES.CAR_ISSUES.KEY_CAR_ID+"=?",
                    arrayOf(CarIssue.ISSUE_PENDING,carId.toString()))
            Log.d(TAG, "replacedRows = $replacedRows")
            issueList.forEach({
                it.carId = carId
                val values = upcomingCarIssueObjectToContentValues(it)
                if (db.insert(TABLES.CAR_ISSUES.TABLE_NAME, null, values) > 0L)
                    rows += 1
            })
            db.setTransactionSuccessful()
        }finally{
            db.endTransaction()
        }

        return rows
    }

    fun markIssueDone(id: Int, doneAt: String): Int {
        Log.d(TAG,"markDone()")
        val db = databaseHelper.writableDatabase

        val values = ContentValues()
        values.put(TABLES.CAR_ISSUES.KEY_STATUS,CarIssue.ISSUE_DONE)
        values.put(TABLES.CAR_ISSUES.KEY_TIMESTAMP, doneAt)
        return db.update(TABLES.CAR_ISSUES.TABLE_NAME, values,
                TABLES.COMMON.KEY_OBJECT_ID + "=?",
                arrayOf(id.toString()))
    }

    fun getCarIssue(issueId: Int): CarIssue? {
        Log.d(TAG,"getCarIssue()")

        var carIssue: CarIssue? = null
        val db = databaseHelper.readableDatabase

        val c = db.query(TABLES.CAR_ISSUES.TABLE_NAME, null,
                TABLES.COMMON.KEY_ID + "=?", arrayOf(issueId.toString())
                , null, null, null)

        if (c.moveToFirst()) {
            carIssue = cursorToCarIssue(c)
        }

        c.close()
        return carIssue
    }

    fun deleteAllUpcomingCarIssues(): Int {
        Log.d(TAG,"deleteAllUpcomingCarIssues()")
        val db = databaseHelper.writableDatabase

        return db.delete(TABLES.CAR_ISSUES.TABLE_NAME, TABLES.CAR_ISSUES.KEY_STATUS + "=?",
                arrayOf(CarIssue.ISSUE_PENDING))
    }

    fun deleteAllDoneCarIssues(): Int {
        Log.d(TAG,"deleteAllDoneCarIssues()")
        val db = databaseHelper.writableDatabase

        return db.delete(TABLES.CAR_ISSUES.TABLE_NAME, TABLES.CAR_ISSUES.KEY_STATUS + "=?",
                arrayOf(CarIssue.ISSUE_DONE))
    }

    fun deleteAllCurrentCarIssues(): Int {
        Log.d(TAG, "deleteAllDoneCarIssues()")
        val db = databaseHelper.writableDatabase

        return db.delete(TABLES.CAR_ISSUES.TABLE_NAME, TABLES.CAR_ISSUES.KEY_STATUS + "=?",
                arrayOf(CarIssue.ISSUE_NEW))
    }

    fun deleteCarIssue(issue: CarIssue): Int {
        Log.d(TAG,"deleteCarIssue()")
        val db = databaseHelper.writableDatabase

        return db.delete(TABLES.CAR_ISSUES.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                arrayOf(issue.id.toString()))
    }

    fun deleteAllCarIssues(carId: Int): Int {
        Log.d(TAG,"deleteAllCarIssues()")
        val db = databaseHelper.writableDatabase
        return db.delete(TABLES.CAR_ISSUES.TABLE_NAME
                , TABLES.CAR_ISSUES.KEY_CAR_ID+"=?", arrayOf(carId.toString()))
    }

    fun deleteAllCarIssues(): Int {
        Log.d(TAG,"deleteAllCarIssues()")
        val db = databaseHelper.writableDatabase
        return db.delete(TABLES.CAR_ISSUES.TABLE_NAME, null, null)
    }

    fun deleteAllRows() {
        Log.d(TAG,"deleteAllRows()")
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.CAR_ISSUES.TABLE_NAME, null, null)
    }

    private fun cursorToCarIssue(c: Cursor): CarIssue {
        val carIssue = CarIssue()
        carIssue.id = c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID))

        carIssue.carId = c.getInt(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_CAR_ID))
        carIssue.status = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_STATUS))
        carIssue.issueType = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ISSUE_TYPE))
        carIssue.priority = c.getInt(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_PRIORITY))
        carIssue.doneAt = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_TIMESTAMP))

        carIssue.issueDetail = IssueDetail(
                item = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ITEM)),
                description = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_DESCRIPTION)),
                action = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ACTION))
        )
        carIssue.symptoms = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_SYMPTOMS))
        carIssue.causes = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_CAUSES))

        return carIssue
    }

    private fun cursorToUpcomingCarIssue(c: Cursor): UpcomingIssue {

        val issueDetails = IssueDetail(
                item = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ITEM)),
                description = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_DESCRIPTION)),
                action = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_ACTION))
        )

        val upcomingIssue = UpcomingIssue(
                id = c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)),
                carId = c.getInt(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_CAR_ID)),
                priority = c.getInt(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_PRIORITY)),
                intervalMileage = c.getString(c.getColumnIndex(TABLES.CAR_ISSUES.KEY_UPCOMING_ISSUE_MILEAGE)),
                issueDetail = issueDetails
        )
        return upcomingIssue
    }

    private fun carIssueObjectToContentValues(carIssue: CarIssue): ContentValues {
        val values = ContentValues()
        values.put(TABLES.COMMON.KEY_OBJECT_ID, carIssue.id)
        values.put(TABLES.CAR_ISSUES.KEY_CAR_ID, carIssue.carId)
        values.put(TABLES.CAR_ISSUES.KEY_STATUS, carIssue.status)
        values.put(TABLES.CAR_ISSUES.KEY_ISSUE_TYPE, carIssue.issueType)
        values.put(TABLES.CAR_ISSUES.KEY_PRIORITY, carIssue.priority)
        values.put(TABLES.CAR_ISSUES.KEY_TIMESTAMP, carIssue.doneAt)

        values.put(TABLES.CAR_ISSUES.KEY_ITEM, carIssue.item)
        values.put(TABLES.CAR_ISSUES.KEY_DESCRIPTION, carIssue.description)
        values.put(TABLES.CAR_ISSUES.KEY_ACTION, carIssue.action)
        values.put(TABLES.CAR_ISSUES.KEY_SYMPTOMS, carIssue.symptoms)
        values.put(TABLES.CAR_ISSUES.KEY_CAUSES, carIssue.causes)

        return values
    }

    private fun upcomingCarIssueObjectToContentValues(upcomingIssue: UpcomingIssue): ContentValues{
        val values = ContentValues()
        values.put(TABLES.COMMON.KEY_OBJECT_ID, upcomingIssue.id)
        values.put(TABLES.CAR_ISSUES.KEY_CAR_ID, upcomingIssue.carId)
        values.put(TABLES.CAR_ISSUES.KEY_ITEM, upcomingIssue.issueDetail.item)
        values.put(TABLES.CAR_ISSUES.KEY_DESCRIPTION, upcomingIssue.issueDetail.description)
        values.put(TABLES.CAR_ISSUES.KEY_ACTION, upcomingIssue.issueDetail.action)
        values.put(TABLES.CAR_ISSUES.KEY_UPCOMING_ISSUE_MILEAGE,upcomingIssue.intervalMileage)
        values.put(TABLES.CAR_ISSUES.KEY_PRIORITY, upcomingIssue.priority)
        values.put(TABLES.CAR_ISSUES.KEY_STATUS, CarIssue.ISSUE_PENDING)
        return values
    }
}


