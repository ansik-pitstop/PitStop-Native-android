package com.pitstop.models;

/**
 * Created by Karol Zdebel on 8/25/2017.
 */

public class Trip215Start extends Trip215 {
    public Trip215Start(long tripIdRaw, String scannerName, double mileage, long rtcTime) {
        super(mileage, rtcTime, scannerName, tripIdRaw);
    }
}
