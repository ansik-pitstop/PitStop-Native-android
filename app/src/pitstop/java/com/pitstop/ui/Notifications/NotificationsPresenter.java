package com.pitstop.ui.Notifications;

import android.util.Log;

import com.pitstop.BuildConfig;
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

    public void onNotificationClicked(String title) {

        if (BuildConfig.BUILD_TYPE == BuildConfig.BUILD_TYPE_RELEASE) {

            if (title.equalsIgnoreCase("New Vehicle Issues!")) {
                notificationView.openCurrentServices();
            }
            else if (title.equalsIgnoreCase("Service Appointment Reminder"))
                notificationView.openAppointments();
            else if (title.equalsIgnoreCase("Vehicle Health Update")){
                notificationView.openScanTab();
            }

        }
        else {
            if (title.equalsIgnoreCase("[staging] New Vehicle Issues!")) {
                notificationView.openCurrentServices();
            }
            else if (title.equalsIgnoreCase("[staging] Service Appointment Reminder"))
                notificationView.openAppointments();
            else if (title.equalsIgnoreCase("[staging] Vehicle Health Update")){
                notificationView.openScanTab();
            }
        }


    }


}
