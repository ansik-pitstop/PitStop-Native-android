package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by Karol Zdebel on 10/3/2017.
 */

public class PetrolEmissionsReport extends EmissionsReport implements Parcelable{
    private String NMHCCatalyst;
    private String components;
    private String NOxSCRMonitor;
    private String boostPressure;
    private String reserved1;
    private String reserved2;
    private String exhaustSensor;
    private String PMFilterMonitoring;

    public PetrolEmissionsReport(int id, String misfire, String ignition, String components
            , String fuelSystem, Date createdAt, boolean pass, String NMHCCatalyst
            , String components1, String NOxSCRMonitor, String boostPressure, String reserved1
            , String reserved2, String exhaustSensor, String PMFilterMonitoring) {
        super(id, misfire, ignition, components, fuelSystem, createdAt, pass);
        this.NMHCCatalyst = NMHCCatalyst;
        this.components = components1;
        this.NOxSCRMonitor = NOxSCRMonitor;
        this.boostPressure = boostPressure;
        this.reserved1 = reserved1;
        this.reserved2 = reserved2;
        this.exhaustSensor = exhaustSensor;
        this.PMFilterMonitoring = PMFilterMonitoring;
    }

    public PetrolEmissionsReport(int id, int vhrId, String misfire, String ignition
            , String components, String fuelSystem, Date createdAt, boolean pass
            , String NMHCCatalyst, String components1, String NOxSCRMonitor, String boostPressure
            , String reserved1, String reserved2, String exhaustSensor, String PMFilterMonitoring) {
        super(id, vhrId, misfire, ignition, components, fuelSystem, createdAt, pass);
        this.NMHCCatalyst = NMHCCatalyst;
        this.components = components1;
        this.NOxSCRMonitor = NOxSCRMonitor;
        this.boostPressure = boostPressure;
        this.reserved1 = reserved1;
        this.reserved2 = reserved2;
        this.exhaustSensor = exhaustSensor;
        this.PMFilterMonitoring = PMFilterMonitoring;
    }

    protected PetrolEmissionsReport(Parcel in) {
        super(in);
        NMHCCatalyst = in.readString();
        components = in.readString();
        NOxSCRMonitor = in.readString();
        boostPressure = in.readString();
        reserved1 = in.readString();
        reserved2 = in.readString();
        exhaustSensor = in.readString();
        PMFilterMonitoring = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(NMHCCatalyst);
        dest.writeString(components);
        dest.writeString(NOxSCRMonitor);
        dest.writeString(boostPressure);
        dest.writeString(reserved1);
        dest.writeString(reserved2);
        dest.writeString(exhaustSensor);
        dest.writeString(PMFilterMonitoring);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PetrolEmissionsReport> CREATOR = new Creator<PetrolEmissionsReport>() {
        @Override
        public PetrolEmissionsReport createFromParcel(Parcel in) {
            return new PetrolEmissionsReport(in);
        }

        @Override
        public PetrolEmissionsReport[] newArray(int size) {
            return new PetrolEmissionsReport[size];
        }
    };

    public String getNMHCCatalyst() {
        return NMHCCatalyst;
    }

    public void setNMHCCatalyst(String NMHCCatalyst) {
        this.NMHCCatalyst = NMHCCatalyst;
    }

    @Override
    public String getComponents() {
        return components;
    }

    @Override
    public void setComponents(String components) {
        this.components = components;
    }

    public String getNOxSCRMonitor() {
        return NOxSCRMonitor;
    }

    public void setNOxSCRMonitor(String NOxSCRMonitor) {
        this.NOxSCRMonitor = NOxSCRMonitor;
    }

    public String getBoostPressure() {
        return boostPressure;
    }

    public void setBoostPressure(String boostPressure) {
        this.boostPressure = boostPressure;
    }

    public String getReserved1() {
        return reserved1;
    }

    public void setReserved1(String reserved1) {
        this.reserved1 = reserved1;
    }

    public String getReserved2() {
        return reserved2;
    }

    public void setReserved2(String reserved2) {
        this.reserved2 = reserved2;
    }

    public String getExhaustSensor() {
        return exhaustSensor;
    }

    public void setExhaustSensor(String exhaustSensor) {
        this.exhaustSensor = exhaustSensor;
    }

    public String getPMFilterMonitoring() {
        return PMFilterMonitoring;
    }

    public void setPMFilterMonitoring(String PMFilterMonitoring) {
        this.PMFilterMonitoring = PMFilterMonitoring;
    }
}
