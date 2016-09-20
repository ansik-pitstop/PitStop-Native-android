package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pitstop.models.ParseNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 26/04/2016.
 */
public class ParseNotificationStore {
    // NOTIFICATION table create statement
    public static final String CREATE_TABLE_NOTIFICATION = "CREATE TABLE IF NOT EXISTS "
            + TABLES.NOTIFICATION.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY,"
            + TABLES.NOTIFICATION.KEY_TITLE + " TEXT, "
            + TABLES.NOTIFICATION.KEY_ALERT + " TEXT, "
            + TABLES.COMMON.KEY_OBJECT_ID + " TEXT, "
            + TABLES.COMMON.KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper databaseHelper;

    public ParseNotificationStore(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void storeNotification(ParseNotification parseNotification) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TABLES.NOTIFICATION.KEY_TITLE,parseNotification.getTitle());
        values.put(TABLES.NOTIFICATION.KEY_ALERT,parseNotification.getAlert());
        values.put(TABLES.COMMON.KEY_OBJECT_ID, parseNotification.getParsePushId());

        db.insert(TABLES.NOTIFICATION.TABLE_NAME, null, values);
        db.close();
    }

    public List<ParseNotification> getAllNotifications() {
        List<ParseNotification> parseNotifications = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.query(TABLES.NOTIFICATION.TABLE_NAME,null,null,null,null,null,null);

        if(c.moveToFirst()) {
            while(!c.isAfterLast()) {
                parseNotifications.add(cursorToParseNotification(c));
                c.moveToNext();
            }
        }
        db.close();
        return parseNotifications;
    }

    public void deleteAllNotifications() {
        List<ParseNotification> parseNotifications = getAllNotifications();

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        for(ParseNotification notification : parseNotifications) {
            db.delete(TABLES.NOTIFICATION.TABLE_NAME, TABLES.COMMON.KEY_ID +"=?",
                    new String[] {String.valueOf(notification.getId())});
        }
        db.close();
    }

    private ParseNotification cursorToParseNotification(Cursor c) {
        ParseNotification parseNotification = new ParseNotification();
        parseNotification.setId(c.getInt(c.getColumnIndex(TABLES.COMMON.KEY_ID)));
        parseNotification.setTitle(c.getString(c.getColumnIndex(TABLES.NOTIFICATION.KEY_TITLE)));
        parseNotification.setAlert(c.getString(c.getColumnIndex(TABLES.NOTIFICATION.KEY_ALERT)));
        parseNotification.setParsePushId(c.getString(c.getColumnIndex(TABLES.COMMON.KEY_OBJECT_ID)));
        return parseNotification;
    }
}
