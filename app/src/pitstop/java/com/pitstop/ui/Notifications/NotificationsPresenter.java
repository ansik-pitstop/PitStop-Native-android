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

    private static final String SERVICE_APPOINTMENT_REMINDER = "service appointment reminder";
    private static final String NEW_VEHICLE_ISSUE = "new vehicle issues";
    private static final String VEHICLE_HEALTH_UPDATE = "vehicle health update";


    private final String TAG = getClass().getSimpleName();
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_NOTIFICATIONS);

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    private boolean updating = false;

    public NotificationsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
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
        if (updating && getView().isRefreshing() && getView() != null){
            getView().hideRefreshing();
        }else{
            onUpdateNeeded();
        }

    }

    public void onUpdateNeeded(){
        Log.d(TAG, "onUpdateNeeded");
        if (getView() == null || updating) {
            return;
        }
        updating = true;
        getView().showLoading();
        useCaseComponent.getUserNotificationUseCase().execute(new GetUserNotificationUseCase.Callback() {
            @Override
            public void onNotificationsRetrieved(List<Notification> list) {
                updating = false;
                if (getView() == null){
                    Log.d("notifications", "return");
                    return;}

                getView().hideLoading();
                if (list.size() == 0) {
                    getView().noNotifications();
                    Log.d("notifications", "zerolist");

                }
                else {
                    Log.d("notifications", "display");
                    getView().displayNotifications(list);
                }

            }
            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;

                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    if (getView().hasBeenPopulated()){
                        getView().displayOfflineErrorDialog();
                    }
                    else {
                        getView().displayOfflineView();
                    }
                }
                else if (error.getError().equals(RequestError.ERR_UNKNOWN)){
                    if (getView().hasBeenPopulated()){
                        getView().displayUnknownErrorDialog();
                    }
                    else {
                        getView().displayUnknownErrorView();
                    }
                }
                getView().hideLoading();
            }
        });
    }

    public void onNotificationClicked(String title) {
        Log.d(TAG, "NotificationClicked()" + title);
        if (getView() == null) return;
        mixpanelHelper.trackItemTapped(MixpanelHelper.NOTIFICATION, title);
        if (title.toLowerCase().contains(NEW_VEHICLE_ISSUE))
            getView().openCurrentServices();
        else if (title.toLowerCase().contains(SERVICE_APPOINTMENT_REMINDER))
            getView().openAppointments();
        else if (title.toLowerCase().contains(VEHICLE_HEALTH_UPDATE))
            getView().openScanTab();
    }

    public int getImageResource(String title) {
        if (getView() == null) return 0;
        if (title.toLowerCase().contains(SERVICE_APPOINTMENT_REMINDER))
            return R.drawable.request_service_dashboard_3x;
        else if (title.toLowerCase().contains(VEHICLE_HEALTH_UPDATE))
            return R.drawable.scan_notification_3x;
        else return R.drawable.notification_default_3x;

    }
}
