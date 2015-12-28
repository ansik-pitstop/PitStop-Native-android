package com.pitstop.database.models;

import com.pitstop.database.DBModel;

/**
 * Created by David on 12/28/2015.
 */
public class Shops extends DBModel{
    public Shops() {
        super("Shops", "ShopID", null);
    }

    @Override
    protected void setUpTable() {
        columns.put("ShopID","Text Primary Key");
        columns.put("name","Text");
        columns.put("address","Text");
        columns.put("email","Text");
    }
}
