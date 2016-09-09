package com.pitstop.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Paul Soladoye on 3/16/2016.
 */
public class LocalDatabaseHelper extends SQLiteOpenHelper {
    // Logcat tag
    private static final String LOG = "LocalDatabaseHelper";

    private static final int DATABASE_VERSION = 18;
    private static final String DATABASE_NAME = "PITSTOP_DB";

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(LocalPidAdapter.CREATE_TABLE_PID_DATA);
        db.execSQL(LocalPidResult4Adapter.CREATE_TABLE_PID_DATA);
        db.execSQL(LocalCarAdapter.CREATE_TABLE_CAR);
        db.execSQL(LocalCarIssueAdapter.CREATE_TABLE_CAR_ISSUES);
        db.execSQL(LocalShopAdapter.CREATE_TABLE_DEALERSHIP);
        db.execSQL(ParseNotificationStore.CREATE_TABLE_NOTIFICATION);
        db.execSQL(UserAdapter.CREATE_TABLE_USER);
        db.execSQL(LocalScannerAdapter.CREATE_TABLE_CAR_ISSUES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PID.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PID.TABLE_NAME_RESULT_4);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR_ISSUES.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.SHOP.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.NOTIFICATION.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.USER.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.SCANNER.TABLE_NAME);
        onCreate(db);
    }
}
