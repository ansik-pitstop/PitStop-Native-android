package com.pitstop.DataAccessLayer.DTOs;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Paul Soladoye on 3/18/2016.
 */
public class CarIssue implements Parcelable {
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

    public static final String ISSUE_DONE = "done";
    public static final String ISSUE_NEW = "new";
    public static final String ISSUE_PENDING = "pending";

    @Expose(serialize = false, deserialize = false)
    private int id;
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

    public CarIssueDetail getIssueDetail() {
        return issueDetail;
    }

    public void setIssueDetail(CarIssueDetail issueDetail) {
        this.issueDetail = issueDetail;
    }

    public static CarIssue createCarIssue(JSONObject issueObject, int carId) throws JSONException {
        CarIssue carIssue = new CarIssue();

        carIssue.setId(issueObject.getInt("id"));
        carIssue.setCarId(carId);
        carIssue.setStatus(issueObject.getString("status"));
        carIssue.setTimestamp(issueObject.getString("doneAt"));
        carIssue.setPriority(Integer.parseInt(issueObject.getString("priority")));
        carIssue.setIssueType(issueObject.getString("issueType"));
        carIssue.setIssueDetail(CarIssueDetail.createCarIssueDetail(issueObject.getJSONObject("issueDetail")));

        if(carIssue.getIssueType().equals(DTC) && !issueObject.getJSONObject("issueDetail").isNull("isPending")
                && issueObject.getJSONObject("issueDetail").getBoolean("isPending")) {
            carIssue.setIssueType(PENDING_DTC);
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
        dest.writeInt(this.carId);
        dest.writeInt(this.id);
        dest.writeString(this.status);
        dest.writeString(this.timestamp);
        dest.writeInt(this.priority);
        dest.writeString(this.issueType);
        dest.writeParcelable(this.issueDetail, 0);
    }

    protected CarIssue(Parcel in) {
        this.carId = in.readInt();
        this.id = in.readInt();
        this.status = in.readString();
        this.timestamp = in.readString();
        this.priority = in.readInt();
        this.issueType = in.readString();
        this.issueDetail = in.readParcelable(CarIssueDetail.class.getClassLoader());
    }

    public static final Parcelable.Creator<CarIssue> CREATOR = new Parcelable.Creator<CarIssue>() {
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