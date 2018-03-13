package com.pitstop.ui.trip;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetTripsUseCase;
import com.pitstop.interactors.get.GetUserNotificationUseCase;
import com.pitstop.models.Notification;
import com.pitstop.models.trip.Trip;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by David C. on 10/3/18.
 */

public class TripListPresenter extends TabPresenter<TripListView> {

    //private List<Trip> tripList = new ArrayList<>();

    private final String TAG = getClass().getSimpleName();
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_TRIPS);

    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY),
            new EventTypeImpl(EventType.EVENT_MILEAGE),
            new EventTypeImpl(EventType.EVENT_SCANNER),
            new EventTypeImpl(EventType.EVENT_SERVICES_NEW),
            new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY),
            new EventTypeImpl(EventType.EVENT_CAR_DEALERSHIP),
            new EventTypeImpl(EventType.EVENT_DTC_NEW)
    };

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    private boolean updating = false;

    public TripListPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        Log.d(TAG, "getIgnoredEventTypes()");
        return ignoredEvents;
    }

    @Override
    public void onAppStateChanged() {
        Log.d(TAG, "onAppStateChanged()");
        onUpdateNeeded();

    }

    @Override
    public EventSource getSourceType() {
        Log.d(TAG, "getSourceType()");
        return EVENT_SOURCE;
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh()");
        if (updating && getView().isRefreshing() && getView() != null) {
            getView().hideRefreshing();
        } else {
            onUpdateNeeded();
        }

    }

    public void loadView() {

        Log.d(TAG,"loadView()");

        useCaseComponent.getTripsUseCase().execute("WVWXK73C37E116278", new GetTripsUseCase.Callback() {
            @Override
            public void onTripsRetrieved(@NotNull List<? extends Trip> tripList, boolean isLocal) {

                if (getView() != null) {
                    getView().displayTripList((List<Trip>) tripList);
                }

            }

            @Override
            public void onError(@NotNull RequestError error) {

                Log.d(TAG,"loadView().onError(): " + error);

            }
        });

    }

    public void onUpdateNeeded() {
        Log.d(TAG, "onUpdateNeeded");
        if (getView() == null || updating) {
            return;
        }
        updating = true;
        getView().showLoading();
        useCaseComponent.getUserNotificationUseCase().execute(new GetUserNotificationUseCase.Callback() {
            @Override
            public void onNotificationsRetrieved(List<Notification> list) {
                Log.d(TAG, "onNotificationsRetrieved() notifs: " + list);
                updating = false;
                if (getView() == null) {
                    Log.d("notifications", "return");
                    return;
                }
/*
                getView().hideLoading();
                if (notifications == null) {
                    getView().displayUnknownErrorView();
                    getView().displayBadgeCount(0);
                    return;
                } else if (list.size() == 0) {
                    getView().noNotifications();
                    getView().displayBadgeCount(0);
                    Log.d("notifications", "zerolist");
                } else {
                    Log.d("notifications", "display");
                    notifications.clear();
                    notifications.addAll(list);
                    Collections.sort(notifications, (t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()));

                    int badgeCount = 0;
                    for (Notification n : notifications) {
                        if (n.isRead() != null && !n.isRead())
                            badgeCount++;
                    }

                    getView().displayBadgeCount(badgeCount);
                    getView().displayNotifications(notifications);
                }
*/
            }

            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
/*
                if (error.getError().equals(RequestError.ERR_OFFLINE)) {
                    if (getView().hasBeenPopulated()) {
                        getView().displayOfflineErrorDialog();
                    } else {
                        getView().displayOfflineView();
                    }
                } else if (error.getError().equals(RequestError.ERR_UNKNOWN)) {
                    if (getView().hasBeenPopulated()) {
                        getView().displayUnknownErrorDialog();
                    } else {
                        getView().displayUnknownErrorView();
                    }
                }
*/
                getView().hideLoading();
            }
        });
    }
}
