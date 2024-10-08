package com.pitstop.models;

import android.content.Context;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.network.RequestCallback;

/**
 * Created by Ben Wu on 2016-07-07.
 */

public class TripStart extends TripIndicator {

    private String scannerId;

    public TripStart(int tripId, String rtcTime, String scannerId) {
        super(tripId, rtcTime);
        this.scannerId = scannerId;
    }

    public String getScannerId() {
        return scannerId;
    }

    @Override
    public void execute(Context context, RequestCallback callback) {
        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        tempNetworkComponent.networkHelper().sendTripStart(scannerId, rtcTime,
                tripId == -1 ? "0" : String.valueOf(tripId), callback);
    }
}
