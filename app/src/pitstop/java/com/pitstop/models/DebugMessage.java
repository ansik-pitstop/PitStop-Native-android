package com.pitstop.models;

import android.content.ContentValues;
import android.database.Cursor;

import com.pitstop.database.TABLES;

public class DebugMessage implements TABLES.DEBUG_MESSAGES{

    private final static int MESSAGE_MAX_LENGTH = 500;

    public static final int TYPE_NETWORK = 0;
    public static final int TYPE_BLUETOOTH = 1;
    public static final int TYPE_OTHER = 2;

    public static final int LEVEL_V = 0;
    public static final int LEVEL_D = 1;
    public static final int LEVEL_I = 2;
    public static final int LEVEL_W = 3;
    public static final int LEVEL_E = 4;
    public static final int LEVEL_WTF = 5;

    private long mTimestamp;
    private String mMessage;

    private String tag;
    private int mType;
    private int mLevel;

    public DebugMessage(long timestamp, String message, String tag, int type, int level) {

        if (message.length() > MESSAGE_MAX_LENGTH){
            mMessage = message.substring(0,MESSAGE_MAX_LENGTH)+"...";
        }
        else{
            mMessage = message;
        }
        this.tag = tag;
        mTimestamp = timestamp;
        mType = type;
        mLevel = level;
    }

    public DebugMessage() {}

    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }

    public int getLevel() {
        return mLevel;
    }

    public void setLevel(int level) {
        mLevel = level;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public static DebugMessage fromCursor(Cursor c) {
        DebugMessage message = new DebugMessage();

        message.setMessage(c.getString(c.getColumnIndex(COLUMN_MESSAGE)));
        message.setTimestamp(c.getLong(c.getColumnIndex(COLUMN_TIMESTAMP)));
        message.setType(c.getInt(c.getColumnIndex(COLUMN_TYPE)));
        message.setLevel(c.getInt(c.getColumnIndex(COLUMN_LEVEL)));

        return message;
    }

    public static ContentValues toContentValues(DebugMessage message) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TYPE, message.getType());
        values.put(COLUMN_MESSAGE, message.getMessage());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(COLUMN_LEVEL, message.getLevel());

        return values;
    }
}
