package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by Karol Zdebel on 9/28/2017.
 */

public abstract class EmissionsReport implements Parcelable{

    private int id;
    private int vhrId = -1;
    private String misfire;
    private String ignition;
    private String components;
    private String fuelSystem;
    private Date createdAt;
    private boolean pass;

    public EmissionsReport(int id, String misfire, String ignition, String components
            , String fuelSystem, Date createdAt, boolean pass) {

        this.id = id;
        this.misfire = misfire;
        this.ignition = ignition;
        this.components = components;
        this.fuelSystem = fuelSystem;
        this.createdAt = createdAt;
        this.pass = pass;
    }

    public EmissionsReport(int id, int vhrId, String misfire, String ignition, String components
            , String fuelSystem, Date createdAt, boolean pass) {

        this.id = id;
        this.vhrId = vhrId;
        this.misfire = misfire;
        this.ignition = ignition;
        this.components = components;
        this.fuelSystem = fuelSystem;
        this.createdAt = createdAt;
        this.pass = pass;
    }

    protected EmissionsReport(Parcel in) {
        id = in.readInt();
        misfire = in.readString();
        ignition = in.readString();
        components = in.readString();
        fuelSystem = in.readString();
        pass = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeInt(id);
        parcel.writeString(misfire);
        parcel.writeString(ignition);
        parcel.writeString(components);
        parcel.writeString(fuelSystem);
        parcel.writeByte((byte) (pass ? 1 : 0));
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMisfire() {
        return misfire;
    }

    public void setMisfire(String misfire) {
        this.misfire = misfire;
    }

    public String getIgnition() {
        return ignition;
    }

    public void setIgnition(String ignition) {
        this.ignition = ignition;
    }

    public String getComponents() {
        return components;
    }

    public void setComponents(String components) {
        this.components = components;
    }

    public String getFuelSystem() {
        return fuelSystem;
    }

    public void setFuelSystem(String fuelSystem) {
        this.fuelSystem = fuelSystem;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public int getVhrId() {
        return vhrId;
    }

    public void setVhrId(int vhrId) {
        this.vhrId = vhrId;
    }

    @Override
    public String toString(){
        try{
            return String.format("id:%d, vhrId:%d, misfire:%s, ignition:%s, components:%s" +
                            " , createdAt:%s, pass:%b",getId(), getVhrId(), getMisfire()
                    , getIgnition(), getComponents(), getFuelSystem(), getCreatedAt().toString()
                    , isPass());
        }catch(NullPointerException e){
            return "null";
        }

    }
}
