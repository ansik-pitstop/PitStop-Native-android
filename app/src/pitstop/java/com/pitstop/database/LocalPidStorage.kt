package com.pitstop.database

import android.content.ContentValues
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
                + TABLES.PID.KEY_RTC_TIME +" LONG,"
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

    fun getAll(after: Long): Observable<List<PidGraphDataPoint>>{
        return databaseHelper.briteDatabase.createQuery(
                TABLES.PID.TABLE_NAME,"SELECT * FROM "
                +TABLES.PID.TABLE_NAME+" WHERE "+TABLES.COMMON.KEY_CREATED_AT+" > "
                + Date(after) +" ORDER BY "+TABLES.PID.KEY_RTC_TIME).map {
                val data = it.run()
                val pidGraphPointList = arrayListOf<PidGraphDataPoint>()
                if (data != null) {
                    if (data.moveToFirst()){
                        while(!data.isAfterLast){
                            val timestamp = data.getLong(data.getColumnIndex(TABLES.PID.KEY_RTC_TIME))
                            val type = data.getString(data.getColumnIndex(TABLES.PID.KEY_TYPE))
                            val value = data.getString(data.getColumnIndex(TABLES.PID.KEY_VALUE))
                            pidGraphPointList.add(PidGraphDataPoint(timestamp,type,Integer.valueOf(value,16)))
                        }
                    }
                }
            pidGraphPointList
        }
    }

    fun deleteAllRows() {
        val db = databaseHelper.writableDatabase
        db.delete(TABLES.PID.TABLE_NAME, null, null)
    }
}
