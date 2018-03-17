package com.pitstop.ui.trip;

import android.util.Log;

import com.google.android.gms.maps.model.PolylineOptions;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetSnapToRoadUseCase;
import com.pitstop.models.snapToRoad.SnappedPoint;
import com.pitstop.models.trip.Trip;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.ui.trip.list.TripListPresenter;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.TripUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by David C. on 10/3/18.
 */

public class TripsPresenter extends TabPresenter<TripsView> implements TripListPresenter.OnChildPresenterInteractorListener {

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
            //getView().hideRefreshing();
        } else {
            getView().requestForDataUpdate();
            //onUpdateNeeded();
        }

    }

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

        mixpanelHelper.trackItemTapped(MixpanelHelper.TRIP, trip.getTripId());

        // Convert LocationPolyline's to LatLng's String
        String listLatLng = TripUtils.locationPolylineToLatLngString(trip.getLocationPolyline());

        useCaseComponent.getSnapToRoadUseCase().execute(listLatLng, "true", new GetSnapToRoadUseCase.Callback() {
            @Override
            public void onError(@NotNull RequestError error) {

                updating = false;
                if (getView() == null) return;

                if (error.getError().equals(RequestError.ERR_OFFLINE)){
//                    if (getView().hasBeenPopulated()){
                        getView().displayOfflineErrorDialog();
//                    }
//                    else {
//                        getView().displayOfflineView();
//                    }
                }
                else if (error.getError().equals(RequestError.ERR_UNKNOWN)){
//                    if (getView().hasBeenPopulated()){
                        getView().displayUnknownErrorDialog();
//                    }
//                    else {
//                        getView().displayUnknownErrorView();
//                    }
                }
                getView().hideLoading();

            }

            @Override
            public void onSnapToRoadRetrieved(@NotNull List<? extends SnappedPoint> snappedPointList) {

                sendPolylineToMap(TripUtils.snappedPointListToPolylineOptions((List<SnappedPoint>) snappedPointList));

            }
        });

    }

    @Override
    public void showTripDetail(Trip trip) {

        mixpanelHelper.trackItemTapped(MixpanelHelper.TRIP, trip.getTripId());

        if (getView() == null) return;

        showTripOnMap(trip);

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

    private void sendPolylineToMap(PolylineOptions polylineOptions) {

        if (getView() == null || polylineOptions == null ) return;

        getView().displayTripPolylineOnMap(polylineOptions);

    }
}
