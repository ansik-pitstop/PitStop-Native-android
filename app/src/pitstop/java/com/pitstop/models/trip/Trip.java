package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.List;

/**
 * Created by David C. on 9/3/18.
 */

public class Trip extends SugarRecord {

    @SerializedName("_id")
    @Expose
    private Long newId;

    @SerializedName("tripId")
    @Expose
    private String tripId;

    @SerializedName("locationStart")
    @Expose
    private LocationStart locationStart;

    @SerializedName("locationEnd")
    @Expose
    private LocationEnd locationEnd;

    @SerializedName("mileageStart")
    @Expose
    private double mileageStart;

    @SerializedName("mileageAccum")
    @Expose
    private double mileageAccum;

    @SerializedName("fuelConsumptionAccum")
    @Expose
    private double fuelConsumptionAccum;

    @SerializedName("fuelConsumptionStart")
    @Expose
    private double fuelConsumptionStart;

    @SerializedName("timeStart")
    @Expose
    private String timeStart;

    @SerializedName("timeEnd")
    @Expose
    private String timeEnd;

    @SerializedName("vin")
    @Expose
    private String vin;

    @Ignore
    @SerializedName("location_polyline")
    @Expose
    private List<LocationPolyline> locationPolyline;

    public Trip() {
    }

    public Trip(Long newId, String tripId, LocationStart locationStart, LocationEnd locationEnd, double mileageStart, double mileageAccum, double fuelConsumptionAccum, double fuelConsumptionStart, String timeStart, String timeEnd, String vin, List<LocationPolyline> locationPolyline) {
        this.newId = newId;
        this.tripId = tripId;
        this.locationStart = locationStart;
        this.locationEnd = locationEnd;
        this.mileageStart = mileageStart;
        this.mileageAccum = mileageAccum;
        this.fuelConsumptionAccum = fuelConsumptionAccum;
        this.fuelConsumptionStart = fuelConsumptionStart;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.vin = vin;
        this.locationPolyline = locationPolyline;
    }

    public Long getNewId() {
        return newId;
    }

    public void setNewId(Long newId) {
        this.newId = newId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public LocationStart getLocationStart() {
        return locationStart;
    }

    public void setLocationStart(LocationStart locationStart) {
        this.locationStart = locationStart;
    }

    public LocationEnd getLocationEnd() {
        return locationEnd;
    }

    public void setLocationEnd(LocationEnd locationEnd) {
        this.locationEnd = locationEnd;
    }

    public double getMileageStart() {
        return mileageStart;
    }

    public void setMileageStart(double mileageStart) {
        this.mileageStart = mileageStart;
    }

    public double getMileageAccum() {
        return mileageAccum;
    }

    public void setMileageAccum(double mileageAccum) {
        this.mileageAccum = mileageAccum;
    }

    public double getFuelConsumptionAccum() {
        return fuelConsumptionAccum;
    }

    public void setFuelConsumptionAccum(double fuelConsumptionAccum) {
        this.fuelConsumptionAccum = fuelConsumptionAccum;
    }

    public double getFuelConsumptionStart() {
        return fuelConsumptionStart;
    }

    public void setFuelConsumptionStart(double fuelConsumptionStart) {
        this.fuelConsumptionStart = fuelConsumptionStart;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public List<LocationPolyline> getLocationPolyline() {
        return locationPolyline;
    }

    public void setLocationPolyline(List<LocationPolyline> locationPolyline) {
        this.locationPolyline = locationPolyline;
    }
}
