package com.pitstop.database

import android.content.ContentValues
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.squareup.sqlbrite.QueryObservable
import java.sql.Date

/**
 * Created by Paul Soladoye on 4/1/2016.
 */
class LocalPidStorage(private val databaseHelper: LocalDatabaseHelper) {

    private val TAG = javaClass.simpleName

    companion object {

        // PID_DATA table create statement
        val CREATE_TABLE_PID_DATA = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.PID.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
                + TABLES.PID.KEY_TYPE
                + TABLES.PID.KEY_VALUE
                + TABLES.PID.KEY_RTC_TIME
                + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")")
    }

    fun store(pid: PidPackage): Int {
        val db = databaseHelper.writableDatabase

        db.beginTransaction()

        var storedCount = 0
        try{
            pid.pids.forEach({
                val values = ContentValues()
                values.put(TABLES.PID.KEY_TYPE, it.key)
                values.put(TABLES.PID.KEY_VALUE, it.value)
                values.put(TABLES.PID.KEY_RTC_TIME, pid.timestamp)
                if (db.insert(TABLES.PID.TABLE_NAME, null, values) > 0)
                    storedCount = storedCount.inc()
            })
            db.setTransactionSuccessful()
        }finally{
            db.endTransaction()
        }

        return storedCount
    }

    fun getAll(after: Long): QueryObservable{
        return databaseHelper.briteDatabase.createQuery(TABLES.PID.TABLE_NAME,"SELECT * FROM "
                +TABLES.PID.TABLE_NAME+" WHERE "+TABLES.COMMON.KEY_CREATED_AT+" > "+ Date(after) +" ORDER BY "+TABLES.PID.KEY_RTC_TIME)
    }

    fun deleteAllRows() {
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.PID.TABLE_NAME, null, null)
    }
}
