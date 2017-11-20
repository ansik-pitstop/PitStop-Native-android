package com.pitstop.interactors.get;

import android.os.Handler;

import com.parse.FindCallback;
import com.parse.ParseException;
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
                , false, DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        useCaseHandler.post(this);
    }

    public void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    public void onNotificationsRetrieved(List<Notification> notificationList){
        Logger.getInstance().logI(TAG, "Use case finished: notificationList="+notificationList
                , false, DebugMessage.TYPE_USE_CASE);
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
                parseQuery.findInBackground(new FindCallback<Notification>() {
                    @Override
                    public void done(List<Notification> notificationsList, ParseException e) {
                        GetUserNotificationUseCaseImpl.this.onNotificationsRetrieved(notificationsList);
                    }
                });
            }
            @Override
            public void onError(RequestError error) {
                GetUserNotificationUseCaseImpl.this.onError(error);
            }
        });

    }
}





