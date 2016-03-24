package com.pitstop.DataAccessLayer.DTOs;

import java.io.Serializable;

/**
 * Created by Paul Soladoye on 3/18/2016.
 */
public class CarIssueDetail implements Serializable {
    private String item;
    private String description;
    private String action;

    public CarIssueDetail() {}

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
}