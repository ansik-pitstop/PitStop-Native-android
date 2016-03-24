package com.pitstop.DataAccessLayer.DTOs;

import com.google.gson.annotations.SerializedName;
import com.parse.ParseObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/18/2016.
 */
public class CarIssue implements Serializable {
    private static final String PRIORITY_KEY = "priority";
    private static final String ITEM_KEY = "item";
    private static final String ITEM_DESCRIPTION_KEY = "itemDescription";
    private static final String ACTION_KEY = "action";
    private static final String DTCCODE_KEY = "dtcCode";
    private static final String RECALLS_ITEM_KEY = "name";
    private static final String DESCRIPTION_KEY = "description";

    private static final int RECALLS_PRIORITY_DEFAULT_VALUE = 6;
    private static final int DTCS_PRIORITY_DEFAULT_VALUE = 5;
    private static final int SERVICES_PRIORITY_DEFAULT_VALUE = 1;

    private long id;
    private String parseId;
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

    public String getParseId() {
        return parseId;
    }

    public void setParseId(String parseId) {
        this.parseId = parseId;
    }

    public CarIssueDetail getIssueDetail() {
        return issueDetail;
    }

    public void setIssueDetail(CarIssueDetail issueDetail) {
        this.issueDetail = issueDetail;
    }

    public static CarIssue createCarIssue(ParseObject parseObject, String issueType) {
        CarIssue carIssue = null;

        if(parseObject != null) {
            carIssue = new CarIssue();
            carIssue.setParseId(parseObject.getObjectId());
            carIssue.setIssueType(issueType);

            if(issueType.equals("recall")) {
                carIssue.setPriority(RECALLS_PRIORITY_DEFAULT_VALUE);

                carIssue.setIssueDetail(CarIssueDetail.createCarIssueDetail(
                        parseObject.getString(RECALLS_ITEM_KEY),
                        parseObject.getString("description"),
                        "Recall for "
                ));
            } else if(issueType.equals("dtc")) {
                carIssue.setPriority(DTCS_PRIORITY_DEFAULT_VALUE);

                carIssue.setIssueDetail(CarIssueDetail.createCarIssueDetail(
                        parseObject.getString(DTCCODE_KEY),
                        parseObject.getString("description"),
                        "Engine Issue: DTC code "
                ));
            } else {
                carIssue.setPriority(parseObject.getInt("priority"));
                carIssue.setIssueDetail(CarIssueDetail.createCarIssueDetail(
                        parseObject.getString(ITEM_KEY),
                        parseObject.getString(ITEM_DESCRIPTION_KEY),
                        parseObject.getString(ACTION_KEY)
                ));
            }
        }
        return carIssue;
    }

    public static ArrayList<CarIssue> createCarIssues(List<ParseObject> parseObjects, String issueType) {
        ArrayList<CarIssue> carIssues = new ArrayList<>();
        for(ParseObject parseObject : parseObjects) {
            carIssues.add(createCarIssue(parseObject,issueType));
        }

        return carIssues;
    }
}