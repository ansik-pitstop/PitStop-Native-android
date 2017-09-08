package com.pitstop.interactors.get;

import android.os.Handler;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.pitstop.interactors.add.AddServiceUseCaseImpl;
import com.pitstop.models.Notification;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import java.util.List;


/**
 * Created by ishan on 2017-09-06.
 */


public class GetUserNotificationUseCaseImpl implements GetUserNotificationUseCase {

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
        this.callback = callback;
        useCaseHandler.post(this);
    }

    public void onError(RequestError error){

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    public void onNotificationsRetrieved(List<Notification> notificationList){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onNotificationsRetrieved(notificationList);
            }
        });
    }

    @Override
    public void run() {
        userRepository.getCurrentUser(new Repository.Callback<User>() {
            @Override
            public void onSuccess(User data) {
                Log.d("userNotif", data.getInstallationID().toString());
                List<String> userInstallationIds = data.getInstallationID();
                /*userInstallationIds = new Gson().fromJson(,  new TypeToken<List<String>>() {
                }.getType());*/
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





