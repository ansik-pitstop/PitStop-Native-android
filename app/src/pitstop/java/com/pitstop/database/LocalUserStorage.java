package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.models.Settings;
import com.pitstop.models.User;

/**
 * Created by Ben Wu on 2016-06-07.
 */
public class LocalUserStorage {

    // USER table create statement
    public static final String CREATE_TABLE_USER = "CREATE TABLE IF NOT EXISTS "
            + TABLES.USER.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.USER.KEY_FIRST_NAME + " TEXT, "
            + TABLES.USER.KEY_LAST_NAME + " TEXT, "
            + TABLES.USER.KEY_EMAIL + " TEXT, "
            + TABLES.USER.KEY_PHONE + " TEXT, "
            + TABLES.USER.KEY_CAR + " TEXT, "
            + TABLES.USER.KEY_FIRST_CAR_ADDED + " TEXT, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalUserStorage(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storeUserData(User user) {
        deleteAllUsers();

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = userObjectToContentValues(user);

        long result = db.insert(TABLES.USER.TABLE_NAME, null, values);

    }

    public User getUser() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.USER.TABLE_NAME,null,
                null, null,null,null,null);
        User user = null;
        if(c.moveToFirst()) {
            user = cursorToUser(c);
        }
        c.close();
        return user;
    }

    private User cursorToUser(Cursor c) {
        User user = new User();
        user.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)));
        user.setFirstName(c.getString(c.getColumnIndex(TABLES.USER.KEY_FIRST_NAME)));
        user.setLastName(c.getString(c.getColumnIndex(TABLES.USER.KEY_LAST_NAME)));
        user.setEmail(c.getString(c.getColumnIndex(TABLES.USER.KEY_EMAIL)));
        user.setPhone(c.getString(c.getColumnIndex(TABLES.USER.KEY_PHONE)));
        int carId = c.getInt(c.getColumnIndex(TABLES.USER.KEY_CAR));
        boolean isFirstCarAdded = c.getInt(c.getColumnIndex(TABLES.USER.KEY_FIRST_CAR_ADDED)) == 1;
        if (carId != -1){
            user.setSettings(new Settings(carId,isFirstCarAdded));
        }
        return user;
    }

    private ContentValues userObjectToContentValues(User user) {
        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_OBJECT_ID, user.getId());
        values.put(TABLES.USER.KEY_FIRST_NAME, user.getFirstName());
        values.put(TABLES.USER.KEY_LAST_NAME, user.getLastName());
        values.put(TABLES.USER.KEY_EMAIL, user.getEmail());
        values.put(TABLES.USER.KEY_PHONE, user.getPhone());
        if (user.getSettings() != null){
            values.put(TABLES.USER.KEY_CAR, user.getSettings().getCarId());
            values.put(TABLES.USER.KEY_FIRST_CAR_ADDED, user.getSettings().isFirstCarAdded());
        }

        return values;
    }

    public void deleteAllUsers() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.USER.TABLE_NAME, null, null);

    }
}
