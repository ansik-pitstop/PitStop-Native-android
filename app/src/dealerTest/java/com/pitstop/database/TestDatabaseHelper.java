package com.pitstop.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by yifan on 16/10/14.
 */

public class TestDatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = TestDatabaseHelper.class.getSimpleName();

    private static TestDatabaseHelper instance;

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "PITSTOP_TEST_DB";

    private Context mContext;

    public static TestDatabaseHelper getInstance(Context context){
        if (instance == null) {
            instance = new TestDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private TestDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TestPidAdapter.CREATE_TABLE_PID_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PID.TABLE_NAME);
        onCreate(db);
    }

}
