package com.pitstop.DataAccessLayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.DTOs.Pid;
import com.pitstop.DataAccessLayer.DataRetrievers.CarDataRetriever;
import com.pitstop.DataAccessLayer.DataRetrievers.CarIssuesDataRetriever;
import com.pitstop.DataAccessLayer.DataRetrievers.DealershipDataRetriever;
import com.pitstop.DataAccessLayer.DataRetrievers.PidDataRetriever;
import com.pitstop.DataAccessLayer.DataRetrievers.TABLES;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/16/2016.
 */
public class LocalDatabaseHelper extends SQLiteOpenHelper {
    // Logcat tag
    private static final String LOG = "LocalDatabaseHelper";

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "PITSTOP_DB";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.i("LocalData", "onCreate");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("onCreate", "LocalDataHelper");
        db.execSQL(PidDataRetriever.CREATE_TABLE_PID_DATA);
        db.execSQL(CarDataRetriever.CREATE_TABLE_CAR);
        db.execSQL(CarIssuesDataRetriever.CREATE_TABLE_CAR_ISSUES);
        db.execSQL(DealershipDataRetriever.CREATE_TABLE_DEALERSHIP);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PID.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR_ISSUES.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.DEALERSHIP.TABLE_NAME);
        onCreate(db);
    }
}
