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
        columns.put("remedy","Text");
        columns.put("risk","Text");
        columns.put("effectiveDate","Text");
        columns.put("oemID","Text");
        columns.put("reimbursement","Text");
        columns.put("state","Text");
        columns.put("riskRank","Integer");

    }
}
