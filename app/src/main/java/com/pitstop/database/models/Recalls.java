package com.pitstop.database.models;

import com.pitstop.database.DBModel;

/**
 * Created by David Liu on 11/20/2015.
 */
public class Recalls extends DBModel {
    public Recalls() {
        super("Recalls","RecallID","null");
    }

    @Override
    protected void setUpTable() {
        columns.put("RecallID","Text");
        columns.put("name","Text");
        columns.put("description","Text");
        columns.put("consequences","Text");
        columns.put("action","Text");
        columns.put("make","Text");
        columns.put("model","Text");
        columns.put("year","Text");
        columns.put("recallNumber","Text");
        columns.put("numberAffected","Integer");

    }
}
