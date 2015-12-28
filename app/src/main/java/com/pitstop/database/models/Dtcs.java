package com.pitstop.database.models;

import com.pitstop.database.DBModel;
/**
 * Created by thebe on 12/22/15.
 */
public class Dtcs extends DBModel {
    public Dtcs() { super("Dtcs", "dtcId", "dtcCode"); }

    @Override
    protected void setUpTable() {
        columns.put("dtcId", "INTEGER PRIMARY KEY AUTOINCREMENT");
        columns.put("dtcCode", "Text");
        columns.put("description", "Text");
    }
}
