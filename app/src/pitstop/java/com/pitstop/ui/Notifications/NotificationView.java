package com.pitstop.ui.Notifications;

import com.pitstop.models.Notification;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;

import java.util.List;

/**
 * Created by ishan on 2017-09-08.
 */

public interface NotificationView  extends LoadingTabView, ErrorHandlingView{
    void displayNotifications(List<Notification> list);
    void noNotifications();
    void onNotificationClicked(String title );
    void openCurrentServices();
    void openAppointments();
    void openScanTab();
    boolean hasBeenPopulated();
    int changeimage(String title);

    void openRequestService();
}
