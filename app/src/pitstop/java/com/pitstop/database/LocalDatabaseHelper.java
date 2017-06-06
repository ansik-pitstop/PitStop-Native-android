package com.pitstop.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import rx.android.schedulers.AndroidSchedulers;


/**
 * Created by Paul Soladoye on 3/16/2016.
 */
public class LocalDatabaseHelper extends SQLiteOpenHelper {
    // Logcat tag
    private static final String LOG = "TestDatabaseHelper";

    private static LocalDatabaseHelper instance;


    private static final int DATABASE_VERSION = 28;
    public static final String DATABASE_NAME = "PITSTOP_DB";

    private BriteDatabase mBriteDatabase;

    public static LocalDatabaseHelper getInstance(Context context){
        if (instance == null) {
            instance = new LocalDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mBriteDatabase = new SqlBrite.Builder().build().wrapDatabaseHelper(this, AndroidSchedulers.mainThread());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(LocalPidAdapter.CREATE_TABLE_PID_DATA);
        db.execSQL(LocalPidResult4Adapter.CREATE_TABLE_PID_DATA);
        db.execSQL(LocalCarAdapter.CREATE_TABLE_CAR);
        db.execSQL(LocalCarIssueAdapter.CREATE_TABLE_CAR_ISSUES);
        db.execSQL(LocalAppointmentAdapter.CREATE_TABLE_APPOINTMENT);
        db.execSQL(LocalTripAdapter.CREATE_TABLE_APPOINTMENT);
        db.execSQL(LocalShopAdapter.CREATE_TABLE_DEALERSHIP);
        db.execSQL(ParseNotificationStore.CREATE_TABLE_NOTIFICATION);
        db.execSQL(UserAdapter.CREATE_TABLE_USER);
        db.execSQL(LocalScannerAdapter.CREATE_TABLE_CAR_ISSUES);
        db.execSQL(LocalDebugMessageAdapter.CREATE_TABLE_DEBUG_MESSAGE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PID.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PID.TABLE_NAME_RESULT_4);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR_ISSUES.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.APPOINTMENT.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.TRIP.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.SHOP.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.NOTIFICATION.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.USER.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.SCANNER.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.DEBUG_MESSAGES.TABLE_NAME);
        onCreate(db);
    }

    BriteDatabase getBriteDatabase() {
        return mBriteDatabase;
    }
}
