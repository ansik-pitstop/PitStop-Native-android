package com.pitstop.DataAccessLayer.DTOs;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.parse.ParseObject;

import org.json.JSONArray;
import org.json.JSONException;
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
    private static final int PENDING_DTC_PRIORITY = 2;
    private static final int SERVICES_PRIORITY_DEFAULT_VALUE = 1;

    public static final String DTC = "dtc"; // stored only
    public static final String PENDING_DTC = "pending_dtc";
    public static final String RECALL = "recall_recallmasters";
    public static final String SERVICE = "service";
    public static final String EDMUNDS = "service_edmunds";
    public static final String FIXED = "fixed";
    public static final String INTERVAL = "interval";

    @Expose(serialize = false, deserialize = false)
    private int id;
    private String parseId;
    private int carId;
    private String status;
    @SerializedName("doneAt")
    private String timestamp;
    private int priority;
    private String issueType;
    private CarIssueDetail issueDetail;

    public CarIssue() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
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

    /*public static CarIssue createCarIssue(ParseObject parseObject, String issueType, String carId) {
        CarIssue carIssue = null;

        if(parseObject != null) {
            carIssue = new CarIssue();
            carIssue.setParseId(parseObject.getObjectId());
            carIssue.setIssueType(issueType);
            carIssue.setCarId(carId);

            if(issueType.equals(RECALL)) {
                carIssue.setPriority(RECALLS_PRIORITY_DEFAULT_VALUE);
                carIssue.setStatus(parseObject.getString("state"));

                carIssue.setIssueDetail(CarIssueDetail.createCarIssueDetail(
                        parseObject.getString(RECALLS_ITEM_KEY),
                        parseObject.getString("description"),
                        "Recall for "
                ));
            } else if(issueType.equals(DTC)) {
                carIssue.setPriority(DTCS_PRIORITY_DEFAULT_VALUE);

                carIssue.setIssueDetail(CarIssueDetail.createCarIssueDetail(
                        parseObject.getString(DTCCODE_KEY),
                        parseObject.getString("description"),
                        "Engine Issue: DTC code "
                ));
            } else if(issueType.equals(PENDING_DTC)) {
                carIssue.setPriority(PENDING_DTC_PRIORITY);

                carIssue.setIssueDetail(CarIssueDetail.createCarIssueDetail(
                        parseObject.getString(DTCCODE_KEY),
                        parseObject.getString("description"),
                        "Potential Engine Issue: DTC code "
                ));
            } else {
                carIssue.setPriority(parseObject.getInt(PRIORITY_KEY));
                carIssue.setIssueDetail(CarIssueDetail.createCarIssueDetail(
                        parseObject.getString(ITEM_KEY),
                        parseObject.getString(ITEM_DESCRIPTION_KEY),
                        parseObject.getString(ACTION_KEY)
                ));
            }
        }
        return carIssue;
    }*/

    public static CarIssue createCarIssue(JSONObject issueObject, int carId) throws JSONException {
        CarIssue carIssue = new CarIssue();

        carIssue.setId(issueObject.getInt("id"));
        carIssue.setCarId(carId);
        carIssue.setStatus(issueObject.getString("status"));
        carIssue.setTimestamp(issueObject.getString("doneAt"));
        carIssue.setPriority(Integer.parseInt(issueObject.getString("priority")));
        carIssue.setIssueType(issueObject.getString("issueType"));
        carIssue.setIssueDetail(CarIssueDetail.createCarIssueDetail(issueObject.getJSONObject("issueDetail")));

        return carIssue;
    }

    /*public static ArrayList<CarIssue> createCarIssues(List<ParseObject> parseObjects,
                                                      String issueType, String carId) {
        ArrayList<CarIssue> carIssues = new ArrayList<>();
        for(ParseObject parseObject : parseObjects) {
            if(issueType.equals(RECALL)) {
                String status = parseObject.getString("state");
                if( status != null && (status.equals("new") || status.equals("pending"))) {
                    carIssues.add(createCarIssue(parseObject, issueType, carId));
                }
            } else {
                carIssues.add(createCarIssue(parseObject, issueType, carId));
            }
        }

        return carIssues;
    }*/

    public static ArrayList<CarIssue> createCarIssues(JSONArray issueArr, int carId) throws JSONException {
        ArrayList<CarIssue> carIssues = new ArrayList<>();

        for(int i = 0 ; i < issueArr.length() ; i++) {
            carIssues.add(createCarIssue(issueArr.getJSONObject(i), carId));
        }

        return carIssues;
    }
}