package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by Karol Zdebel on 10/3/2017.
 */

public class DieselEmissionsReport extends EmissionsReport implements Parcelable{
    private String heatedCatalyst;
    private String catalyst;
    private String evap;
    private String secondaryAir;
    private String ACRefrigirator;
    private String O2Sensor;
    private String O2SensorHeater;
    private String EGR;

    public DieselEmissionsReport(int id, String misfire, String ignition, String components
            , String fuelSystem, Date createdAt, boolean pass, String heatedCatalyst
            , String catalyst, String evap, String secondaryAir, String ACRefrigirator
            , String o2Sensor, String o2SensorHeater, String EGR) {
        super(id, misfire, ignition, components, fuelSystem, createdAt, pass);
        this.heatedCatalyst = heatedCatalyst;
        this.catalyst = catalyst;
        this.evap = evap;
        this.secondaryAir = secondaryAir;
        this.ACRefrigirator = ACRefrigirator;
        O2Sensor = o2Sensor;
        O2SensorHeater = o2SensorHeater;
        this.EGR = EGR;
    }

    public DieselEmissionsReport(int id, int vhrId, String misfire, String ignition
            , String components, String fuelSystem, Date createdAt, boolean pass
            , String heatedCatalyst, String catalyst, String evap, String secondaryAir
            , String ACRefrigirator, String o2Sensor, String o2SensorHeater, String EGR) {
        super(id, vhrId, misfire, ignition, components, fuelSystem, createdAt, pass);
        this.heatedCatalyst = heatedCatalyst;
        this.catalyst = catalyst;
        this.evap = evap;
        this.secondaryAir = secondaryAir;
        this.ACRefrigirator = ACRefrigirator;
        O2Sensor = o2Sensor;
        O2SensorHeater = o2SensorHeater;
        this.EGR = EGR;
    }

    protected DieselEmissionsReport(Parcel in) {
        super(in);
        heatedCatalyst = in.readString();
        catalyst = in.readString();
        evap = in.readString();
        secondaryAir = in.readString();
        ACRefrigirator = in.readString();
        O2Sensor = in.readString();
        O2SensorHeater = in.readString();
        EGR = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(heatedCatalyst);
        dest.writeString(catalyst);
        dest.writeString(evap);
        dest.writeString(secondaryAir);
        dest.writeString(ACRefrigirator);
        dest.writeString(O2Sensor);
        dest.writeString(O2SensorHeater);
        dest.writeString(EGR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DieselEmissionsReport> CREATOR = new Creator<DieselEmissionsReport>() {
        @Override
        public DieselEmissionsReport createFromParcel(Parcel in) {
            return new DieselEmissionsReport(in);
        }

        @Override
        public DieselEmissionsReport[] newArray(int size) {
            return new DieselEmissionsReport[size];
        }
    };

    public String getHeatedCatalyst() {
        return heatedCatalyst;
    }

    public void setHeatedCatalyst(String heatedCatalyst) {
        this.heatedCatalyst = heatedCatalyst;
    }

    public String getCatalyst() {
        return catalyst;
    }

    public void setCatalyst(String catalyst) {
        this.catalyst = catalyst;
    }

    public String getEvap() {
        return evap;
    }

    public void setEvap(String evap) {
        this.evap = evap;
    }

    public String getSecondaryAir() {
        return secondaryAir;
    }

    public void setSecondaryAir(String secondaryAir) {
        this.secondaryAir = secondaryAir;
    }

    public String getACRefrigirator() {
        return ACRefrigirator;
    }

    public void setACRefrigirator(String ACRefrigirator) {
        this.ACRefrigirator = ACRefrigirator;
    }

    public String getO2Sensor() {
        return O2Sensor;
    }

    public void setO2Sensor(String o2Sensor) {
        O2Sensor = o2Sensor;
    }

    public String getO2SensorHeater() {
        return O2SensorHeater;
    }

    public void setO2SensorHeater(String o2SensorHeater) {
        O2SensorHeater = o2SensorHeater;
    }

    public String getEGR() {
        return EGR;
    }

    public void setEGR(String EGR) {
        this.EGR = EGR;
    }
}
