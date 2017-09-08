package com.pitstop.ui.Notifications;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.models.Notification;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

/**
 * Created by ishan on 2017-09-08.
 */

public class NotificationsPresenter {

    private final String TAG = getClass().getSimpleName();
    private NotificationView notificationView;

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    public NotificationsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;

    }

    public void subscribe(NotificationView view) {
        this.notificationView = view;
        Log.d(TAG, "subscribed");
    }

    public void unsubscribe(){
        Log.d(TAG, "unsubscribed");
        this.notificationView = null;
    }

    public void onRefresh(){
        Log.d("notification Presenter", "refreshed");
        onUpdateNeeded();

    }

    public void onUpdateNeeded(){
        Log.d(TAG, "onUpdateNeeded");
        useCaseComponent.getUserNotificationUseCase().execute(new GetUserNotificationUseCase.Callback() {
            @Override
            public void onNotificationsRetrieved(List<Notification> list) {
                if (list.size() == 0)
                    notificationView.noNotifications();
                else {
                    notificationView.displayNotifications(list);
                    Log.d("NotificationsRetrieved", Integer.toString(list.size()));
                    Log.d("NotificationsRetrieved", list.get(0).getTitle());
                }
            }
            @Override
            public void onError(RequestError error) {
                Log.d("NotificationsError", "Error");

            }
        });


    }


}
