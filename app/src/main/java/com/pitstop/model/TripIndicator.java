package com.pitstop.model;

import android.content.Context;

import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;

/**
 * Created by Ben Wu on 2016-07-07.
 */

public abstract class TripIndicator {

    int tripId;
    String rtcTime;

    TripIndicator(int tripId, String rtcTime) {
        this.tripId = tripId;
        this.rtcTime = rtcTime;
    }

    public String getRtcTime() {
        return rtcTime;
    }

    public int getTripId() {
        return tripId;
    }

    public abstract void execute(Context context, RequestCallback callback);
}
