package com.pitstop.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import com.pitstop.database.TABLES;

public class DebugMessage implements TABLES.DEBUG_MESSAGES{

    private final static int MESSAGE_MAX_LENGTH = 200;
    private final static int MESSAGE_MAX_LENGTH_DEBUG = 5000;

    public static final int TYPE_NETWORK = 0;
    public static final int TYPE_BLUETOOTH = 1;
    public static final int TYPE_OTHER = 2;
    public static final int TYPE_REPO = 3;
    public static final int TYPE_USE_CASE = 4;
    public static final int TYPE_VIEW = 5;
    public static final int TYPE_PRESENTER = 6;
    public static final int TYPE_TRIP = 7;

    public static final int LEVEL_V = 0;
    public static final int LEVEL_D = 1;
    public static final int LEVEL_I = 2;
    public static final int LEVEL_W = 3;
    public static final int LEVEL_E = 4;
    public static final int LEVEL_WTF = 5;

    private double mTimestamp;
    private String mMessage;

    private String tag;
    private int mType;
    private int mLevel;

    public DebugMessage(long timestamp, String message, String tag, int type, int level) {

        if (message.length() > MESSAGE_MAX_LENGTH && level != DebugMessage.LEVEL_D){
            mMessage = message.substring(0,MESSAGE_MAX_LENGTH)+"...";
        }else if (message.length() > MESSAGE_MAX_LENGTH_DEBUG){
            mMessage = message.substring(0,MESSAGE_MAX_LENGTH_DEBUG)+"...";
        }
        else{
            mMessage = message;
        }
        this.tag = tag;
        mTimestamp = timestamp/1000.0D;
        mType = type;
        mLevel = level;
    }

    public DebugMessage() {}

    public double getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(double timestamp) {
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
        message.setTimestamp(c.getDouble(c.getColumnIndex(COLUMN_TIMESTAMP)));
        message.setType(c.getInt(c.getColumnIndex(COLUMN_TYPE)));
        message.setLevel(c.getInt(c.getColumnIndex(COLUMN_LEVEL)));
        message.setTag(c.getString(c.getColumnIndex(COLUMN_TAG)));

        return message;
    }

    public static ContentValues toContentValues(DebugMessage message, boolean sent) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TYPE, message.getType());
        values.put(COLUMN_MESSAGE, message.getMessage());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(COLUMN_LEVEL, message.getLevel());
        values.put(COLUMN_TAG, message.getTag());
        values.put(COLUMN_SENT, sent? 1 : 0);
        Log.d("DebugMessage.content","toContentValues: "+values);

        return values;
    }

    @Override
    public String toString(){
        return String.format("{ message: %s, tag: %s, type: %d, level: %d, phoneTimestamp: %f }"
                ,mMessage,tag,mType,mLevel,mTimestamp);
    }

}
