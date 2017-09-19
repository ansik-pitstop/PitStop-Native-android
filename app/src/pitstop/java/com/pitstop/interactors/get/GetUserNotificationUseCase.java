package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Notification;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;

import java.util.List;

/**
 * Created by ishan on 2017-09-06.
 */

public interface GetUserNotificationUseCase extends Interactor{

    interface Callback{
        void onNotificationsRetrieved(List<Notification> list );
        void onError(RequestError error);
    }
    public void execute(Callback callback);
}
