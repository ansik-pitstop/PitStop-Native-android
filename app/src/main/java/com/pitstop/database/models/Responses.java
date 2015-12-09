package com.pitstop.database.models;

import com.pitstop.database.DBModel;

/**
 * Created by David Liu on 12/9/2015.
 */
public class Responses extends DBModel {
    public Responses() {
        super("Responses","ResponseID","deviceId");
    }

    @Override
    protected void setUpTable() {
        columns.put("ResponseID","Text");
        columns.put("result","Integer");
        columns.put("deviceId","Text");
        columns.put("tripId","Text");
        columns.put("dataNumber","Integer");
        columns.put("tripFlag","Text");
        columns.put("rtcTime","Text");
        columns.put("protocolType","Text");
        columns.put("tripMileage","Text");
        columns.put("tripfuel","Text");
        columns.put("vState","Text");
        columns.put("vState","Text");
        columns.put("OBD","Text");
        columns.put("Freeze","Text");
        columns.put("supportPid","Text");
        columns.put("dtcData","Text");
    }

    @Override
    public void setValue(String key, String value) {
        values.put(key, value);
    }
}
