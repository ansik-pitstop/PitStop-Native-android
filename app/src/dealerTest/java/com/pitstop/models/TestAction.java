package com.pitstop.models;

/**
 * Created by Ben Wu on 2016-09-15.
 */
public class TestAction {

    public enum Type {
        SETUP, DISCONNECT, CONNECT, CHECK_TIME, VIN, PID, DTC, COLLECT_DATA ,RESET
    }

    public String title;
    public String description;
    public Type type;

    public TestAction(String title, String description, Type type) {
        this.title = title;
        this.description = description;
        this.type = type;
    }
}
