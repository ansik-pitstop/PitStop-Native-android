package com.pitstop.DataAccessLayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.pitstop.DataAccessLayer.DataAdapters.CarAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.CarIssueAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.DealershipAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.PidAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.TABLES;
import com.pitstop.MainActivity;

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
        Log.i(MainActivity.TAG, "LocalDataBaseHelper::constructor");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(MainActivity.TAG, "LocalDataHelper::onCreate");
        db.execSQL(PidAdapter.CREATE_TABLE_PID_DATA);
        db.execSQL(CarAdapter.CREATE_TABLE_CAR);
        db.execSQL(CarIssueAdapter.CREATE_TABLE_CAR_ISSUES);
        db.execSQL(DealershipAdapter.CREATE_TABLE_DEALERSHIP);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(MainActivity.TAG, "LocalDataHelper::onUpgrade");
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PID.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR_ISSUES.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.DEALERSHIP.TABLE_NAME);
        onCreate(db);
    }
}
