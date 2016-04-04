package com.pitstop.DataAccessLayer.DataRetrievers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

/**
 * Created by Paul Soladoye on 4/1/2016.
 */
public class DtcDataRetriever extends LocalDatabaseHelper {
    //DTC table create statement
    private static final String CREATE_TABLE_DTC = "CREATE TABLE "
            + TABLES.DTC.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.DTC.KEY_CODE + " TEXT, "
            + TABLES.DTC.KEY_DESCRIPTION + " TEXT, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    public DtcDataRetriever(Context context) {
        super(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_DTC);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.DTC.TABLE_NAME);
        onCreate(db);
    }
}
