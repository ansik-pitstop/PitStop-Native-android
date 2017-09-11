package com.pitstop.ui.Notifications;

import com.pitstop.models.Notification;

import java.util.List;

/**
 * Created by ishan on 2017-09-08.
 */

public interface NotificationView {
    void displayNotifications(List<Notification> list);
    void noNotifications();
    void showLoading();
    void hideLoading();
    void displayOfflineErrorDialog();
    void displayOfflineErrorView();
    void displayUnknownErrorDialog();
    void displayUnknownErrorView();

    boolean hasBeenPoppulated();


}
