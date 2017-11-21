package com.pitstop.database;

import android.content.Context;

import com.pitstop.models.DebugMessage;
import com.squareup.sqlbrite.QueryObservable;

public class LocalDebugMessageStorage implements TABLES.DEBUG_MESSAGES {
    public static final String CREATE_TABLE_DEBUG_MESSAGE = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME + "(" + KEY_ID + " INTEGER PRIMARY KEY,"
            + COLUMN_TYPE + " INTEGER,"
            + COLUMN_MESSAGE + " TEXT,"
            + COLUMN_TIMESTAMP + " INTEGER,"
            + COLUMN_LEVEL + " INTEGER,"
            + COLUMN_TAG +" TEXT,"
            + KEY_CREATED_AT + " DATETIME" + ")";

    private LocalDatabaseHelper mDatabaseHelper;
    private final String TAG = getClass().getSimpleName();

    public LocalDebugMessageStorage(Context context) {
        mDatabaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public void addMessage(DebugMessage message) {
        mDatabaseHelper.getBriteDatabase().insert(TABLE_NAME, DebugMessage.toContentValues(message));
    }

    public QueryObservable getQueryObservable(int type) {
        QueryObservable observable = mDatabaseHelper.getBriteDatabase().createQuery(TABLE_NAME,
                "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_TYPE + "=? ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 30",
                String.valueOf(type));

        return observable;
    }

    public QueryObservable getQueryObservableAll() {
        QueryObservable observable = mDatabaseHelper.getBriteDatabase().createQuery(TABLE_NAME,
                "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 512");

        return observable;
    }

}
