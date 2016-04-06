package com.pitstop.DataAccessLayer.DataAdapters;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.DataAccessLayer.LocalDatabaseHelper;

/**
 * Created by psola on 4/1/2016.
 */
public class RecallsDataRetriever extends LocalDatabaseHelper {
    //RECALL table create statement
    private static final String CREATE_TABLE_RECALL = "CREATE TABLE "
            + TABLES.RECALL.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + "INTEGER PRIMARY KEY,"
            + TABLES.COMMON.KEY_PARSE_ID + " TEXT, "
            + TABLES.RECALL.KEY_NAME + " TEXT, "
            + TABLES.RECALL.KEY_DESCRIPTION + " TEXT, "
            + TABLES.RECALL.KEY_REMEDY + " TEXT, "
            + TABLES.RECALL.KEY_RISK + " TEXT, "
            + TABLES.RECALL.KEY_EFFECTIVE_DATE + " TEXT, "
            + TABLES.RECALL.KEY_OEM_ID + " TEXT, "
            + TABLES.RECALL.KEY_REIMBURSEMENT + " TEXT, "
            + TABLES.RECALL.KEY_STATE + " TEXT, "
            + TABLES.RECALL.KEY_RISK_RANK + "INTEGER"
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    public RecallsDataRetriever(Context context) {
        super(context);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /*db.execSQL(CREATE_TABLE_RECALL);*/
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        /*db.execSQL("DROP TABLE IF EXISTS " + TABLES.RECALL.TABLE_NAME);

        onCreate(db);*/
    }
}
