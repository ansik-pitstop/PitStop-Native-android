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
    private Handler useCaseHandler;
    private Handler mainHandler;

    public GetPrevIgnitionTimeUseCaseImpl(Device215TripRepository device215TripRepository
            , Handler useCaseHandler, Handler mainHandler){
        this.device215TripRepository = device215TripRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(String device215Fullname, Callback callback) {
        this.device215FullName = device215Fullname;
        this.callback = callback;

        useCaseHandler.post(this);
    }

    private void onGotIgnitionTime(long ignitionTime){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onGotIgnitionTime(ignitionTime);
            }
        });
    }
    private void onNoneExists(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onNoneExists();
            }
        });
    }
    private void onError(RequestError error){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
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
                    onNoneExists();
                    return;
                }

                //TripIdRaw = IgnitionTime just different names
                // , this probably isn't the best way of doing this
                onGotIgnitionTime(data.getTripIdRaw());
            }

            @Override
            public void onError(RequestError error) {
                GetPrevIgnitionTimeUseCaseImpl.this.onError(error);
            }
        });
    }
}
