package com.pitstop.models;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by zohaibhussain on 2017-01-11.
 */

public class TimelineResult {
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("issues")
    @Expose
    private List<Issue> issues = null;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }
}
