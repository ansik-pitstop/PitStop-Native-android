package com.pitstop.ui.trip;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetSnapToRoadUseCase;
import com.pitstop.models.snapToRoad.SnappedPoint;
import com.pitstop.models.trip.Trip;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.ui.trip.detail.TripDetailPresenter;
import com.pitstop.ui.trip.list.TripListPresenter;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.TripUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by David C. on 10/3/18.
 */

public class TripsPresenter extends TabPresenter<TripsView> implements
        TripListPresenter.OnListChildPresenterInteractorListener, TripDetailPresenter.OnDetailChildPresenterInteractorListener {

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

    }

    @Override
    public EventSource getSourceType() {
        Log.d(TAG, "getSourceType()");
        return EVENT_SOURCE;
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh()");
        if (!updating && getView() != null) {
            updating = true;
            getView().requestForDataUpdate();
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
    public void showTripOnMap(Trip trip, String interpolate, String apiKey) {

        mixpanelHelper.trackItemTapped(MixpanelHelper.TRIP, trip.getTripId());

        if (trip.getLocationPolyline() == null) {
            return;
        }

        useCaseComponent.getSnapToRoadUseCase().execute(TripUtils.Companion.polylineToLocationList(
                trip.getLocationPolyline())
                , new GetSnapToRoadUseCase.Callback() {
                    @Override
                    public void onError(@NotNull RequestError error) {

                        updating = false;
                        if (getView() == null) return;

                        if (error.getError().equals(RequestError.ERR_OFFLINE)) {
                            getView().showToast(R.string.polyline_error_offline_message);

                        } else if (error.getError().equals(RequestError.ERR_UNKNOWN)) {
                            getView().showToast(R.string.polyline_error_message);
                        }
                        getView().hideLoading();

                    }

                    @Override
                    public void onSnapToRoadRetrieved(@NotNull List<? extends SnappedPoint> snappedPointList) {

                        LatLng startCoord = null, endCoord = null;
                        if (trip.getLocationStart() != null && trip.getLocationStart().getLatitude() != null && trip.getLocationStart().getLongitude() != null) {
                            startCoord = new LatLng(Double.parseDouble(trip.getLocationStart().getLatitude()), Double.parseDouble(trip.getLocationStart().getLongitude()));
                        }

                        if (trip.getLocationEnd() != null && trip.getLocationEnd().getLatitude() != null && trip.getLocationEnd().getLongitude() != null) {
                            endCoord = new LatLng(Double.parseDouble(trip.getLocationEnd().getLatitude()), Double.parseDouble(trip.getLocationEnd().getLongitude()));
                        }

                        sendPolylineToMap(startCoord, endCoord, TripUtils.Companion.snappedPointListToPolylineOptions((List<SnappedPoint>) snappedPointList));

                    }
                });

    }

    @Override
    public void showTripDetail(Trip trip) {

        mixpanelHelper.trackItemTapped(MixpanelHelper.TRIP, trip.getTripId());

        if (getView() == null) return;

        if (updating) {
            getView().showToast(R.string.wait_loading_finish);

            return;
        }

        getView().displayTripDetailsView(trip);

    }

    @Override
    public void onShowLoading() {

        if (getView() != null) {
            updating = true;
            getView().showLoading();
        }

    }

    @Override
    public void onHideLoading() {

        if (getView() != null) {
            updating = false;
            getView().hideLoading();
        }

    }

    private void sendPolylineToMap(LatLng startCoord, LatLng endCoord, PolylineOptions polylineOptions) {

        if (getView() == null || polylineOptions == null) return;

        getView().displayTripPolylineOnMap(startCoord, endCoord, polylineOptions);

    }

    @Override
    public void onTripRemoved() {

        if (getView() == null) return;

        getView().requestForDataUpdate(); // Update the List children to stop showing the removed item

        getView().clearMap(); // Once the trip has been removed, let's clear the old polyline on Map

    }
}
