package com.pitstop.database.models;

import com.pitstop.database.DBModel;

/**
 * Created by David Liu on 11/20/2015.
 */
public class Services extends DBModel {
    public Services() {
        super("Services","ServiceID", "null");
    }

    @Override
    protected void setUpTable() {
        columns.put("ServiceID","INTEGER PRIMARY KEY AUTOINCREMENT");
        columns.put("ParseID","Text");
        columns.put("serviceType","Text");
        columns.put("itemDescription","Text");
        columns.put("item","Text");
        columns.put("action","Text");
        columns.put("intervalMonth","Text");
        columns.put("intervalMileage","Text");
        columns.put("intervalFixed","Text");
        columns.put("priority","Text");
        columns.put("dealership","Text");
    }

}
