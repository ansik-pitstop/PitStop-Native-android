package com.pitstop.models.issue;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by zohaibhussain on 2017-01-11.
 */

public class IssueDetail {
    @SerializedName("item")
    @Expose
    private String item;
    @SerializedName("action")
    @Expose
    private String action;
    @SerializedName("description")
    @Expose
    private String description;

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

}
