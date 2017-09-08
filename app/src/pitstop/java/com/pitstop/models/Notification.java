package com.pitstop.models;

import android.text.format.DateFormat;

import com.parse.ParseClassName;
import com.parse.ParseObject;


import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zohaibhussain on 2016-12-19.
 */

@ParseClassName("Notification")
public class Notification extends ParseObject {
    public static final String TITLE_KEY = "title";

    public static final String CONTENT_KEY = "content";

    public Notification() {
    }

    public String getTitle(){

        return getString(TITLE_KEY);
    }

    public String getContent(){
        return getString(CONTENT_KEY);
    }

    public String getDateCreated(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(getCreatedAt());
    }

}
