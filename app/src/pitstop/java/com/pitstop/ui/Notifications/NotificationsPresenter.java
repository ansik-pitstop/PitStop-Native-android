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
        Log.d(TAG, "subscribe()");
    }

    public void unsubscribe(){
        Log.d(TAG, "unsubscribe()");
        this.notificationView = null;
    }

    public void onRefresh(){
        Log.d(TAG, "onRefresh()");
        onUpdateNeeded();

    }

    public void onUpdateNeeded(){
        Log.d(TAG, "onUpdateNeeded");
        if (notificationView == null) return;
        notificationView.showLoading();
        useCaseComponent.getUserNotificationUseCase().execute(new GetUserNotificationUseCase.Callback() {
            @Override
            public void onNotificationsRetrieved(List<Notification> list) {
                if (notificationView == null) return;
                notificationView.hideLoading();
                if (list.size() == 0)
                    notificationView.noNotifications();
                else {
                    notificationView.displayNotifications(list);
                }

            }
            @Override
            public void onError(RequestError error) {
                if (notificationView == null) return;
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
            }
        });
    }

    public void onNotificationClicked(String title) {
        if (notificationView == null) return;
        mixpanelHelper.trackItemTapped(MixpanelHelper.NOTIFICATION, title);

        if (title.toLowerCase().contains("new vehicle issues")) {
            notificationView.openCurrentServices();
        }
        else if (title.toLowerCase().contains("service appointment reminder"))
            notificationView.openAppointments();
        else if (title.equalsIgnoreCase("vehicle health update")){
            notificationView.openScanTab();
        }
    }

}
