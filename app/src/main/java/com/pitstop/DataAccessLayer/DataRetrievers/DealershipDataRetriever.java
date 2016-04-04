package com.pitstop.DataAccessLayer.DataRetrievers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

/**
 * Created by Paul Soladoye on 3/31/2016.
 */
public class DealershipDataRetriever extends LocalDatabaseHelper {

    //DEALERSHIP table create statement
    private static final String CREATE_TABLE_DEALERSHIP = "CREATE TABLE "
            + TABLES.DEALERSHIP.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.DEALERSHIP.KEY_NAME + " TEXT, "
            + TABLES.DEALERSHIP.KEY_ADDRESS + " TEXT, "
            + TABLES.DEALERSHIP.KEY_PHONE + " TEXT, "
            + TABLES.DEALERSHIP.KEY_EMAIL + " TEXT, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    public DealershipDataRetriever(Context context) {
        super(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_DEALERSHIP);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.DEALERSHIP.TABLE_NAME);
    }

    /**
     * Dealership
     */
    public int storeDealership(Dealership dealership) {

        return 0;
    }

    public Dealership getDealership() {
        return new Dealership();
    }
}
