package com.pitstop.DataAccessLayer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pitstop.DataAccessLayer.DataAdapters.LocalCarAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarIssueAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalDealershipAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalPidAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.TABLES;

/**
 * Created by Paul Soladoye on 3/16/2016.
 */
public class LocalDatabaseHelper extends SQLiteOpenHelper {
    // Logcat tag
    private static final String LOG = "LocalDatabaseHelper";

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "PITSTOP_DB";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(LocalPidAdapter.CREATE_TABLE_PID_DATA);
        db.execSQL(LocalCarAdapter.CREATE_TABLE_CAR);
        db.execSQL(LocalCarIssueAdapter.CREATE_TABLE_CAR_ISSUES);
        db.execSQL(LocalDealershipAdapter.CREATE_TABLE_DEALERSHIP);
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
