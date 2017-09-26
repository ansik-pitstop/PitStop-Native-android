package com.pitstop.models;

import android.util.Log;

import com.parse.ParseClassName;
import com.parse.ParseObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * Created by zohaibhussain on 2016-12-19.
 */

@ParseClassName("Notification")
public class Notification extends ParseObject {
    private final static String TAG = Notification.class.getSimpleName();

    public static final String TITLE_KEY = "title";
    public static final String DATA_KEY = "data";
    public static final String CONTENT_KEY = "content";
    public static final String PUSH_TYPE_KEY = "pushType";

    public Notification() {
    }



    public String getTitle()
    {
        Log.d(TAG, getString(TITLE_KEY));
        return getString(TITLE_KEY);
    }

    public String getPushType(){
        if (get(DATA_KEY) == null) return "";
        String getDataKey =  (String)((HashMap)get(DATA_KEY)).keySet().toArray()[0];
        return ((HashMap)get(DATA_KEY)).get(getDataKey).toString();
    }

    public String getContent(){
        Log.d(TAG, getString(CONTENT_KEY));
        return getString(CONTENT_KEY);
    }

    public String getDateCreated(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(getCreatedAt());
    }

    @Override
    public String toString(){
        return "hasDataKey ? "+(get(DATA_KEY) == null);
    }

}
