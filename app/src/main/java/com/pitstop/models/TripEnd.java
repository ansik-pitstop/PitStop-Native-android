package com.pitstop.models;

import android.content.Context;

import com.pitstop.network.RequestCallback;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Ben Wu on 2016-07-07.
 */

public class TripEnd extends TripIndicator {

    private String mileage;

    public TripEnd(long tripId, String rtcTime, String mileage) {
        super(tripId, rtcTime);
        this.mileage = mileage;
    }

    public String getMileage() {
        return mileage;
    }

    @Override
    public void execute(Context context, RequestCallback callback) {
        new NetworkHelper(context).saveTripMileage(tripId, mileage, rtcTime, callback);
    }
}
