package com.pitstop.model;

import android.content.Context;

import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Ben Wu on 2016-07-07.
 */

public class TripEnd extends TripIndicator {

    private String mileage;

    public TripEnd(int tripId, String rtcTime, String mileage) {
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
