package com.pitstop.models;

/**
 * Created by Karol Zdebel on 8/25/2017.
 */

public class RetrievedTrip215 extends Trip215{

    private int tripId = -1;
    private boolean isEnded;

    public RetrievedTrip215(int tripId,long tripIdRaw, String scannerName, double mileage
            , long rtcTime, boolean isEnded) {
        super(mileage, rtcTime, scannerName, tripIdRaw);
        this.tripId = tripId;
        this.isEnded = isEnded;
    }

    public int getTripId() {
        return tripId;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public boolean isEnded() {
        return isEnded;
    }

    public void setEnded(boolean ended) {
        isEnded = ended;
    }
}
