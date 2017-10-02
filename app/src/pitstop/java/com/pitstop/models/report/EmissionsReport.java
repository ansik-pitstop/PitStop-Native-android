package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by Karol Zdebel on 9/28/2017.
 */

public class EmissionsReport implements Parcelable{

    private int id;
    private int vhrId = -1;
    private String misfire;
    private String ignition;
    private String components;
    private String fuelSystem;
    private String NMHCCatalyst;
    private String boostPressure;
    private String EGRVVTSystem;
    private String exhaustSensor;
    private String NOxSCRMonitor;
    private String PMFilterMonitoring;
    private Date createdAt;
    private boolean pass;

    public EmissionsReport(int id, String misfire, String ignition, String components
            , String fuelSystem, String NMHCCatalyst, String boostPressure
            , String EGRVVTSystem, String exhaustSensor, String NOxSCRMonitor
            , String PMFilterMonitoring, Date createdAt, boolean pass) {

        this.id = id;
        this.misfire = misfire;
        this.ignition = ignition;
        this.components = components;
        this.fuelSystem = fuelSystem;
        this.NMHCCatalyst = NMHCCatalyst;
        this.boostPressure = boostPressure;
        this.EGRVVTSystem = EGRVVTSystem;
        this.exhaustSensor = exhaustSensor;
        this.NOxSCRMonitor = NOxSCRMonitor;
        this.PMFilterMonitoring = PMFilterMonitoring;
        this.createdAt = createdAt;
        this.pass = pass;
    }

    public EmissionsReport(int id, int vhrId, String misfire, String ignition, String components
            , String fuelSystem, String NMHCCatalyst, String boostPressure
            , String EGRVVTSystem, String exhaustSensor, String NOxSCRMonitor
            , String PMFilterMonitoring, Date createdAt, boolean pass) {

        this.id = id;
        this.vhrId = vhrId;
        this.misfire = misfire;
        this.ignition = ignition;
        this.components = components;
        this.fuelSystem = fuelSystem;
        this.NMHCCatalyst = NMHCCatalyst;
        this.boostPressure = boostPressure;
        this.EGRVVTSystem = EGRVVTSystem;
        this.exhaustSensor = exhaustSensor;
        this.NOxSCRMonitor = NOxSCRMonitor;
        this.PMFilterMonitoring = PMFilterMonitoring;
        this.createdAt = createdAt;
        this.pass = pass;
    }

    protected EmissionsReport(Parcel in) {
        id = in.readInt();
        misfire = in.readString();
        ignition = in.readString();
        components = in.readString();
        fuelSystem = in.readString();
        NMHCCatalyst = in.readString();
        boostPressure = in.readString();
        EGRVVTSystem = in.readString();
        exhaustSensor = in.readString();
        NOxSCRMonitor = in.readString();
        PMFilterMonitoring = in.readString();
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
        parcel.writeString(NMHCCatalyst);
        parcel.writeString(boostPressure);
        parcel.writeString(EGRVVTSystem);
        parcel.writeString(exhaustSensor);
        parcel.writeString(NOxSCRMonitor);
        parcel.writeString(PMFilterMonitoring);
        parcel.writeByte((byte) (pass ? 1 : 0));
    }

    public static final Creator<EmissionsReport> CREATOR = new Creator<EmissionsReport>() {
        @Override
        public EmissionsReport createFromParcel(Parcel in) {
            return new EmissionsReport(in);
        }

        @Override
        public EmissionsReport[] newArray(int size) {
            return new EmissionsReport[size];
        }
    };

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

    public String getNMHCCatalyst() {
        return NMHCCatalyst;
    }

    public void setNMHCCatalyst(String NMHCCatalyst) {
        this.NMHCCatalyst = NMHCCatalyst;
    }

    public String getBoostPressure() {
        return boostPressure;
    }

    public void setBoostPressure(String boostPressure) {
        this.boostPressure = boostPressure;
    }

    public String getEGRVVTSystem() {
        return EGRVVTSystem;
    }

    public void setEGRVVTSystem(String EGRVVTSystem) {
        this.EGRVVTSystem = EGRVVTSystem;
    }

    public String getExhaustSensor() {
        return exhaustSensor;
    }

    public void setExhaustSensor(String exhaustSensor) {
        this.exhaustSensor = exhaustSensor;
    }

    public String getNOxSCRMonitor() {
        return NOxSCRMonitor;
    }

    public void setNOxSCRMonitor(String NOxSCRMonitor) {
        this.NOxSCRMonitor = NOxSCRMonitor;
    }

    public String getPMFilterMonitoring() {
        return PMFilterMonitoring;
    }

    public void setPMFilterMonitoring(String PMFilterMonitoring) {
        this.PMFilterMonitoring = PMFilterMonitoring;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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
        return String.format("id:%d, vhrId:%d, misfire:%s, ignition:%s, components:%s" +
                ", fuel system: %s, catalyst:%s, boost pressure:%s, egr/vvt system:%s" +
                ", exhaust sensor:%s, NOx/SCR monitor:%s, PM fiter monitoring:%s, createdAt:%s" +
                ", pass:%b",getId(), getVhrId(), getMisfire(), getIgnition(), getComponents()
                , getFuelSystem(), getNMHCCatalyst(), getBoostPressure(), getEGRVVTSystem()
                , getExhaustSensor(), getNOxSCRMonitor(), getPMFilterMonitoring()
                , getCreatedAt().toString(), isPass());
    }
}
