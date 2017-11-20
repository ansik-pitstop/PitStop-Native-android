package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.Trip215;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Device215TripRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.utils.Logger;

/**
 * Created by Karol Zdebel on 7/19/2017.
 */

public class GetPrevIgnitionTimeUseCaseImpl implements GetPrevIgnitionTimeUseCase {

    private final String TAG = getClass().getSimpleName();

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
        Logger.getInstance().logI(TAG, "Use case started execution: device215FullName="+device215Fullname
                , false, DebugMessage.TYPE_USE_CASE);
        this.device215FullName = device215Fullname;
        this.callback = callback;

        useCaseHandler.post(this);
    }

    private void onGotIgnitionTime(long ignitionTime){
        Logger.getInstance().logI(TAG, "Use case finished: ignitionTime="+ignitionTime
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onGotIgnitionTime(ignitionTime));
    }
    private void onNoneExists(){
        Logger.getInstance().logI(TAG, "Use case finished: none exists!"
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onNoneExists());
    }
    private void onError(RequestError error){
        Logger.getInstance().logI(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
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
