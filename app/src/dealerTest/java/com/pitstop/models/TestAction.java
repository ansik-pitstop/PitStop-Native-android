package com.pitstop.models;

/**
 * Created by Ben Wu on 2016-09-15.
 */
public class TestAction {

    enum TYPE {
        CONNECT, CHECK_TIME, PID, DTC, VIN, RESET
    }

    public String title;
    public String description;
    public TYPE type;

}
