package com.pitstop.interactors.add;

import com.pitstop.interactors.Interactor;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.models.Notification;
import com.pitstop.network.RequestError;

import java.util.List;

/**
 * Created by ishan on 2017-09-26.
 */

public interface AddLicensePlateUseCase extends Interactor{

    interface Callback{
        void onError(RequestError error);
        void onLicensePlateStored(String licensePlate);
    }
    public void execute(int carid, String plate, AddLicensePlateUseCase.Callback callback);
}
