package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by David C. on 9/3/18.
 */

public class Trip {

    @SerializedName("_id")
    @Expose
    private int id;
    @SerializedName("tripId")
    @Expose
    private String tripId;
    @SerializedName("locationStart")
    @Expose
    private Location locationStart;
    @SerializedName("locationEnd")
    @Expose
    private Location locationEnd;
    @SerializedName("mileageStart")
    @Expose
    private int mileageStart;
    @SerializedName("mileageAccum")
    @Expose
    private int mileageAccum;
    @SerializedName("fuelConsumptionAccum")
    @Expose
    private int fuelConsumptionAccum;
    @SerializedName("fuelConsumptionStart")
    @Expose
    private int fuelConsumptionStart;
    @SerializedName("timeStart")
    @Expose
    private String timeStart;
    @SerializedName("timeEnd")
    @Expose
    private String timeEnd;
    @SerializedName("vin")
    @Expose
    private String vin;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public Location getLocationStart() {
        return locationStart;
    }

    public void setLocationStart(Location locationStart) {
        this.locationStart = locationStart;
    }

    public Location getLocationEnd() {
        return locationEnd;
    }

    public void setLocationEnd(Location locationEnd) {
        this.locationEnd = locationEnd;
    }

    public int getMileageStart() {
        return mileageStart;
    }

    public void setMileageStart(int mileageStart) {
        this.mileageStart = mileageStart;
    }

    public int getMileageAccum() {
        return mileageAccum;
    }

    public void setMileageAccum(int mileageAccum) {
        this.mileageAccum = mileageAccum;
    }

    public int getFuelConsumptionAccum() {
        return fuelConsumptionAccum;
    }

    public void setFuelConsumptionAccum(int fuelConsumptionAccum) {
        this.fuelConsumptionAccum = fuelConsumptionAccum;
    }

    public int getFuelConsumptionStart() {
        return fuelConsumptionStart;
    }

    public void setFuelConsumptionStart(int fuelConsumptionStart) {
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

}
