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
        columns.put("ServiceID","Text");
        columns.put("action","Text");
        columns.put("engineCode","Text");
        columns.put("intervalMileage","Text");
        columns.put("item","Text");
        columns.put("description","Text");
        columns.put("priority","Text");
    }

    @Override
    public void setValue(String key, String value) {

    }
}
