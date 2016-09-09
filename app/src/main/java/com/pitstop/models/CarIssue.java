package com.pitstop.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.castel.obd.util.JsonUtil;
import com.google.gson.annotations.Expose;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Paul Soladoye on 3/18/2016.
 */
public class CarIssue implements Parcelable {
    public static final String DTC = "dtc"; // stored only
    public static final String PENDING_DTC = "pending_dtc";
    public static final String RECALL = "recall_recallmasters";
    public static final String SERVICE = "service";
    public static final String EDMUNDS = "service_edmunds";
    public static final String FIXED = "fixed";
    public static final String INTERVAL = "interval";

    public static final String ISSUE_DONE = "done";
    public static final String ISSUE_NEW = "new";
    public static final String ISSUE_PENDING = "pending";

    @Expose(serialize = false, deserialize = false)
    private int id;
    private int carId;
    private String status;
    private String doneAt;
    private int priority;
    private String issueType;
    private String item;
    private String description;
    private String action;

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

    public String getDoneAt() {
        return doneAt;
    }

    public void setDoneAt(String doneAt) {
        this.doneAt = doneAt;
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

    public static CarIssue createCarIssue(JSONObject issueObject, int carId) throws JSONException {
        CarIssue carIssue = JsonUtil.json2object(issueObject.toString(), CarIssue.class);

        carIssue.setCarId(carId);

        JSONObject issueDetail = issueObject.getJSONObject("issueDetail");
        if(issueDetail != null) {
            if(!issueDetail.isNull("item")) {
                carIssue.setItem(issueDetail.getString("item"));
            }
            if(!issueDetail.isNull("action")) {
                carIssue.setAction(issueDetail.getString("action"));
            } else if(carIssue.getIssueType().equals(DTC)) {
                carIssue.setAction("Engine issue: Code");
            } else if(carIssue.getIssueType().equals(RECALL)) {
                carIssue.setAction("Recall:");
            } else {
                carIssue.setAction("");
            }
            if(!issueDetail.isNull("description")) {
                carIssue.setDescription(issueDetail.getString("description"));
            }
        }

        if(carIssue.getIssueType().equals(DTC) && !issueObject.getJSONObject("issueDetail").isNull("isPending")
                && issueObject.getJSONObject("issueDetail").getBoolean("isPending")) {
            carIssue.setIssueType(PENDING_DTC);
            carIssue.setAction("Potential Engine issue: Code");
        }

        return carIssue;
    }

    public static ArrayList<CarIssue> createCarIssues(JSONArray issueArr, int carId) throws JSONException {
        ArrayList<CarIssue> carIssues = new ArrayList<>();

        for(int i = 0 ; i < issueArr.length() ; i++) {
            carIssues.add(createCarIssue(issueArr.getJSONObject(i), carId));
        }

        return carIssues;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeInt(this.carId);
        dest.writeString(this.status);
        dest.writeString(this.doneAt);
        dest.writeInt(this.priority);
        dest.writeString(this.issueType);
        dest.writeString(this.item);
        dest.writeString(this.description);
        dest.writeString(this.action);
    }

    protected CarIssue(Parcel in) {
        this.id = in.readInt();
        this.carId = in.readInt();
        this.status = in.readString();
        this.doneAt = in.readString();
        this.priority = in.readInt();
        this.issueType = in.readString();
        this.item = in.readString();
        this.description = in.readString();
        this.action = in.readString();
    }

    public static final Creator<CarIssue> CREATOR = new Creator<CarIssue>() {
        @Override
        public CarIssue createFromParcel(Parcel source) {
            return new CarIssue(source);
        }

        @Override
        public CarIssue[] newArray(int size) {
            return new CarIssue[size];
        }
    };
}