package com.pitstop.models.service;

import android.os.Parcel;
import android.os.Parcelable;

import com.pitstop.models.issue.Issue;
import com.pitstop.models.issue.IssueDetail;
import com.pitstop.models.issue.UpcomingIssue;

/**
 *
 * Created by Karol Zdebel on 6/7/2017.
 */

public class UpcomingService implements Parcelable {

    private UpcomingIssue issue;

    private Issue realIssue;


    public String getDescription(){
        return realIssue.getDescription();
    }

    public int getPriority(){
        return realIssue.getPriority();
    }

    public String getAction(){
        return realIssue.getAction();
    }

    public String getItem(){
        return realIssue.getItem();
    }

    public int getId() { return realIssue.getIssueId(); }

    public UpcomingService(UpcomingIssue issue) {
        this.realIssue = issue;
        this.issue = issue;
    }

    protected UpcomingService(Parcel in) {
        UpcomingIssue issue = new UpcomingIssue();
        IssueDetail issueDetail = new IssueDetail();
        issueDetail.setDescription(in.readString());
        issue.setPriority(in.readInt());
        issueDetail.setItem(in.readString());
        issueDetail.setAction(in.readString());
        issue.setIssueDetail(issueDetail);
        this.issue = issue;
        this.realIssue = issue;

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
        parcel.writeString(this.issue.getDescription());
        parcel.writeInt(this.issue.getPriority());
        parcel.writeString(this.issue.getItem());
        parcel.writeString(this.issue.getAction());
    }
}