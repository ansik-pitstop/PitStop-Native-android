package com.pitstop.ui.my_garage;

import android.support.v4.app.Fragment;
import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCarsWithDealershipsUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import io.smooch.core.User;

/**
 * Created by ishan on 2017-09-19.
 */

public class MyGaragePresenter extends TabPresenter<MyGarageView>{

    private static final String TAG = MyGaragePresenter.class.getSimpleName();
    private HashMap<String, Object> customProperties;
    private boolean dealershipsLoaded = false;
    private boolean carsLoaded = false;
    private List<Dealership> dealershipList = new ArrayList<>();
    private List<Car> carList = new ArrayList<>();
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
        Log.d(TAG, "onAppStateChanged()");
        dealershipsLoaded = false;
        carsLoaded = false;
        if (getView()!=null)
            getView().onUpdateNeeded();

    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    public void onMyAppointmentsClicked() {
        Log.d(TAG, "onMyAppointmentsClicked()");
        if (getView()!= null)
            getView().openMyAppointments();

    }

    public void onRequestServiceClicked() {
        Log.d(TAG, "onRequestServiceClicked()");
        if (getView()!= null)
            getView().openRequestService();
    }

    public void onMessageClicked() {
        Log.d(TAG, "onMessageClicked()");
        if (getView() == null||updating )return;
        updating = true;
        getView().showLoadingDialog("Loading...");
        if (customProperties == null){
            useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
                @Override
                public void onCarRetrieved(Car car, Dealership dealership) {
                    Log.d(TAG, "onCarRetrieved()");
                    updating = false;
                    if (getView() == null)return;
                    //mixpanelHelper.trackFabClicked("Message");
                    customProperties = new HashMap<>();
                    customProperties.put("VIN", car.getVin());
                    customProperties.put("Car Make", car.getMake());
                    customProperties.put("Car Model", car.getModel());
                    customProperties.put("Car Year", car.getYear());
                    Log.i(TAG, dealership.getEmail());
                    customProperties.put("Email", dealership.getEmail());
                    User.getCurrentUser().addProperties(customProperties);
                    if (!getView().isUserNull()) {
                        customProperties.put("Phone", getView().getUserPhone());
                        User.getCurrentUser().setFirstName(getView().getUserFirstName());
                        User.getCurrentUser().setEmail(getView().getUserEmail());
                    }
                    getView().hideLoadingDialog();
                    getView().openSmooch();
                }
                @Override
                public void onNoCarSet() {
                    Log.d(TAG, "noCarSet()");
                    updating = false;
                    if (getView() == null) return;
                    getView().hideLoadingDialog();
                    getView().toast(((Fragment)getView()).getContext().getString(R.string.select_car_request_toast));
                }
                @Override
                public void onError(RequestError error) {
                    updating = false;
                    if (getView() == null) return;
                    getView().hideLoadingDialog();
                }});}
        else{
            updating = false;
            if (getView() == null) return;
            getView().hideLoadingDialog();
            getView().openSmooch();
        }
    }

    public void onCallClicked() {
        Log.d(TAG, "onCallClicked()");
        if(getView() == null || updating) return;
        if (!dealershipsLoaded) {
            updating = true;
            useCaseComponent.getCarsWithDealershipsUseCase().execute(new GetCarsWithDealershipsUseCase.Callback() {
                @Override
                public void onGotCarsWithDealerships(@NotNull LinkedHashMap<Car, Dealership> data) {
                    Log.d(TAG, "onCarsRetrieved()");
                    updating = false;
                    if (getView()== null) return;

                    mergeSetWithCarList(data.keySet());
                    dealershipsLoaded = true;
                    carsLoaded = true;
                    mergeSetWithDealershipList(data.values());

                    if (dealershipList.size() == 0)
                        getView().toast(((Fragment)getView()).getContext().getString(R.string.select_dealership_toast_text));
                    else if (dealershipList.size() == 1)
                        getView().callDealership(dealershipList.get(0));
                    else
                        getView().showDealershipsCallDialog(getValidDealershipList());
                }

                @Override
                public void onError(@NotNull RequestError error) {
                    Log.d(TAG, error.getMessage());
                    updating = false;
                    if (getView() == null) return;
                    getView().toast(error.getMessage());
                }
            });
        }
        else {
            updating = false;
            if (getView() == null)return;
            Log.d(TAG, "dealershipsAlreadyGot");
            if (dealershipList.size() == 0)
                getView().toast(((Fragment)getView()).getContext().getString(R.string.select_dealership_toast_text));
            else if (dealershipList.size() == 1)
                getView().callDealership(dealershipList.get(0));
            else
                getView().showDealershipsCallDialog(getValidDealershipList());
        }
    }

    public void onFindDirectionsClicked() {
        Log.d(TAG, "onFindDirectionClicked()");
        if(getView() == null || updating) return;
        if (!dealershipsLoaded) {
            updating = true;
            useCaseComponent.getCarsWithDealershipsUseCase().execute(new GetCarsWithDealershipsUseCase.Callback() {
                @Override
                public void onGotCarsWithDealerships(@NotNull LinkedHashMap<Car, Dealership> data) {
                    updating = false;
                    if (getView() == null) return;

                    mergeSetWithDealershipList(data.values());
                    dealershipsLoaded = true;

                    if (dealershipList.size() == 0)
                        getView().toast(((Fragment)getView()).getContext().getString(R.string.select_dealership_toast_text));
                    else if (dealershipList.size() == 1)
                        getView().openDealershipDirections(dealershipList.get(0));
                    else
                        getView().showDealershipsDirectionDialog(getValidDealershipList());
                }

                @Override
                public void onError(@NotNull RequestError error) {
                    updating = false;
                    if (getView() == null) return;
                    getView().toast(error.getMessage());
                }
            });
        }
        else {
            updating = false;
            if (getView() == null)return;
            if (dealershipList.size() == 0)
                getView().toast(((Fragment)getView()).getContext().getString(R.string.select_dealership_toast_text));
            else if (dealershipList.size() == 1)
                getView().openDealershipDirections(dealershipList.get(0));
            else
                getView().showDealershipsDirectionDialog(getValidDealershipList());
        }
    }

    public void loadCars() {
        Log.d(TAG, "loadCars()");
        if(getView() == null|| updating)return;
        if (!carsLoaded){
            getView().showLoading();
            updating = true;
            useCaseComponent.getCarsWithDealershipsUseCase().execute(new GetCarsWithDealershipsUseCase.Callback() {
                @Override
                public void onGotCarsWithDealerships(@NotNull LinkedHashMap<Car, Dealership> data) {
                    Log.d(TAG, "onCarsRetrieved()");
                    updating = false;
                    if (getView()  == null) return;
                    getView().hideLoading();
                    if (data.keySet().size() == 0){
                        getView().noCarsView();
                    }else
                        getView().appointmentsVisible();

                    mergeSetWithCarList(data.keySet());
                    mergeSetWithDealershipList(data.values());
                    dealershipsLoaded = true;
                    carsLoaded = true;

                    getView().hideLoading();
                    getView().showCars(carList);
                }

                @Override
                public void onError(@NotNull RequestError error) {
                    updating = false;
                    if (getView() == null)return;
                    if (getView().hasBeenPopulated()){
                        getView().showErrorDialog();
                    }
                    getView().hideLoading();
                }
            });
        }
        else {
            if (getView() == null) return;
            getView().hideLoading();
        }
    }

    private void mergeSetWithDealershipList(Collection<Dealership> data){
        dealershipList.clear();
        dealershipList.addAll(data);
    }

    private void mergeSetWithCarList(Set<Car> data){
        carList.clear();
        carList.addAll(data);
    }

    public void onCarClicked(Car car, Dealership dealership, int position) {
        Log.d(TAG, "onCarClicked()");
        if (getView()!=null){
            getView().openSpecsActivity(car, dealership, position);

        }
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh()");
        if (getView()!=null)
            getView().onUpdateNeeded();
    }

    public void makeCarListNull() {
        carsLoaded = false;
    }

    List<Car> getCars(){
        return carList;
    }

    List<Dealership> getDealerships(){
        return dealershipList;
    }

    void onCarSetAsCurrent(int pos){
        for (Car c: carList){
            c.setCurrentCar(false);
        }
        carList.get(pos).setCurrentCar(true);
        if (getView() != null) getView().notifyCarDataChanged();
    }

    public void onCarRemoved(int anInt) {
        if (getView() != null && carList != null){
            if (carList.get(anInt).isCurrentCar()){
                getView().onUpdateNeeded();
            }else{
                carList.remove(anInt);
                getView().notifyCarDataChanged();
                if (carList.size() == 0)
                    getView().appointmentsInvisible();
            }



        }
    }

    private List<Dealership> getValidDealershipList(){
        List<Dealership> validList = new ArrayList<>();
        for (Dealership d: dealershipList){
            if (d.getId() != 1)
                validList.add(d);
        }
        return validList;
    }
}
