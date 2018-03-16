package com.pitstop.ui.trip;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.trip.Trip;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.ui.trip.list.TripListPresenter;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by David C. on 10/3/18.
 */

public class TripsPresenter extends TabPresenter<TripsView> implements TripListPresenter.OnChildPresenterInteractorListener {

    //private List<Trip> tripList = new ArrayList<>();

    private final String TAG = getClass().getSimpleName();
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_TRIPS);

    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY),
            new EventTypeImpl(EventType.EVENT_MILEAGE),
            new EventTypeImpl(EventType.EVENT_SCANNER),
            new EventTypeImpl(EventType.EVENT_SERVICES_NEW),
            new EventTypeImpl(EventType.EVENT_CAR_DEALERSHIP),
            new EventTypeImpl(EventType.EVENT_DTC_NEW)
    };

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    private boolean updating = false;

    public TripsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
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
        getView().requestForDataUpdate();
        //onUpdateNeeded();

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
            getView().requestForDataUpdate();
            //onUpdateNeeded();
        }

    }

//    public void loadView() {
//
//        Log.d(TAG,"loadView()");
//
//        useCaseComponent.getTripsUseCase().execute("WVWXK73C37E116278", new GetTripsUseCase.Callback() {
//            @Override
//            public void onTripsRetrieved(@NotNull List<? extends Trip> tripList, boolean isLocal) {
//
//                if (getView() != null) {
//                    getView().displayTripList((List<Trip>) tripList);
//                }
//
//            }
//
//            @Override
//            public void onError(@NotNull RequestError error) {
//
//                Log.d(TAG,"loadView().onError(): " + error);
//
//            }
//        });
//
//    }

//    public void onTripClicked(Trip trip) {

//        String pushType = n.getPushType();
//        Log.d(TAG, "onNotificationClicked() pushType:" +pushType+", title: "+n.getTitle());
//        mixpanelHelper.trackItemTapped(MixpanelHelper.NOTIFICATION, pushType);
//        useCaseComponent.getSetNotificationReadUseCase().execute(notifications, true, () -> {
//            if (getView() != null){
//                getView().onReadStatusChanged();
//                getView().displayBadgeCount(0);
//            }
//            Log.d(TAG,"setNotificationUseCase.success()");
//        });
//        if (getView() == null) return;
//        if (convertPushType(pushType).equalsIgnoreCase("serviceUpdate"))
//            getView().openCurrentServices();
//        else if (convertPushType(pushType).equalsIgnoreCase("scanReminder"))
//            getView().openScanTab();
//        else if (convertPushType(pushType).equalsIgnoreCase("serviceRequest"))
//            getView().openRequestService();
//
//        mixpanelHelper.trackItemTapped(MixpanelHelper.TRIP, trip.getTripId());
//
//        if (getView() == null || trip.getLocationPolyline() == null ) return;
//
//        getView().displayTripPolylineOnMap(trip.getLocationPolyline());
//
//    }

    @Override
    public void noTrips() {

        if (getView() != null) {

            getView().noTrips();

        }

    }

    @Override
    public void thereAreTrips() {

        if (getView() != null) {

            getView().thereAreTrips();

        }

    }

    @Override
    public void showTripOnMap(Trip trip) {
        // TODO:
        mixpanelHelper.trackItemTapped(MixpanelHelper.TRIP, trip.getTripId());

        if (getView() == null || trip.getLocationPolyline() == null ) return;

        getView().displayTripPolylineOnMap(trip.getLocationPolyline());
    }

    @Override
    public void showTripDetail(Trip trip) {

        mixpanelHelper.trackItemTapped(MixpanelHelper.TRIP, trip.getTripId());

        if (getView() == null) return;

        getView().displayTripDetailsView(trip);

    }

    @Override
    public void onShowLoading() {

        if (getView() != null) {
            getView().showLoading();
        }

    }

    @Override
    public void onHideLoading() {

        if (getView() != null) {
            getView().hideLoading();
        }

    }
}
