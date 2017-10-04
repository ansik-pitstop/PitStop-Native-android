package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Karol Zdebel on 9/20/2017.
 */

public class EngineIssue extends CarHealthItem implements Parcelable{
    private boolean isPending;
    private String symptoms;
    private String causes;

    public EngineIssue(int id, int priority, boolean isPending, String item
            , String symptoms, String description, String causes) {
        super(id,priority,item,description);
        this.isPending = isPending;
        this.symptoms = symptoms;
        this.causes = causes;
    }

    protected EngineIssue(Parcel in) {
        super(in);
        isPending = in.readByte() != 0;
        symptoms = in.readString();
        causes = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel,i);
        parcel.writeByte((byte)(isPending ? 1 : 0));
        parcel.writeString(symptoms);
        parcel.writeString(causes);
    }

    public static final Creator<EngineIssue> CREATOR = new Creator<EngineIssue>() {
        @Override
        public EngineIssue createFromParcel(Parcel in) {
            return new EngineIssue(in);
        }

        @Override
        public EngineIssue[] newArray(int size) {
            return new EngineIssue[size];
        }
    };

    public String getCauses() {
        return causes;
    }

    public void setCauses(String causes) {
        this.causes = causes;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    @Override
    public String toString(){
        return "item: "+getItem()+", priority: "+getPriority() +", isPending: "+isPending
                +", description: "+getDescription()+", symptoms: "+getSymptoms()
                +", causes: "+causes;
    }
}
