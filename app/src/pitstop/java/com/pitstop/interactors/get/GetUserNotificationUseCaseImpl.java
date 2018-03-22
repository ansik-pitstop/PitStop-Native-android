package com.pitstop.interactors.get;

import android.os.Handler;

import com.parse.ParseQuery;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Notification;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import java.util.List;


/**
 * Created by ishan on 2017-09-06.
 */


public class GetUserNotificationUseCaseImpl implements GetUserNotificationUseCase {

    private final String TAG = getClass().getSimpleName();

    private UserRepository userRepository;
    private Callback callback;
    private Handler mainHandler;
    private Handler useCaseHandler;

    public GetUserNotificationUseCaseImpl( UserRepository userRepository, Handler mainHandler, Handler useCasehandler){

        this.userRepository = userRepository;
        this.mainHandler = mainHandler;
        this.useCaseHandler = useCasehandler;

    }
    @Override
    public void execute(Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        useCaseHandler.post(this);
    }

    public void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    public void onNotificationsRetrieved(List<Notification> notificationList){
        Logger.getInstance().logI(TAG, "Use case finished: notificationList.size="+notificationList.size()
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onNotificationsRetrieved(notificationList));
    }

    @Override
    public void run() {
        userRepository.getRemoteCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User data) {
                List<String> userInstallationIds = data.getInstallationID();
                ParseQuery<Notification> parseQuery = ParseQuery.getQuery("Notification");
                parseQuery.whereContainedIn("recipients", userInstallationIds);
                parseQuery.findInBackground((notificationsList, e) -> {
                    if (e == null)
                        GetUserNotificationUseCaseImpl.this.onNotificationsRetrieved(notificationsList);
                    else GetUserNotificationUseCaseImpl.this.onError(RequestError.getUnknownError());
                });
            }
            @Override
            public void onError(RequestError error) {
                GetUserNotificationUseCaseImpl.this.onError(error);
            }
        });

    }
}





