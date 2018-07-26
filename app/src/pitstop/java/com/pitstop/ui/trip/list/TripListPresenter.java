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
import com.pitstop.ui.trip.TripActivityObserver;
import com.pitstop.utils.MixpanelHelper;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by David C. on 14/3/18.
 */

public class TripListPresenter extends TabPresenter<TripListView> implements TripActivityObserver {

    public interface OnListChildPresenterInteractorListener {

        void showTripOnMap(Trip trip, String interpolate, String apiKey);

        void showTripDetail(Trip trip);

        void onShowLoading();

        void onHideLoading();
    }

    private final String TAG = getClass().getSimpleName();

    private OnListChildPresenterInteractorListener mParentListener;

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
    private CompositeDisposable compositeDisposable;

    private boolean updating = false;
    private boolean carAdded = true;

    public TripListPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void onTripStart() {
        Log.d(TAG,"onTripStart()");
    }

    @Override
    public void onTripUpdate() {
        Log.d(TAG,"onTripUpdate()");
    }

    @Override
    public void onTripEnd() {
        Log.d(TAG,"onTripEnd()");
        if (getView() != null && carAdded){
            onUpdateNeeded(getView().getSortType());
        }
    }

    @Override
    public void subscribe(TripListView view) {
        super.subscribe(view);
        Disposable d = view.getManualTripController().subscribe(controller -> {
            Log.d(TAG,"Got manual trip controller");
            controller.getTripState().subscribe(state ->{
                Log.d(TAG,"Got new trip state: "+state);
                view.toggleRecordingButton(state);
            });
        });
        compositeDisposable.add(d);
    }

    @Override
    public void unsubscribe(){
        compositeDisposable.clear();
        super.unsubscribe();
    }

    public void setCommunicationInteractor(OnListChildPresenterInteractorListener onListChildPresenterInteractorListener) {

        this.mParentListener = onListChildPresenterInteractorListener;

    }

    public void sentOnShowLoading() {

        Log.d(TAG, "sentOnShowLoading()");

        mParentListener.onShowLoading();

    }

    public void sendOnHideLoading() {

        Log.d(TAG, "sendOnHideLoading()");

        mParentListener.onHideLoading();

    }

    public void onTripRowClicked(Trip trip, String interpolate, String apiKey) {

        mParentListener.showTripOnMap(trip, interpolate, apiKey);

    }

    public void onTripInfoClicked(Trip trip) {

        if (getView().isRefreshing()) {
            getView().showToastStillRefreshing();
        } else {
            mParentListener.showTripDetail(trip);
        }

    }

    public void onBottomListButtonClicked(){
        Log.d(TAG,"onBottomListButtonClicked()");
        if (getView() == null) return;

        getView().checkPermissions();

        if (!carAdded){
            getView().beginAddCar();
        }
    }

    public boolean isRefreshing() {
        return updating;
    }

    void onRefresh(int sortParam) {
        Log.d(TAG, "onRefresh()");

        if (getView() != null && getView().isRefreshing() && updating) {
            getView().hideRefreshing();
        } else {
            onUpdateNeeded(sortParam);
        }

    }

    public void onUpdateNeeded(int sortParam) {
        Log.d(TAG, "onUpdateNeeded, TripListPresenter");
        if (getView() == null || updating) {
            return;
        }
        updating = true;
        getView().showLoading();

        useCaseComponent.getTripsUseCase().execute(new GetTripsUseCase.Callback() {
            @Override
            public void onNoCar() {
                Log.d(TAG,"GetTripsUseCase.onNoCar()");
                carAdded = false;
                if (getView() != null){
                    getView().hideLoading();
                    getView().displayNoCar();
                }
                updating = false;
            }

            @Override
            public void onTripsRetrieved(@NotNull List<? extends Trip> tripList, boolean isLocal) {

                Log.d(TAG, "onTripListRetrieved() trips: " + tripList);

                carAdded = true;
                updating = false;
                if (getView() == null) {
                    Log.d("trips", "return");
                    return;
                }

                if (!isLocal) { // Only hide the spinner when the Remote call (the 2nd and last) is finished
                    updating = false;
                    getView().hideLoading();
                }
                if (tripList == null) {
                    updating = false;
                    getView().displayUnknownErrorView();
                    return;
                } else if (tripList.size() == 0) {
                    getView().displayNoTrips();
                    Log.d("trips", "zerolist");
                } else {
                    Log.d("trips", "display");
                    sortTripListBy((List<Trip>) tripList, sortParam);
                }

            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG, "getTripsUseCase().error() event fired: " + error);

                updating = false;
                if (getView() == null) return;

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
                getView().hideLoading();

            }
        });

    }

    public void sortTripListBy(List<Trip> tripList, int sortParam) {
        if (!carAdded) return;

        List<Trip> sortedTripList;

        switch (sortParam) {
            case 0:
                sortedTripList = sortTripsByDate(tripList);
                break;
            case 1:
                sortedTripList = sortTripsByLengthOfTime(tripList);
                break;
            case 2:
                sortedTripList = sortTripsByDistance(tripList);
                break;
            default:
                sortedTripList = tripList;
                break;
        }

        getView().displayTripList(sortedTripList);

    }

    private List<Trip> sortTripsByDate(List<Trip> tripList) {
        Collections.sort(tripList, (trip1, trip2) -> {

            long date1 = Long.valueOf(trip1.getTimeStart());
            long date2 = Long.valueOf(trip2.getTimeStart());

            return date1 < date2 ? 1 : -1;
        });

        return tripList;
    }

    private List<Trip> sortTripsByLengthOfTime(List<Trip> tripList) {
        Collections.sort(tripList, (trip1, trip2) -> {

            int time1 = Integer.valueOf(trip1.getTimeEnd()) - Integer.valueOf(trip1.getTimeStart());
            int time2 = Integer.valueOf(trip2.getTimeEnd()) - Integer.valueOf(trip2.getTimeStart());

            return time1 < time2 ? 1 : -1;
        });

        return tripList;
    }

    private List<Trip> sortTripsByDistance(List<Trip> tripList) {
        Collections.sort(tripList, (trip1, trip2) -> {

            double distance1 = trip1.getMileageAccum();
            double distance2 = trip2.getMileageAccum();

            return distance1 < distance2 ? 1 : -1;
        });

        return tripList;
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
