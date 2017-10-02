package com.pitstop.models.report;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Karol Zdebel on 10/1/2017.
 */

public class FullReport implements Parcelable{

    private VehicleHealthReport vehicleHealthReport;
    private EmissionsReport emissionsReport;

    public FullReport(VehicleHealthReport vehicleHealthReport, EmissionsReport emissionsReport) {
        this.vehicleHealthReport = vehicleHealthReport;
        this.emissionsReport = emissionsReport;
    }

    protected FullReport(Parcel in) {
        vehicleHealthReport = in.readParcelable(VehicleHealthReport.class.getClassLoader());
        emissionsReport = in.readParcelable(EmissionsReport.class.getClassLoader());
    }

    public static final Creator<FullReport> CREATOR = new Creator<FullReport>() {
        @Override
        public FullReport createFromParcel(Parcel in) {
            return new FullReport(in);
        }

        @Override
        public FullReport[] newArray(int size) {
            return new FullReport[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(vehicleHealthReport, i);
        parcel.writeParcelable(emissionsReport, i);
    }

    public VehicleHealthReport getVehicleHealthReport() {
        return vehicleHealthReport;
    }

    public void setVehicleHealthReport(VehicleHealthReport vehicleHealthReport) {
        this.vehicleHealthReport = vehicleHealthReport;
    }

    public EmissionsReport getEmissionsReport() {
        return emissionsReport;
    }

    public void setEmissionsReport(EmissionsReport emissionsReport) {
        this.emissionsReport = emissionsReport;
    }
}
