package com.pitstop.ui.Notifications;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.models.Notification;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by ishan on 2017-09-08.
 */

public class NotificationsPresenter extends TabPresenter <NotificationView>{

    private final String TAG = getClass().getSimpleName();
    private NotificationView notificationView;
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_NOTIFICATIONS);

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    private boolean updating = false;

    public NotificationsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    @Override
    public void subscribe(NotificationView view) {
        this.notificationView = view;
        setNoUpdateOnEventTypes(getIgnoredEventTypes());
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void unsubscribe() {
        super.unsubscribe();
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        Log.d(TAG,"getIgnoredEventTypes()");
        return new EventType[0];
    }

    @Override
    public void onAppStateChanged() {
        Log.d(TAG,"onAppStateChanged()");
        onUpdateNeeded();

    }

    @Override
    public EventSource getSourceType() {
        Log.d(TAG,"getSourceType()");
        return EVENT_SOURCE;
    }

    public void onRefresh(){
        Log.d(TAG, "onRefresh()");
        if (updating && notificationView.isRefreshing() && notificationView != null){
            notificationView.hideRefreshing();
        }else{
            onUpdateNeeded();
        }

    }

    public void onUpdateNeeded(){
        Log.d(TAG, "onUpdateNeeded");
        if (notificationView == null || updating) {
            Log.d("notification", "notificationviewisnull");
            return;
        }
        updating = true;
        notificationView.showLoading();
        useCaseComponent.getUserNotificationUseCase().execute(new GetUserNotificationUseCase.Callback() {
            @Override
            public void onNotificationsRetrieved(List<Notification> list) {
                updating = false;
                if (notificationView == null){
                    Log.d("notifications", "return");
                    return;}

                notificationView.hideLoading();
                if (list.size() == 0) {
                    notificationView.noNotifications();
                    Log.d("notifications", "zerolist");

                }
                else {
                    Log.d("notifications", "display");
                    notificationView.displayNotifications(list);
                }

            }
            @Override
            public void onError(RequestError error) {
                updating = false;
                if (notificationView == null) return;

                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    if (notificationView.hasBeenPopulated()){
                        notificationView.displayOfflineErrorDialog();
                    }
                    else {
                        notificationView.displayOfflineView();
                    }
                }
                else if (error.getError().equals(RequestError.ERR_UNKNOWN)){
                    if (notificationView.hasBeenPopulated()){
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
        else if (title.toLowerCase().contains("vehicle health update")){
            notificationView.openScanTab();
        }
    }

    public int getImageResource(String title) {
        if (notificationView == null) return 0;
        if (title.toLowerCase().contains("new vehicle issues")) {
            return R.drawable.notification_default_3x;
        }
        else if (title.toLowerCase().contains("service appointment reminder"))
            return R.drawable.request_service_dashboard_3x;
        else if (title.toLowerCase().contains("vehicle health update")){
            return R.drawable.scan_notification_3x;
        }
        return 0;

    }
}
