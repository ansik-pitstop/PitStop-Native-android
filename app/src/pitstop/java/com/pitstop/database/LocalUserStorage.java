package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.models.Settings;
import com.pitstop.models.User;

/**
 * Created by Ben Wu on 2016-06-07.
 */
public class LocalUserStorage {
    private static final String TAG = LocalUserStorage.class.getSimpleName();

    // USER table create statement
    public static final String CREATE_TABLE_USER = "CREATE TABLE IF NOT EXISTS "
            + TABLES.USER.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.USER.KEY_FIRST_NAME + " TEXT, "
            + TABLES.USER.KEY_LAST_NAME + " TEXT, "
            + TABLES.USER.KEY_EMAIL + " TEXT, "
            + TABLES.USER.KEY_PHONE + " TEXT, "
            + TABLES.USER.KEY_CAR + " TEXT, "
            + TABLES.USER.KEY_FIRST_CAR_ADDED + " TEXT, "
            + TABLES.USER.KEY_ALARMS_ENABLED + " TEXT, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public LocalUserStorage(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storeUserData(User user) {
        Log.d(TAG,"storeUserData() user: "+user);
        if (user == null)
            return;
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = userObjectToContentValues(user);

        long result = db.insert(TABLES.USER.TABLE_NAME, null, values);

    }

    public int updateUser(User user){
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = userObjectToContentValues(user);
        Log.d(TAG,"updateUser() user: "+user+", content values: "+values +", getUser(): "+getUser());

        return db.update(TABLES.USER.TABLE_NAME,values, TABLES.COMMON.KEY_OBJECT_ID + "=?",
                new String[] { String.valueOf(user.getId()) });
    }

    public User getUser() {
        for (StackTraceElement e: Thread.currentThread().getStackTrace()){
            Log.d(TAG,e.toString());
        }
        Log.d(TAG,"\n\n");
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.USER.TABLE_NAME,null,
                null, null,null,null,null);
        User user = null;
        if(c.moveToFirst()) {
            user = cursorToUser(c);
        }
        c.close();
        Log.d(TAG,"getUser() user: "+user);
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
        boolean alarmsEnabled = c.getInt(c.getColumnIndex(TABLES.USER.KEY_ALARMS_ENABLED)) == 1;
        user.setSettings(new Settings(user.getId(),carId,isFirstCarAdded,alarmsEnabled));
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
            values.put(TABLES.USER.KEY_ALARMS_ENABLED, user.getSettings().isAlarmsEnabled());
        }

        return values;
    }

    public void deleteAllUsers() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.USER.TABLE_NAME, null, null);

    }
}
