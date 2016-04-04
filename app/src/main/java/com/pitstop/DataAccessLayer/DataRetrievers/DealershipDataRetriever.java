package com.pitstop.DataAccessLayer.DataRetrievers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

/**
 * Created by Paul Soladoye on 3/31/2016.
 */
public class DealershipDataRetriever {

    //DEALERSHIP table create statement
    public static final String CREATE_TABLE_DEALERSHIP = "CREATE TABLE IF NOT EXISTS "
            + TABLES.DEALERSHIP.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.DEALERSHIP.KEY_NAME + " TEXT, "
            + TABLES.DEALERSHIP.KEY_ADDRESS + " TEXT, "
            + TABLES.DEALERSHIP.KEY_PHONE + " TEXT, "
            + TABLES.DEALERSHIP.KEY_EMAIL + " TEXT, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public DealershipDataRetriever(Context context) {
        databaseHelper = new LocalDatabaseHelper(context);
    }

    /*@Override
    public void onCreate(SQLiteDatabase db) {
        super.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onUpgrade(db, oldVersion, newVersion);
    }*/

    /**
     * Dealership
     */
    public void storeDealership(Dealership dealership) {

    }

    public Dealership getDealership() {
        return new Dealership();
    }
}
