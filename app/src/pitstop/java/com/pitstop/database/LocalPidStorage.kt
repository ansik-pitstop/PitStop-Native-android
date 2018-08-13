package com.pitstop.database

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.models.PidGraphDataPoint
import rx.Observable
import java.sql.Date

/**
 * Created by Karol ZDebel on 8/7/2018.
 */
class LocalPidStorage(private val databaseHelper: LocalDatabaseHelper) {

    private val TAG = javaClass.simpleName

    companion object {

        // PID_DATA table create statement
        val CREATE_TABLE_PID_DATA = ("CREATE TABLE IF NOT EXISTS "
                + TABLES.PID.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
                + TABLES.PID.KEY_TYPE +" TEXT,"
                + TABLES.PID.KEY_VALUE +" TEXT,"
                + TABLES.PID.KEY_RTC_TIME +" INTEGER,"
                + TABLES.COMMON.KEY_CREATED_AT + " TIMESTAMP" + ")")
    }

    fun store(pid: PidPackage): Int {
        val db = databaseHelper.briteDatabase

        val transaction = db.newTransaction()

        var storedCount = 0
        try{
            pid.pids.forEach({
                val values = ContentValues()
                values.put(TABLES.PID.KEY_TYPE, it.key)
                values.put(TABLES.PID.KEY_VALUE, it.value)
                values.put(TABLES.PID.KEY_RTC_TIME, pid.timestamp)
                values.put(TABLES.COMMON.KEY_CREATED_AT, System.currentTimeMillis())
                if (db.insert(TABLES.PID.TABLE_NAME, values) > 0)
                    storedCount = storedCount.inc()
            })
            transaction.markSuccessful()
        }finally{
            transaction.end()
        }

        return storedCount
    }

    fun getAllSync(after: Long): List<PidGraphDataPoint>{
        return cursorToPidGraphPointList(databaseHelper.readableDatabase.query(TABLES.PID.TABLE_NAME
                ,null,TABLES.COMMON.KEY_CREATED_AT+" > ?"
                , arrayOf(after.toString()),null,null,null))
    }

    fun getAll(after: Long): Observable<List<PidGraphDataPoint>>{
        Log.d(TAG,"after: ${Date(after)}")
        return databaseHelper.briteDatabase.createQuery(
                TABLES.PID.TABLE_NAME,"SELECT * FROM "
                +TABLES.PID.TABLE_NAME
                +" WHERE "+TABLES.COMMON.KEY_CREATED_AT+" > " + after
                +" ORDER BY "+TABLES.PID.KEY_RTC_TIME).map {
                    val data = it.run()
                    var pidGraphPointList : List<PidGraphDataPoint> = arrayListOf<PidGraphDataPoint>()
                    Log.d(TAG,"got rows: ${data?.count}")
                    if (data != null) {
                        pidGraphPointList = cursorToPidGraphPointList(data)
                    }
                pidGraphPointList
        }
    }

    fun cursorToPidGraphPointList(cursor: Cursor): List<PidGraphDataPoint>{
        val pidGraphPointList = arrayListOf<PidGraphDataPoint>()
        if (cursor.moveToFirst()){
            while(!cursor.isAfterLast){
                val timestamp = cursor.getLong(cursor.getColumnIndex(TABLES.PID.KEY_RTC_TIME))
                val type = cursor.getString(cursor.getColumnIndex(TABLES.PID.KEY_TYPE))
                val value = cursor.getString(cursor.getColumnIndex(TABLES.PID.KEY_VALUE))
                pidGraphPointList.add(PidGraphDataPoint(timestamp,type,Integer.valueOf(value,16)))
                cursor.moveToNext()
            }
        }
        cursor.close()
        return pidGraphPointList

    }

    fun deleteAllRows() {
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.PID.TABLE_NAME, null, null)
    }
}
