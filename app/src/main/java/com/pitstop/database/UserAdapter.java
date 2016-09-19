package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.models.User;

/**
 * Created by Ben Wu on 2016-06-07.
 */
public class UserAdapter  {

    // USER table create statement
    public static final String CREATE_TABLE_USER = "CREATE TABLE IF NOT EXISTS "
            + TABLES.USER.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.USER.KEY_FIRST_NAME + " TEXT, "
            + TABLES.USER.KEY_LAST_NAME + " TEXT, "
            + TABLES.USER.KEY_EMAIL + " TEXT, "
            + TABLES.USER.KEY_PHONE + " TEXT, "
            + TABLES.COMMON.KEY_OBJECT_ID + " INTEGER, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public UserAdapter(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storeUserData(User user) {
        deleteAllUsers();

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = userObjectToContentValues(user);

        long result = db.insert(TABLES.USER.TABLE_NAME, null, values);

        db.close();
    }

    public User getUser() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.query(TABLES.USER.TABLE_NAME,null,
                null, null,null,null,null);
        User user = null;
        if(c.moveToFirst()) {
            user = cursorToUser(c);
        }

        db.close();

        return user;
    }

    public void deleteAllUsers() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.USER.TABLE_NAME, null, null);

        db.close();
    }

    private User cursorToUser(Cursor c) {
        User user = new User();
        user.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)));
        user.setFirstName(c.getString(c.getColumnIndex(TABLES.USER.KEY_FIRST_NAME)));
        user.setLastName(c.getString(c.getColumnIndex(TABLES.USER.KEY_LAST_NAME)));
        user.setEmail(c.getString(c.getColumnIndex(TABLES.USER.KEY_EMAIL)));
        user.setPhone(c.getString(c.getColumnIndex(TABLES.USER.KEY_PHONE)));

        return user;
    }


    private ContentValues userObjectToContentValues(User user) {
        ContentValues values = new ContentValues();
        values.put(TABLES.COMMON.KEY_OBJECT_ID, user.getId());
        values.put(TABLES.USER.KEY_FIRST_NAME, user.getFirstName());
        values.put(TABLES.USER.KEY_LAST_NAME, user.getLastName());
        values.put(TABLES.USER.KEY_EMAIL, user.getEmail());
        values.put(TABLES.USER.KEY_PHONE, user.getPhone());

        return values;
    }
}
