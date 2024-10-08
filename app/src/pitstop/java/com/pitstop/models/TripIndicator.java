package com.pitstop.models;

import android.content.Context;

import com.pitstop.network.RequestCallback;

/**
 * Created by Ben Wu on 2016-07-07.
 */

public abstract class TripIndicator {

    long tripId;
    String rtcTime;

    TripIndicator(long tripId, String rtcTime) {
        this.tripId = tripId;
        this.rtcTime = rtcTime;
    }

    public String getRtcTime() {
        return rtcTime;
    }

    public long getTripId() {
        return tripId;
    }

    public abstract void execute(Context context, RequestCallback callback);
}
