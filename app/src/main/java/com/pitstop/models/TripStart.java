package com.pitstop.models;

import android.content.Context;

import com.pitstop.network.RequestCallback;
import com.pitstop.utils.NetworkHelper;

/**
 * Created by Ben Wu on 2016-07-07.
 */

public class TripStart extends TripIndicator {

    private String scannerId;

    public TripStart(long tripId, String rtcTime, String scannerId) {
        super(tripId, rtcTime);
        this.scannerId = scannerId;
    }

    public String getScannerId() {
        return scannerId;
    }

    @Override
    public void execute(Context context, RequestCallback callback) {
        new NetworkHelper(context).sendTripStart(scannerId, rtcTime,
                tripId == -1 ? "0" : String.valueOf(tripId), callback);
    }
}
