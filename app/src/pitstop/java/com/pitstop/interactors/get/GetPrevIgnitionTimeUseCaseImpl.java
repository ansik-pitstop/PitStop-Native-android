package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Trip215;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 7/19/2017.
 */

public class GetPrevIgnitionTimeUseCaseImpl implements GetPrevIgnitionTimeUseCase {

    private Device215TripRepository device215TripRepository;
    private String device215FullName;
    private Callback callback;
    private Handler handler;

    public GetPrevIgnitionTimeUseCaseImpl(Device215TripRepository device215TripRepository
            , Handler handler){
        this.device215TripRepository = device215TripRepository;
        this.handler = handler;
    }

    @Override
    public void execute(String device215Fullname, Callback callback) {
        this.device215FullName = device215Fullname;
        this.callback = callback;

        handler.post(this);
    }

    private String getScannerName(String device215FullName){
        return device215FullName.replace("IDD-","").replace("215B ","215B");
    }

    @Override
    public void run() {

        String scannerName = getScannerName(device215FullName); //Convert device name to scanner name

        device215TripRepository.retrieveLatestTrip(scannerName, new Repository.Callback<Trip215>() {

            @Override
            public void onSuccess(Trip215 data) {

                if (data == null){
                    callback.onNoneExists();
                    return;
                }

                //TripIdRaw = IgnitionTime just different names
                // , this probably isn't the best way of doing this
                callback.onGotIgnitionTime(data.getTripIdRaw());
            }

            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });
    }
}
