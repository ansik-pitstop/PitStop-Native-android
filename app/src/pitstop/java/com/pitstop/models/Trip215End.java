package com.pitstop.models;

/**
 * Created by Karol Zdebel on 8/25/2017.
 */

public class Trip215End extends Trip215 {
    private int tripId;

    public Trip215End(int tripId, long tripIdRaw, String scannerName
            , long rtcTime, double mileage) {
        super(mileage, rtcTime, scannerName, tripIdRaw);
        this.tripId = tripId;
    }

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }
}
