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
        if (getString(TITLE_KEY)!=null) {
            Log.d(TAG, getString(TITLE_KEY));
            return getString(TITLE_KEY);
        }
        return "";
    }

    public String getPushType(){
        if (get(DATA_KEY) == null) return "";
        String getDataKey =  (String)((HashMap)get(DATA_KEY)).keySet().toArray()[0];
        return ((HashMap)get(DATA_KEY)).get(getDataKey).toString();
    }

    public String getContent(){
        if (getString(CONTENT_KEY)!= null) {
            Log.d(TAG, getString(CONTENT_KEY));
            return getString(CONTENT_KEY);
        }
        return "";
    }

    public String getDateCreated(){
        if(getCreatedAt()!= null){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(getCreatedAt());
        }
        return "";
    }

    public Boolean isRead(){
        HashMap dataKey = (HashMap)get(DATA_KEY);
        if (dataKey == null) return null;
        else return (boolean)(dataKey.get("isRead"));
    }

    public void setRead(boolean read){
        HashMap dataKey = (HashMap)get(DATA_KEY);
        if (dataKey != null){
            dataKey.put("isRead",read);
            put(DATA_KEY,dataKey);
        }
        saveEventually();
    }
}
