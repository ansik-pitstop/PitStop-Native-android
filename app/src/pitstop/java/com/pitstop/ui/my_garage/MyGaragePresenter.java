package com.pitstop.ui.my_garage;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCarsByUserIdUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.get.GetUserShopsUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.dashboard.DashboardPresenter;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.ui.service_request.RequestServiceActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;

import static com.pitstop.ui.main_activity.MainActivity.RC_REQUEST_SERVICE;

/**
 * Created by ishan on 2017-09-19.
 */

public class MyGaragePresenter extends TabPresenter<MyGarageView>{

    private static final String TAG = MyGaragePresenter.class.getSimpleName();
    private HashMap<String, Object> customProperties;
    private List<Dealership> dealershipList;
    private List<Car> carList;

    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_MY_GARAGE);

    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY),
            new EventTypeImpl(EventType.EVENT_DTC_NEW),
            new EventTypeImpl(EventType.EVENT_MILEAGE),
            new EventTypeImpl(EventType.EVENT_SERVICES_NEW)
    };

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private boolean updating = false;

    public MyGaragePresenter (UseCaseComponent useCaseComponent,
                               MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;

    }

    @Override
    public EventType[] getIgnoredEventTypes() {
        return ignoredEvents;
    }

    @Override
    public void onAppStateChanged() {
        dealershipList = null;
        carList = null;
        getView().onUpdateNeeded();

    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }


    public void onMyAppointmentsClicked() {
        Log.d(TAG, "onMyAppointmentsClicked()");
        getView().openMyAppointments();

    }

    public void onRequestServiceClicked() {
        Log.d(TAG, "onRequestServiceClicked()");
        getView().openRequestService();
    }

    public void onMessageClicked() {
        Log.d(TAG, "onMessageClicked()");
        if (getView() == null||updating )return;
        updating = true;
        getView().showLoading();
        if (customProperties == null){
            useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
                @Override
                public void onCarRetrieved(Car car) {
                    updating = false;
                    //mixpanelHelper.trackFabClicked("Message");
                    customProperties = new HashMap<>();
                    customProperties.put("VIN", car.getVin());
                    customProperties.put("Car Make", car.getMake());
                    customProperties.put("Car Model", car.getModel());
                    customProperties.put("Car Year", car.getYear());
                    Log.i(TAG, car.getDealership().getEmail());
                    customProperties.put("Email", car.getDealership().getEmail());
                    User.getCurrentUser().addProperties(customProperties);

                    if (!getView().isUserNull()) {
                        customProperties.put("Phone", getView().getUserPhone());
                        User.getCurrentUser().setFirstName(getView().getUserFirstName());
                        User.getCurrentUser().setEmail(getView().getUserEmail());
                    }
                    getView().hideLoading();
                    getView().openSmooch();
                }

                @Override
                public void onNoCarSet() {
                    updating = false;
                    getView().hideLoading();
                    getView().toast("Please Select a Car");
                }

                @Override
                public void onError(RequestError error) {
                    updating = false;
                    getView().hideLoading();
                    getView().toast(error.getMessage());
                }
            });
        }
        else{
            updating = false;
            getView().hideLoading();
            getView().openSmooch();
        }
    }

    public void onCallClicked() {
        Log.d(TAG, "onCallClicked()");
        if(getView() == null || updating) return;
        if (dealershipList == null) {
            updating = true;
            useCaseComponent.getCarsByUserIdUseCase().execute(new GetCarsByUserIdUseCase.Callback() {
                @Override
                public void onCarsRetrieved(List<Car> cars) {
                    Log.d(TAG, "onCarsRetrieved()");
                    updating = false;
                    carList = cars;
                    dealershipList = new ArrayList<Dealership>();
                    for (Car c : cars) {
                        if (c.getDealership().getId()!=1) {
                            dealershipList.add(c.getDealership());
                        }
                    }
                    if (dealershipList.size() == 0)
                        getView().toast("Please add a dealership");
                    else if (dealershipList.size() == 1)
                        getView().callDealership(dealershipList.get(0));
                    else
                        getView().showDealershipsCallDialog(dealershipList);
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG, error.getMessage());
                    updating = false;
                    getView().toast(error.getMessage());
                }
            });
        }
        else {
            updating = false;
            Log.d(TAG, "dealershipsAlreadyGot");
            if (dealershipList.size() == 0)

                getView().toast("Please add a dealership");
            else if (dealershipList.size() == 1)
                getView().callDealership(dealershipList.get(0));
            else
                getView().showDealershipsCallDialog(dealershipList);
        }
    }


    public void onFindDirectionsClicked() {
        Log.d(TAG, "onFindDirectionClicked()");

        if(getView() == null || updating) return;
        if (dealershipList == null) {
            updating = true;
            useCaseComponent.getCarsByUserIdUseCase().execute(new GetCarsByUserIdUseCase.Callback() {
                @Override
                public void onCarsRetrieved(List<Car> cars) {
                    updating = false;
                    dealershipList = new ArrayList<Dealership>();
                    for (Car c : cars) {
                        if (c.getDealership().getId()!=1) {
                            dealershipList.add(c.getDealership());
                        }
                    }
                    if (dealershipList.size() == 0)
                        getView().toast("Please add a dealership");
                    else if (dealershipList.size() == 1)
                        getView().openDealershipDirections(dealershipList.get(0));
                    else
                        getView().showDealershipsDirectionDialog(dealershipList);
                }

                @Override
                public void onError(RequestError error) {
                    updating = false;
                    getView().toast(error.getMessage());
                }
            });
        }
        else {
            updating = false;
            if (dealershipList.size() == 0)
                getView().toast("Please add a dealership");
            else if (dealershipList.size() == 1)
                getView().openDealershipDirections(dealershipList.get(0));
            else
                getView().showDealershipsDirectionDialog(dealershipList);
        }
    }
    public void loadCars() {
        Log.d(TAG, "loadCars()");
        if(getView() == null|| updating) return;
        if (carList ==null){
            getView().showLoading();
            updating = true;
            useCaseComponent.getCarsByUserIdUseCase().execute(new GetCarsByUserIdUseCase.Callback() {
                @Override
                public void onCarsRetrieved(List<Car> cars) {
                    getView().hideLoading();
                    updating = false;
                    if (cars.size() == 0){
                        getView().noCarsView();
                    }else
                        getView().appointmentsVisible();
                    carList = cars;
                    dealershipList = new ArrayList<Dealership>();
                    for (Car c : cars) {
                        if (c.getDealership().getId()!=1 ) {
                            dealershipList.add(c.getDealership());
                            Log.d(TAG, c.getDealership().getName());
                        }
                    }
                    getView().showCars(carList);
                }
                @Override
                public void onError(RequestError error) {
                    getView().hideLoading();
                    updating = false;
                    getView().toast(error.getMessage());
                }
            });
        }
    }

    public void onCarClicked(Car car) {
        Log.d(TAG, "onCarClicked()");
        getView().openSpecsActivity(car);

    }
}
