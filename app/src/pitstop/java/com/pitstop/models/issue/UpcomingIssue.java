package com.pitstop.models.issue;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by zohaibhussain on 2017-01-11.
 */

public class UpcomingIssue implements Issue{

    private int carId;
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("intervalMileage")
    @Expose
    private String intervalMileage;
    @SerializedName("intervalMonth")
    @Expose
    private Object intervalMonth;
    @SerializedName("fixedMileage")
    @Expose
    private Object fixedMileage;
    @SerializedName("fixedMonth")
    @Expose
    private Object fixedMonth;
    @SerializedName("priority")
    @Expose
    private Integer priority;
    @SerializedName("issueDetail")
    @Expose
    private IssueDetail issueDetail;

    public Integer getId() {
        return id;
    }

    public void setCarId(int carId){
        this.carId = carId;
    }

    public int getCarId(){
        return carId;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setIntervalMileage(String mileage){ this.intervalMileage = mileage; }

    public String getIntervalMileage() {
        return intervalMileage;
    }

    public void setIntervalMileage(String intervalMileage) {
        this.intervalMileage = intervalMileage;
    }

    public Object getIntervalMonth() {
        return intervalMonth;
    }

    public void setIntervalMonth(Object intervalMonth) {
        this.intervalMonth = intervalMonth;
    }

    public Object getFixedMileage() {
        return fixedMileage;
    }

    public void setFixedMileage(Object fixedMileage) {
        this.fixedMileage = fixedMileage;
    }

    public Object getFixedMonth() {
        return fixedMonth;
    }

    public void setFixedMonth(Object fixedMonth) {
        this.fixedMonth = fixedMonth;
    }

    @Override
    public int getIssueId(){
        return id.intValue();
    }

    @Override
    public int getPriority() {
        if(priority == null){
            return 2;
        }
        return priority;
    }

    @Override
    public String getDescription() {
        return issueDetail.getDescription();
    }

    @Override
    public String getItem() {
        return issueDetail.getItem();
    }

    @Override
    public String getAction() {
        return issueDetail.getAction();
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public IssueDetail getIssueDetail() {
        return issueDetail;
    }

    public void setIssueDetail(IssueDetail issueDetail) {
        this.issueDetail = issueDetail;
    }
}
