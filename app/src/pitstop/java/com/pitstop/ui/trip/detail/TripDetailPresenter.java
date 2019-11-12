package com.pitstop.ui.trip.detail;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCurrentUserUseCase;
import com.pitstop.interactors.remove.RemoveTripUseCase;
import com.pitstop.models.Settings;
import com.pitstop.models.User;
import com.pitstop.models.trip.Trip;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.UnitOfLength;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import kotlin.Unit;

/**
 * Created by David C. on 14/3/18.
 */

public class TripDetailPresenter extends TabPresenter<TripDetailView> {

    public interface OnDetailChildPresenterInteractorListener {

        void onTripRemoved();

    }

    private OnDetailChildPresenterInteractorListener mParentListener;

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

    public TripDetailPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void setCommunicationInteractor(OnDetailChildPresenterInteractorListener onDetailChildPresenterInteractorListener) {

        this.mParentListener = onDetailChildPresenterInteractorListener;

    }

    public void onDeleteTripClicked(Trip trip) {

        Log.d(TAG, "onDeleteTripClicked(), trip: " + trip);

        useCaseComponent.removeTripUseCase().execute(trip.getTripId(), trip.getVin(), new RemoveTripUseCase.Callback() {

            @Override
            public void onTripRemoved() {

                Log.d(TAG, "removeTripUseCase().onTripRemoved()");
                if (getView() == null) return;
                getView().onCloseView();

                mParentListener.onTripRemoved();

            }

            @Override
            public void onError(@NotNull RequestError error) {
                Log.d(TAG, "removeTripUseCase().onError(), error: " + error);

                if (getView() == null) return;

                if (error.getError().equals(RequestError.ERR_OFFLINE)) {
                    getView().displayOfflineErrorDialog();
                } else if (error.getError().equals(RequestError.ERR_UNKNOWN)) {
                    getView().displayUnknownErrorDialog();
                }

            }

        });

    }

    public void setMileage(String mileage) {
        TripDetailView view = getView();
        if (view == null) {
            return;
        }
        useCaseComponent.getGetCurrentUserUseCase().execute(new GetCurrentUserUseCase.Callback() {
            @Override
            public void onUserRetrieved(User user) {
                Settings settings = user.getSettings();
                if (settings != null) {
                    UnitOfLength unitOfLengthFromSettings = UnitOfLength.getValueFromToString(settings.getOdometer());
                    view.setUnitOfLength(unitOfLengthFromSettings);
                    if (unitOfLengthFromSettings == UnitOfLength.Miles) {
                        Double miles = UnitOfLength.convertKilometreToMiles(mileage);
                        getView().setTripMileage(String.format(Locale.getDefault(), "%.2f", miles));
                        return;
                    }
                }
                view.setTripMileage(mileage);
            }

            @Override
            public void onError(RequestError error) {
                view.setTripMileage(mileage);
            }
        });
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
