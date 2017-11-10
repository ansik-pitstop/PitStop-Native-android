package com.pitstop.ui.vehicle_specs;

import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddLicensePlateUseCase;
import com.pitstop.interactors.get.GetCarImagesArrayUseCase;
import com.pitstop.interactors.get.GetCarStyleIDUseCase;
import com.pitstop.interactors.get.GetLicensePlateUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.update.UpdateCarMileageUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.TabPresenter;
import com.pitstop.utils.MixpanelHelper;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsPresenter extends TabPresenter<VehicleSpecsView> {

    private final static String TAG = VehicleSpecsPresenter.class.getSimpleName();
    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private boolean updating;
    private Car mCar;
    private Dealership mdealership;
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_MY_CAR);
    public final EventType[] ignoredEvents = {
            new EventTypeImpl(EventType.EVENT_SERVICES_HISTORY),
            new EventTypeImpl(EventType.EVENT_DTC_NEW),
            new EventTypeImpl(EventType.EVENT_SERVICES_NEW)
    };


    @Override
    public EventType[] getIgnoredEventTypes() {
        return ignoredEvents;
    }

    @Override
    public void onAppStateChanged() {
        onUpdateNeeded();
    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    public static final String BASE_URL_PHOTO = "https://media.ed.edmunds-media.com";

    public VehicleSpecsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void onUpdateLicensePlateDialogConfirmClicked(int carID, String s) {
        Log.d(TAG, "onUpdateLicensePlateDialogConfirmClicked()");
        if (getView()== null|| updating)return;
        updating = true;
        useCaseComponent.addLicensePlateUseCase().execute(carID, s, new AddLicensePlateUseCase.Callback() {
            @Override
            public void onLicensePlateStored(String licensePlate) {
                updating = false;
                if (getView()== null)return;
                getView().showLicensePlate(licensePlate);
            }

            @Override
            public void onError(RequestError error) {}
        });
    }

    public void getCarImage(String Vin){
        if (getView() == null || updating)return;
        updating = true;
        Log.d(TAG, "getCarImage()");
        getView().showImageLoading();
        useCaseComponent.getCarStyleIDUseCase().execute(Vin, new GetCarStyleIDUseCase.Callback() {
            @Override
            public void onStyleIDGot(String styleID) {
                if (getView() == null)return;
                Log.d(TAG, styleID);
                useCaseComponent.getCarImagesArrayUseCase().execute(styleID, new GetCarImagesArrayUseCase.Callback() {
                    @Override
                    public void onArrayGot(String imageLink) {
                        updating = false;
                        if (getView() == null) return;
                        getView().hideImageLoading();
                        getView().showImage(BASE_URL_PHOTO + imageLink);
                    }
                    @Override
                    public void onError(RequestError error) {
                        updating = false;
                        if (getView() ==null) return;
                        getView().hideImageLoading();
                        getView().showDealershipBanner();
                       // Log.d(TAG, error.getMessage());
                    }
                });
            }
            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
                getView().hideImageLoading();
                getView().showDealershipBanner();
                Log.d(TAG, error.getMessage());
            }
        });
    }

    public void getLicensePlate(int carID){
        Log.d(TAG, "getLicensePlate()");
        if (getView() == null) return;
        useCaseComponent.getLicensePlateUseCase().execute(carID, new GetLicensePlateUseCase.Callback() {
            @Override
            public void onLicensePlateGot(String licensePlate) {
                if (getView() == null) return;
                Log.d(TAG, "licensePlateGot");
                getView().showLicensePlate(licensePlate);
            }
            @Override
            public void onError(RequestError error) {
                if (getView() == null) return;
                Log.d(TAG, "gettingLicensePlateFailed");
                getView().showLicensePlate("");
            }
        });
    }

    public void deleteCar(){
        if(getView() == null||updating)return;
        updating = true;
        getView().showLoadingDialog("Loading...");
        Log.d(TAG, "deleteCar()");
        useCaseComponent.removeCarUseCase().execute(this.mCar.getId(), EventSource.SOURCE_MY_GARAGE, new RemoveCarUseCase.Callback() {
            @Override
            public void onCarRemoved() {
                updating = false;
                if (getView() == null)return;
                getView().hideLoadingDialog();
                onUpdateNeeded();

            }
            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
                getView().hideLoadingDialog();
                getView().toast(error.getMessage());
            }
        });
    }

    public void getCurrentCar() {
        Log.d(TAG, "getCurrentCar()");
        if (getView() == null|| updating)return;
        updating = true;
        getView().showLoading();
        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                mCar = car;
                mdealership = car.getShop();
                if (!isLocal)
                    updating = false;
                if (getView()!=null) {
                    if (!isLocal)
                        getView().hideLoading();
                    getView().setCarView(mCar);
                    //getCarImage(mCar.getVin());
                }
            }

            @Override
            public void onNoCarSet(boolean isLocal) {
                updating = false;
                if (getView()!=null && !isLocal)
                    getView().showNoCarView();
            }

            @Override
            public void onError(RequestError error) {
                updating = false;
                if (getView() == null) return;
                if (error.getError() == RequestError.ERR_OFFLINE) {
                    if (getView().hasBeenPopulated())
                        getView().displayOfflineErrorDialog();
                    else
                        getView().showOfflineErrorView();
                }
                else {
                    if (getView().hasBeenPopulated())
                        getView().displayUnknownErrorDialog();
                    else
                        getView().showUnknownErrorView();
                }
            }
        });



    }

    public Car getCar() {
        Log.d(TAG, "getCar()");
        if (this.mCar!=null){
            return this.mCar;
        }
        else
            return null;
    }

    public Dealership getDealership(){
        Log.d(TAG, "getDealership()");
        return this.mdealership;
    }

    public void onScannerViewClicked() {
        Log.d(TAG, "onScannerVIewCLicked()");
        if (this.mCar.getScannerId() == null&& getView()!= null)
            getView().showBuyDeviceDialog();

    }

    public void onUpdateNeeded() {
        Log.d(TAG, "onUdateNeeded()");
        getCurrentCar();
    }

    public void onRefresh() {
        Log.d(TAG, "onRefresh()");
        onUpdateNeeded();
    }

    void onUpdateMileageDialogConfirmClicked(String mileageText){
        Log.d(TAG,"onUpdateMileageDialogConfirmClicked()");
        if (updating) return;
        updating = true;
        getView().showLoading();
        double mileage;
        try{
            mileage = Double.valueOf(mileageText);
        }catch(NumberFormatException e){
            e.printStackTrace();
            getView().displayUpdateMileageError();
            getView().hideLoading();
            updating = false;
            return;
        }

        if (mileage < 0 || mileage > 3000000){
            getView().hideLoading();
            getView().displayUpdateMileageError();
            updating = false;
            return;
        }

        useCaseComponent.updateCarMileageUseCase().execute(mileage
                , new UpdateCarMileageUseCase.Callback() {

                    @Override
                    public void onMileageUpdated() {
                        updating = false;
                        EventBus.getDefault().post(new CarDataChangedEvent(
                                new EventTypeImpl(EventType.EVENT_MILEAGE),EVENT_SOURCE));
                        if (getView() == null) return;
                        getView().hideLoading();

                        try{
                            getView().displayMileage(mileage);
                        }catch(NumberFormatException e){
                            e.printStackTrace();
                            getView().displayUnknownErrorDialog();
                        }
                    }

                    @Override
                    public void onNoCarAdded() {
                        updating = false;
                        if (getView() == null) return;
                        getView().hideLoading();
                        getView().showNoCarView();
                    }

                    @Override
                    public void onError(RequestError error) {
                        updating = false;
                        if (getView() == null) return;

                        if (error.getError().equals(RequestError.ERR_OFFLINE)){
                            if (getView().hasBeenPopulated()){
                                getView().displayOfflineErrorDialog();
                            }
                            else{
                                getView().showOfflineErrorView();
                            }
                        }
                        else{
                            getView().hideLoading();
                            getView().displayUnknownErrorDialog();
                        }

                        getView().hideLoading();
                    }
                });
    }
}