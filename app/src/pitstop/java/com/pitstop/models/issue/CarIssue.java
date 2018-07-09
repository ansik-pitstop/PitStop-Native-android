package com.pitstop.models.issue;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.pitstop.models.report.CarHealthItem;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Created by Paul Soladoye on 3/18/2016.
 */
public class CarIssue implements Parcelable, Issue {

    private static final String TAG = CarIssue.class.getSimpleName();

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
    public static final String SERVICE_PRESET = "service_preset";
    public static final String SERVICE_USER = "service_user";

    public static final String ISSUE_DONE = "done";
    public static final String ISSUE_NEW = "new";
    public static final String ISSUE_PENDING = "pending";

    private int id;
    private int carId;
    private int year;
    private int month;
    private int day;
    private int doneMileage;
    private String status;
    private String doneAt;
    private int priority;
    private IssueDetail issueDetail;
    private String issueType;

    public CarIssue() {}

    public int getDaysAgo(){
        Calendar doneAt = Calendar.getInstance();
        doneAt.set(year,month,day);
        int daysToday = (int) TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis());
        int doneDay = (int)TimeUnit.MILLISECONDS.toDays(doneAt.getTimeInMillis());
        return daysToday-doneDay;
    }

    public IssueDetail getIssueDetail() {
        return issueDetail;
    }

    public void setIssueDetail(IssueDetail issueDetail) {
        this.issueDetail = issueDetail;
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
        return issueDetail.getItem();
    }

    public String getAction() {
        return issueDetail.getAction();
    }

    public String getDescription() {
        return issueDetail.getDescription();
    }

    public String getSymptoms() {
        return issueDetail.getSymptoms();
    }

    public String getCauses() {
        return issueDetail.getCauses();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CarIssue)){ return false; }

        CarIssue carIssue = (CarIssue)obj;
        return carIssue.getId() == getId();
    }

    public static CarIssue fromCarHealthItem(CarHealthItem carHealthItem, int carId){
        Log.d(TAG,"fromCarHealthItem() carHealthItem: "+carHealthItem+", carId: "+carId);

        String symptoms = "";
        String action = "";
        String causes = "";
        String issueType = "";

        if (carHealthItem instanceof Recall){
            action = "Recall";
            issueType = RECALL;
        }else if (carHealthItem instanceof EngineIssue){
            symptoms = ((EngineIssue)carHealthItem).getSymptoms();
            causes = ((EngineIssue)carHealthItem).getCauses();
            if (((EngineIssue)carHealthItem).isPending()){
                action = "Pending Engine Code";
                issueType = PENDING_DTC;
            }
            else{
                action = "Stored Engine Code";
                issueType = DTC;
            }
        }else if (carHealthItem instanceof Service){
            issueType = SERVICE_PRESET;
            action = ((Service)carHealthItem).getAction();
        }
        CarIssue carIssue = new CarIssue.Builder()
                .setIssueType(issueType)
                .setCarId(carId)
                .setItem(carHealthItem.getItem())
                .setAction(action)
                .setDescription(carHealthItem.getDescription())
                .setPriority(carHealthItem.getPriority())
                .setSymptoms(symptoms)
                .setCauses(causes)
                .build();

        Log.d(TAG,"fromCarHealthItem() carIssue: "+carIssue);

        return carIssue;
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
        dest.writeString(this.issueDetail.getItem());
        dest.writeString(this.issueDetail.getAction());
        dest.writeString(this.issueDetail.getDescription());
        dest.writeString(this.issueDetail.getCauses());
        dest.writeString(this.issueDetail.getSymptoms());
    }

    protected CarIssue(Parcel in) {
        this.id = in.readInt();
        this.carId = in.readInt();
        this.status = in.readString();
        this.doneAt = in.readString();
        this.priority = in.readInt();
        this.issueType = in.readString();
        String item = in.readString();
        String action = in.readString();
        String description = in.readString();
        String causes = in.readString();
        String symptoms = in.readString();
        this.issueDetail = new IssueDetail(item,action,description,symptoms,causes);
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
        String item = builder.item;
        String description = builder.description;
        String action = builder.action;
        String symptoms = builder.symptoms;
        String causes = builder.causes;
        issueDetail = new IssueDetail(item,action,description,symptoms,causes);
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

        public Builder setSymptoms(String symptoms) {
            this.symptoms = symptoms;
            return this;
        }

        public Builder setCauses(String causes) {
            this.causes = causes;
            return this;
        }

        public CarIssue build(){
            return new CarIssue(this);
        }
    }

    @Override
    public String toString(){
        try{
            return String.format("id:%d, carId:%d, year:%d, month:%d, day:%d, doneMileage:%d, status:%s" +
                            ", doneAt:%s, priority:%d, issueType:%s" +
                            ",issueDetail: %s", id, carId, year, month, day ,doneMileage, status, doneAt
                    , priority, issueType, issueDetail);
        }catch(NullPointerException e){
            return "null";
        }

    }
}