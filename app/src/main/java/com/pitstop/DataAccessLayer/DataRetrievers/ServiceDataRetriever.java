package com.pitstop.DataAccessLayer.DataRetrievers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

/**
 * Created by psola on 4/1/2016.
 */
public class ServiceDataRetriever {

    //SERVICE table create statement
    private static final String CREATE_TABLE_SERVICE = "CREATE TABLE "
            + TABLES.SERVICE.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY, "
            + TABLES.SERVICE.KEY_TYPE + " TEXT, "
            + TABLES.SERVICE.KEY_DEALERSHIP + " TEXT, "
            + TABLES.SERVICE.KEY_ITEM + " TEXT, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    public ServiceDataRetriever(Context context) {

    }

    /*@Override
    public void onCreate(SQLiteDatabase db) {
        *//*db.execSQL(CREATE_TABLE_SERVICE);
        super.onCreate(db);*//*
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        *//*db.execSQL("DROP TABLE IF EXISTS " + TABLES.SERVICE.TABLE_NAME);
        onCreate(db);
        super.onUpgrade(db, oldVersion, newVersion);*//*
    }*/
}
