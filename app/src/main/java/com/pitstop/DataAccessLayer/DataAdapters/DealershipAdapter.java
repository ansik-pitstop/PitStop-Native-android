package com.pitstop.DataAccessLayer.DataAdapters;

import android.content.Context;

import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

/**
 * Created by Paul Soladoye on 3/31/2016.
 */
public class DealershipAdapter {

    //DEALERSHIP table create statement
    public static final String CREATE_TABLE_DEALERSHIP = "CREATE TABLE IF NOT EXISTS "
            + TABLES.DEALERSHIP.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.DEALERSHIP.KEY_NAME + " TEXT, "
            + TABLES.DEALERSHIP.KEY_ADDRESS + " TEXT, "
            + TABLES.DEALERSHIP.KEY_PHONE + " TEXT, "
            + TABLES.DEALERSHIP.KEY_EMAIL + " TEXT, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public DealershipAdapter(Context context) {
        databaseHelper = new LocalDatabaseHelper(context);
    }

    /**
     * Dealership
     */
    public void storeDealership(Dealership dealership) {

    }

    public Dealership getDealership() {
        return new Dealership();
    }
}
