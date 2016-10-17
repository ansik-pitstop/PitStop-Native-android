package com.pitstop.models;

import org.json.JSONArray;

/**
 * Created by yifan on 16/10/17.
 */

public class DtcPayload {

    private JSONArray pidArr;
    private String dtc;

    public DtcPayload(JSONArray pidArr, String dtc) {
        this.pidArr = pidArr;
        this.dtc = dtc;
    }

    public JSONArray getPidArr() {
        return pidArr;
    }

    public String getDtc() {
        return dtc;
    }
}
