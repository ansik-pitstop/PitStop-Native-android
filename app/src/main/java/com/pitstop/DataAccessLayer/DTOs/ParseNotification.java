package com.pitstop.DataAccessLayer.DTOs;

import java.io.Serializable;

/**
 * Created by Paul Soladoye on 26/04/2016.
 */
public class ParseNotification implements Serializable {

    private int id;
    private String parsePushId;
    private String alert;
    private String title;

    public ParseNotification() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getParsePushId() {
        return parsePushId;
    }

    public void setParsePushId(String parsePushId) {
        this.parsePushId = parsePushId;
    }

    public String getAlert() {
        return alert;
    }

    public void setAlert(String alert) {
        this.alert = alert;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
