package com.pitstop.ui.Notifications;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.models.Notification;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

import static com.pitstop.R.id.view;

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
        Log.d(TAG, "refreshed");
        onUpdateNeeded();


    }

    public void onUpdateNeeded(){
       notificationView.showLoading();
        Log.d(TAG, "onUpdateNeeded");
        useCaseComponent.getUserNotificationUseCase().execute(new GetUserNotificationUseCase.Callback() {
            @Override
            public void onNotificationsRetrieved(List<Notification> list) {
                notificationView.hideLoading();
                if (list.size() == 0)
                    notificationView.noNotifications();
                else {
                    notificationView.displayNotifications(list);
                    Log.d("NotificationsRetrieved", Integer.toString(list.size()));
                }

            }
            @Override
            public void onError(RequestError error) {

                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    if (notificationView.hasBeenPoppulated()){
                        notificationView.displayOfflineErrorDialog();
                    }
                    else {
                        notificationView.displayOfflineErrorView();
                    }
                }
                else if (error.getError().equals(RequestError.ERR_UNKNOWN)){
                    if (notificationView.hasBeenPoppulated()){
                        notificationView.displayUnknownErrorDialog();
                    }
                    else {
                        notificationView.displayUnknownErrorView();
                    }
                }
                notificationView.hideLoading();
                Log.d("NotificationsError", "Error");
            }
        });
    }
}
