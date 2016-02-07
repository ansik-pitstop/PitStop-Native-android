package com.pitstop.database.models;

import com.pitstop.database.DBModel;
/**
 * Created by thebe on 12/22/15.
 */
public class DTCs extends DBModel {
    public DTCs() { super("DTCs", "DTCID", "dtcCode"); }

    @Override
    protected void setUpTable() {
        columns.put("DTCID", "INTEGER PRIMARY KEY AUTOINCREMENT");
        columns.put("dtcCode", "Text");
        columns.put("description", "Text");
    }
}
