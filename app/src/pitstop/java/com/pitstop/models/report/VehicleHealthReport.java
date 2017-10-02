package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Karol Zdebel on 9/19/2017.
 */

public class VehicleHealthReport implements Parcelable {

    private int id;
    private List<EngineIssue> engineIssues;
    private List<Recall> recalls;
    private List<Service> services;
    private Date date;

    public VehicleHealthReport(int id, Date date, List<EngineIssue> engineIssues
            , List<Recall> recalls, List<Service> services) {
        this.id = id;
        this.date = date;
        this.engineIssues = engineIssues;
        this.recalls = recalls;
        this.services = services;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<EngineIssue> getEngineIssues() {
        return engineIssues;
    }

    public void setEngineIssues(List<EngineIssue> engineIssues) {
        this.engineIssues = engineIssues;
    }

    public List<Recall> getRecalls() {
        return recalls;
    }

    public void setRecalls(List<Recall> recalls) {
        this.recalls = recalls;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString(){
        try{
            return "id: "+id+", engineIssues: "+getEngineIssues() +", recalls: "+getRecalls()
                    +", services: "+getServices();
        }catch(NullPointerException e){
            return "null";
        }

    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected VehicleHealthReport(Parcel in) {
        this.id = in.readInt();
        this.recalls = new ArrayList<>();
        this.engineIssues = new ArrayList<>();
        this.services = new ArrayList<>();
        in.readList(this.engineIssues,EngineIssue.class.getClassLoader());
        in.readList(this.recalls,Recall.class.getClassLoader());
        in.readList(this.services,Service.class.getClassLoader());
        this.date = (Date)in.readSerializable();
    }

    public static final Creator<VehicleHealthReport> CREATOR = new Creator<VehicleHealthReport>() {
        @Override
        public VehicleHealthReport createFromParcel(Parcel source) {
            return new VehicleHealthReport(source);
        }

        @Override
        public VehicleHealthReport[] newArray(int size) {
            return new VehicleHealthReport[size];
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeList(engineIssues);
        parcel.writeList(recalls);
        parcel.writeList(services);
        parcel.writeSerializable(date);
    }
}
