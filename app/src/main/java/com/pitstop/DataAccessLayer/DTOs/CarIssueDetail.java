package com.pitstop.DataAccessLayer.DTOs;

import com.google.gson.annotations.Expose;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by Paul Soladoye on 3/18/2016.
 */
public class CarIssueDetail implements Serializable {

    @Expose(serialize = false, deserialize = false)
    private int id;

    private String item;
    private String description;
    private String action;

    public CarIssueDetail() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static CarIssueDetail createCarIssueDetail(String item,
                                                      String description, String action) {
        CarIssueDetail carIssueDetail = new CarIssueDetail();
        carIssueDetail.setItem(item);
        carIssueDetail.setDescription(description);
        carIssueDetail.setAction(action);
        return carIssueDetail;
    }

    public static CarIssueDetail createCarIssueDetail(JSONObject detailObject) throws JSONException {
        CarIssueDetail carIssueDetail = new CarIssueDetail();

        carIssueDetail.setItem(detailObject.getString("item"));
        carIssueDetail.setDescription(detailObject.getString("description"));
        carIssueDetail.setAction(detailObject.optString("acton"));

        return carIssueDetail;
    }
}