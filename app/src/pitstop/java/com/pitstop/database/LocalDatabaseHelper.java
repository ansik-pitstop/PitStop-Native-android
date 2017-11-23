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

    private static final int DATABASE_VERSION = 51;
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
        db.execSQL(LocalPidStorage.CREATE_TABLE_PID_DATA);
        db.execSQL(LocalPidResult4Storage.CREATE_TABLE_PID_DATA);
        db.execSQL(LocalCarStorage.CREATE_TABLE_CAR);
        db.execSQL(LocalCarIssueStorage.CREATE_TABLE_CAR_ISSUES);
        db.execSQL(LocalAppointmentStorage.CREATE_TABLE_APPOINTMENT);
        db.execSQL(LocalTripStorage.CREATE_TABLE_APPOINTMENT);
        db.execSQL(LocalShopStorage.CREATE_TABLE_DEALERSHIP);
        db.execSQL(LocalParseNotificationStorage.CREATE_TABLE_NOTIFICATION);
        db.execSQL(LocalUserStorage.CREATE_TABLE_USER);
        db.execSQL(LocalScannerStorage.CREATE_TABLE_CAR_ISSUES);
        db.execSQL(LocalDebugMessageStorage.CREATE_TABLE_DEBUG_MESSAGE);
        db.execSQL(LocalDeviceTripStorage.CREATE_TABLE_DEVICE_TRIP);
        db.execSQL(LocalSpecsStorage.CREATE_LOCAL_SPEC_STORAGE);
        db.execSQL(LocalAlarmStorage.CREATE_LOCAL_ALARM_STORAGE);
        db.execSQL(LocalFuelConsumptionStorage.CREATE_LOCAL_FUEL_CONSUMPTION_STORAGE);
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
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.TRIP_DEVICE.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCAL_SPECS_DATA.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCAL_ALARMS.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME);
        onCreate(db);
    }

    BriteDatabase getBriteDatabase() {
        return mBriteDatabase;
    }
}
