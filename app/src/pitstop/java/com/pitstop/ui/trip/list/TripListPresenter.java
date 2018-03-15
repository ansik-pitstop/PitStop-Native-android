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
        void showTripOnMap(Trip trip);

        void showTripDetail(Trip trip);
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

    public void loadView() {

        Log.d(TAG, "loadView()");

        useCaseComponent.getTripsUseCase().execute("WVWXK73C37E116278", new GetTripsUseCase.Callback() {
            @Override
            public void onTripsRetrieved(@NotNull List<? extends Trip> tripList, boolean isLocal) {

                if (getView() != null) {
                    getView().displayTripList((List<Trip>) tripList);
                }

            }

            @Override
            public void onError(@NotNull RequestError error) {

                Log.d(TAG, "loadView().onError(): " + error);

            }
        });

    }

    public void setCommunicationInteractor(OnChildPresenterInteractorListener onChildPresenterInteractorListener) {

        this.mParentListener = onChildPresenterInteractorListener;

    }

    public void onTripRowClicked(Trip trip) {

        mParentListener.showTripOnMap(trip);

    }

    public void onTripInfoClicked(Trip trip) {

        mParentListener.showTripDetail(trip);

    }

    void onRefresh() {
        Log.d(TAG, "onRefresh()");

        mixpanelHelper.trackViewRefreshed(MixpanelHelper.SERVICE_UPCOMING_VIEW);
        if (getView() != null && getView().isRefreshing() && updating) {
            getView().hideRefreshing();
        } else {
            //onUpdateNeeded();
        }

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
