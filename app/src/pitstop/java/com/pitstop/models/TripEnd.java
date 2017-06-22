package com.pitstop.models;

import android.content.Context;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.network.RequestCallback;

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
        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        tempNetworkComponent.networkHelper().saveTripMileage(tripId, mileage, rtcTime, callback);
    }
}
