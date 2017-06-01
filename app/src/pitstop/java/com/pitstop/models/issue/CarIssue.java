package com.pitstop.models.issue;

import android.os.Parcel;
import android.os.Parcelable;

import com.castel.obd.util.JsonUtil;
import com.google.gson.annotations.Expose;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by Paul Soladoye on 3/18/2016.
 */
public class CarIssue implements Parcelable, Issue {
    public static final String DTC = "dtc"; // stored only
    public static final String PENDING_DTC = "pending_dtc";
    public static final String RECALL = "recall_recallmasters";
    public static final String SERVICE = "service";
    public static final String TENTATIVE = "tentative";
    public static final String EDMUNDS = "service_edmunds";
    public static final String FIXED = "fixed";
    public static final String INTERVAL = "interval";
    public static final String TYPE_USER_INPUT = "userInput";
    public static final String TYPE_PRESET = "preset";

    public static final String ISSUE_DONE = "done";
    public static final String ISSUE_NEW = "new";
    public static final String ISSUE_PENDING = "pending";

    @Expose(serialize = false, deserialize = false)
    private int id;
    private int carId;
    private int year;
    private int month;
    private int day;
    private int doneMileage;
    private String status;
    private String doneAt;
    private int priority;
    private String issueType;
    private String item;
    private String description;
    private String symptoms;
    private String causes;
    private String action;

    public CarIssue() {}

    public int getDaysAgo(){
        Calendar doneAt = Calendar.getInstance();
        doneAt.set(year,month,day);
        int daysToday = (int) TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis());
        int doneDay = (int)TimeUnit.MILLISECONDS.toDays(doneAt.getTimeInMillis());
        return daysToday-doneDay;
    }

    public boolean isDone() {
        return doneAt != null;
    }

    public int getId() {
        return id;
    }

    public int getDoneMileage() {
        return doneMileage;
    }

    public void setDoneMileage(int doneMileage) {
        this.doneMileage = doneMileage;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
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

    @Override
    public int getIssueId() {
        return getId();
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

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getCauses() {
        return causes;
    }

    public void setCauses(String causes) {
        this.causes = causes;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CarIssue)){ return false; }

        CarIssue carIssue = (CarIssue)obj;
        return carIssue.getId() == getId();
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
            if (!issueDetail.isNull("symptoms")){
                carIssue.setSymptoms(issueDetail.getString("symptoms"));
            }
            if (!issueDetail.isNull("causes")){
                carIssue.setCauses(issueDetail.getString("causes"));
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
        dest.writeString(this.symptoms);
        dest.writeString(this.causes);
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
        this.symptoms = in.readString();
        this.causes = in.readString();
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

    public CarIssue(Builder builder){
        id = builder.id;
        carId = builder.carId;
        status = builder.status;
        doneAt = builder.doneAt;
        priority = builder.priority;
        issueType = builder.issueType;
        item = builder.item;
        description = builder.description;
        action = builder.action;
        symptoms = builder.symptoms;
        causes = builder.causes;
    }

    public static class Builder{
        private int id;
        private int carId;
        private String status = "";
        private String doneAt = "";
        private int priority;
        private String issueType = "";
        private String item = "";
        private String description = "";
        private String action = "";
        private String symptoms;
        private String causes;

        public Builder() {}

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setCarId(int carId) {
            this.carId = carId;
            return this;
        }

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder setDoneAt(String doneAt) {
            this.doneAt = doneAt;
            return this;
        }

        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder setIssueType(String issueType) {
            this.issueType = issueType;
            return this;
        }

        public Builder setItem(String item) {
            this.item = item;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setAction(String action) {
            this.action = action;
            return this;
        }

        public void setSymptoms(String symptoms) {
            this.symptoms = symptoms;
        }

        public void setCauses(String causes) {
            this.causes = causes;
        }

        public CarIssue build(){
            return new CarIssue(this);
        }
    }


}