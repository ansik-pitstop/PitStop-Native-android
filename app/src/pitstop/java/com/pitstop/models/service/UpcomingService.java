package com.pitstop.models.service;

import android.os.Parcel;
import android.os.Parcelable;

import com.pitstop.models.issue.IssueDetail;
import com.pitstop.models.issue.UpcomingIssue;

/**
 *
 * Created by Karol Zdebel on 6/7/2017.
 */

public class UpcomingService implements Parcelable {

    private UpcomingIssue issue;

    public String getDescription(){
        return issue.getIssueDetail().getDescription();
    }

    public int getPriority(){
        return issue.getPriority();
    }

    public String getAction(){
        return issue.getIssueDetail().getAction();
    }

    public String getItem(){
        return issue.getIssueDetail().getItem();
    }

    public int getId() { return issue.getId(); }

    public UpcomingService(UpcomingIssue issue) {
        this.issue = issue;
    }

    protected UpcomingService(Parcel in) {
        String description = in.readString();
        int priority = in.readInt();
        String item = in.readString();
        String action = in.readString();
        String interval = in.readString();
        int id = in.readInt();
        int carId = in.readInt();
        IssueDetail issueDetail = new IssueDetail(item,action,description,"","");
        this.issue = new UpcomingIssue(carId,id,priority,interval,issueDetail);
    }

    public static final Creator<UpcomingService> CREATOR = new Creator<UpcomingService>() {
        @Override
        public UpcomingService createFromParcel(Parcel in) {
            return new UpcomingService(in);
        }

        @Override
        public UpcomingService[] newArray(int size) {
            return new UpcomingService[size];
        }
    };

    public int getMileage() {
        return Integer.valueOf(issue.getIntervalMileage());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.issue.getIssueDetail().getDescription());
        parcel.writeInt(this.issue.getPriority());
        parcel.writeString(this.issue.getIssueDetail().getItem());
        parcel.writeString(this.issue.getIssueDetail().getAction());
        parcel.writeString(this.issue.getIntervalMileage());
        parcel.writeInt(this.issue.getId());
        parcel.writeInt(this.issue.getCarId());
    }
}