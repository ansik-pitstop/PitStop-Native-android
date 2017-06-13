package com.pitstop.models;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.pitstop.models.issue.UpcomingIssue;

/**
 * Created by zohaibhussain on 2017-01-11.
 */

public class TimelineResult {
    @SerializedName("type")
    @Expose
    private String type;
    @SerializedName("issues")
    @Expose
    private List<UpcomingIssue> upcomingIssues = null;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<UpcomingIssue> getUpcomingIssues() {
        return upcomingIssues;
    }

    public void setUpcomingIssues(List<UpcomingIssue> upcomingIssues) {
        this.upcomingIssues = upcomingIssues;
    }
}
