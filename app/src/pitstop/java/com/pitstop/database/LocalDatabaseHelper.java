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

    private static final int DATABASE_VERSION = 73;
    public static final String DATABASE_NAME = "PITSTOP_DB";

    private BriteDatabase mBriteDatabase;

    public static LocalDatabaseHelper getInstance(Context context){
        if (instance == null) {
            instance = new LocalDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public LocalDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mBriteDatabase = new SqlBrite.Builder().build().wrapDatabaseHelper(this, AndroidSchedulers.mainThread());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(LocalPidStorage.CREATE_TABLE_PID_DATA);
        db.execSQL(LocalCarStorage.Companion.getCREATE_TABLE_CAR());
        db.execSQL(LocalCarStorage.Companion.getCREATE_TABLE_PENDING_UPDATES());
        db.execSQL(LocalCarIssueStorage.CREATE_TABLE_CAR_ISSUES);
        db.execSQL(LocalAppointmentStorage.CREATE_TABLE_APPOINTMENT);
        db.execSQL(LocalShopStorage.Companion.getCREATE_TABLE_DEALERSHIP());
        db.execSQL(LocalParseNotificationStorage.CREATE_TABLE_NOTIFICATION);
        db.execSQL(LocalUserStorage.CREATE_TABLE_USER);
        db.execSQL(LocalDebugMessageStorage.CREATE_TABLE_DEBUG_MESSAGE);
        db.execSQL(LocalSpecsStorage.CREATE_LOCAL_SPEC_STORAGE);
        db.execSQL(LocalAlarmStorage.CREATE_LOCAL_ALARM_STORAGE);
        db.execSQL(LocalFuelConsumptionStorage.CREATE_LOCAL_FUEL_CONSUMPTION_STORAGE);
        db.execSQL(LocalPendingTripStorage.Companion.getCREATE_PENDING_TRIP_TABLE());
        db.execSQL(LocalLocationStorage.Companion.getCREATE_LOCATION_DATA_TABLE());
        db.execSQL(LocalActivityStorage.Companion.getCREATE_ACTIVITY_DATA_TABLE());
        db.execSQL(LocalTripStorage.CREATE_TABLE_TRIP);
        db.execSQL(LocalTripStorage.CREATE_TABLE_LOCATION_START);
        db.execSQL(LocalTripStorage.CREATE_TABLE_LOCATION_END);
        db.execSQL(LocalTripStorage.CREATE_TABLE_LOCATION_POLYLINE);
        db.execSQL(LocalTripStorage.CREATE_TABLE_LOCATION);
        db.execSQL(LocalSensorDataStorage.CREATE_TABLE_SENSOR_DATA);
        db.execSQL(LocalSensorDataStorage.CREATE_TABLE_SENSOR_DATA_POINT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PID.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR_PENDING.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.CAR_ISSUES.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.APPOINTMENT.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.TRIP.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.SHOP.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.NOTIFICATION.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.USER.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.SCANNER.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.DEBUG_MESSAGES.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCAL_ALARMS.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.TRIP.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCATION_START.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCATION_END.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCATION_POLYLINE.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCATION.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.PENDING_TRIP_DATA.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.SENSOR_DATA.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.SENSOR_DATA_POINT.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.LOCATION_DATA.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLES.ACTIVITY_DATA.TABLE_NAME);

        onCreate(db);
    }

    public void deleteAllData(){
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.PID.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.CAR.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.CAR_PENDING.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.CAR_ISSUES.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.APPOINTMENT.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.TRIP.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.SHOP.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.NOTIFICATION.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.USER.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.DEBUG_MESSAGES.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.LOCAL_SPECS_DATA.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.LOCAL_ALARMS.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.LOCAL_FUEL_CONSUMPTION.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.TRIP.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.LOCATION_START.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.LOCATION_END.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.LOCATION_POLYLINE.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.LOCATION.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.PENDING_TRIP_DATA.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.SENSOR_DATA.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.SENSOR_DATA_POINT.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.LOCATION_DATA.TABLE_NAME);
        getWritableDatabase().execSQL("DELETE FROM " + TABLES.ACTIVITY_DATA.TABLE_NAME);
    }

    BriteDatabase getBriteDatabase() {
        return mBriteDatabase;
    }
}
