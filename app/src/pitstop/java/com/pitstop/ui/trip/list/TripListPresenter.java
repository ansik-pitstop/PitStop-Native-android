package com.pitstop.ui.trip.list;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetTripsUseCase;
import com.pitstop.models.trip.Trip;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by David C. on 14/3/18.
 */

public class TripListPresenter extends TabPresenter<TripListView> {

    public interface OnChildPresenterInteractorListener {

        void noTrips();

        void thereAreTrips();

        void showTripOnMap(Trip trip);

        void showTripDetail(Trip trip);

        void onShowLoading();

        void onHideLoading();

    }

    private final String TAG = getClass().getSimpleName();

    private OnChildPresenterInteractorListener mParentListener;

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

//    public void loadView(String carVin) {
//
//        Log.d(TAG, "loadView()");
//
//        useCaseComponent.getTripsUseCase().execute(carVin, new GetTripsUseCase.Callback() {
//            @Override
//            public void onTripsRetrieved(@NotNull List<? extends Trip> tripList, boolean isLocal) {
//
//                if (getView() == null) {
//                    return;
//                }
//
//                if (tripList != null && tripList.size() > 0) {
//                    getView().displayTripList((List<Trip>) tripList);
//                } else {
//                    mParentListener.noTrips();
//                }
//
//            }
//
//            @Override
//            public void onError(@NotNull RequestError error) {
//
//                Log.d(TAG, "loadView().onError(): " + error);
//
//            }
//        });
//
//    }

    public void setCommunicationInteractor(OnChildPresenterInteractorListener onChildPresenterInteractorListener) {

        this.mParentListener = onChildPresenterInteractorListener;

    }

    public void sentOnShowLoading() {

        mParentListener.onShowLoading();

    }

    public void sendOnHideLoading() {

        mParentListener.onHideLoading();

    }

    public void onTripRowClicked(Trip trip) {

        mParentListener.showTripOnMap(trip);

    }

    public void onTripInfoClicked(Trip trip) {

        mParentListener.showTripDetail(trip);

    }

    void onRefresh() {
        Log.d(TAG, "onRefresh()");

        if (getView() != null && getView().isRefreshing() && updating) {
            getView().hideRefreshing();
        } else {
            onUpdateNeeded();
        }

    }

    public void onUpdateNeeded(){
        Log.d(TAG, "onUpdateNeeded, TripListPresenter");
        if (getView() == null || updating) {
            return;
        }
        updating = true;
        getView().showLoading();

        useCaseComponent.getTripsUseCase().execute(new GetTripsUseCase.Callback() {
            @Override
            public void onTripsRetrieved(@NotNull List<? extends Trip> tripList, boolean isLocal) {

                Log.d(TAG,"onTripListRetrieved() trips: " + tripList);
                updating = false;
                if (getView() == null){
                    Log.d("trips", "return");
                    return;
                }

                getView().hideLoading();
                if (tripList == null){
                    getView().displayUnknownErrorView();
                    //getView().displayBadgeCount(0);
                    return;
                }
                else if (tripList.size() == 0) {
                    notifyParentFragmentNoTrips();
                    Log.d("trips", "zerolist");
                }
                else {
                    Log.d("trips", "display");
                    mParentListener.thereAreTrips();
                    getView().displayTripList((List<Trip>) tripList);
                }

            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG, "getTripsUseCase().error() event fired: " + error);

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

    public void notifyParentFragmentNoTrips() {
        mParentListener.noTrips();
    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        return new EventType[0];
    }

    @Override
    public void onAppStateChanged() {

    }

    @Override
    public EventSource getSourceType() {
        return null;
    }

}
