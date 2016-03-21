package com.pitstop.DataAccessLayer.DTOs;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by Paul Soladoye on 3/18/2016.
 */
public class CarIssue implements Serializable {
    private long id;
    private String status;
    @SerializedName("doneAt")
    private Date timestamp;
    private int priority;
    private String issueType;
    private CarIssueDetail issueDetail;

    public CarIssue() {}

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public CarIssueDetail getIssueDetail() {
        return issueDetail;
    }

    public void setIssueDetail(CarIssueDetail issueDetail) {
        this.issueDetail = issueDetail;
    }
}
