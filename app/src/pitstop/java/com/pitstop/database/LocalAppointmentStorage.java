package com.pitstop.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.models.Appointment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Matthew on 2017-05-05.
 */

public class LocalAppointmentStorage {

    private final String TAG = LocalAppointmentStorage.class.getSimpleName();

    // APPOINTMENT table create statement
    public static final String CREATE_TABLE_APPOINTMENT = "CREATE TABLE IF NOT EXISTS "
            + TABLES.APPOINTMENT.TABLE_NAME + "("
            + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.APPOINTMENT.KEY_COMMENT + " TEXT, "
            + TABLES.APPOINTMENT.KEY_DATE + " TEXT, "
            + TABLES.APPOINTMENT.KEY_STATE + " TEXT, "
            + TABLES.APPOINTMENT.KEY_SHOP_ID + " INTEGER, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalAppointmentStorage(LocalDatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    /**
     * Store appointment data
     */
    public void storeAppointmentData(Appointment appointment) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = appointmentObjectToContentValues(appointment);
        long result = db.insert(TABLES.APPOINTMENT.TABLE_NAME, null, values);
    }

    public void deleteAndStoreAppointments(List<Appointment> appointments){
        Log.d(TAG,"deleteAndStoreAppointments() appointments: "+appointments);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();
        try{
            db.delete(TABLES.APPOINTMENT.TABLE_NAME, null, null);
            for (Appointment a: appointments){
                ContentValues v = appointmentObjectToContentValues(a);
                db.insert(TABLES.APPOINTMENT.TABLE_NAME, null, v);
            }
            db.setTransactionSuccessful();
        } finally{
            db.endTransaction();
        }
    }

    /**
     * Get all appointments
     */
    public List<Appointment> getAllAppointments() {
        List<Appointment> appointments = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.APPOINTMENT.TABLE_NAME, null,null,null,null,null,null);

        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                appointments.add(cursorToAppointment(c));
                c.moveToNext();
            }
        }
        c.close();
        return appointments;
    }

    /**
     * Get appointment by parse id
     */

    public Appointment getAppointment(String parseId) {

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.APPOINTMENT.TABLE_NAME,null,
                TABLES.COMMON.KEY_OBJECT_ID +"=?", new String[] {parseId},null,null,null);
        Appointment appointment = null;
        if(c.moveToFirst()) {
            appointment = cursorToAppointment(c);
        }

        c.close();
        return appointment;
    }




    /**
     * Update appointment
     */
    public int updateAppointment(Appointment appointment) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = appointmentObjectToContentValues(appointment);

        int rows = db.update(TABLES.APPOINTMENT.TABLE_NAME,values, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                new String[] { String.valueOf(appointment.getId()) });



        return rows;
    }

    /** Delete all appointments*/
    public void deleteAllAppointments() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.APPOINTMENT.TABLE_NAME, null, null);


    }

    private Appointment cursorToAppointment(Cursor c) {
        Appointment appointment = new Appointment();
        appointment.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)));
        appointment.setComments(c.getString(c.getColumnIndex(TABLES.APPOINTMENT.KEY_COMMENT)));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        try{
            appointment.setDate(simpleDateFormat.parse(c.getString(c.getColumnIndex(TABLES.APPOINTMENT.KEY_DATE))));
        }catch(ParseException e){
            appointment.setDate(new Date());
        }
        appointment.setState(c.getString(c.getColumnIndex(TABLES.APPOINTMENT.KEY_STATE)));
        appointment.setShopId(c.getInt(c.getColumnIndex(TABLES.APPOINTMENT.KEY_SHOP_ID)));

        return appointment;
    }


    private ContentValues appointmentObjectToContentValues(Appointment appointment) {
        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_OBJECT_ID, appointment.getId());

        values.put(TABLES.APPOINTMENT.KEY_COMMENT, appointment.getComments());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        values.put(TABLES.APPOINTMENT.KEY_DATE, simpleDateFormat.format(appointment.getDate()));
        values.put(TABLES.APPOINTMENT.KEY_STATE, appointment.getState());
        values.put(TABLES.APPOINTMENT.KEY_SHOP_ID, appointment.getShopId());


        return values;
    }

    public void deleteAllRows(){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.APPOINTMENT.TABLE_NAME, null, null);

    }

    public void deleteAppointment(Appointment appointment){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(TABLES.APPOINTMENT.TABLE_NAME, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                new String[]{String.valueOf(appointment.getId())});

    }


}
