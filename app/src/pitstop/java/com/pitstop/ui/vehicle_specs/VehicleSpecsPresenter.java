package com.pitstop.ui.vehicle_specs;

import android.app.Dialog;
import android.util.Log;
import android.widget.Toast;

import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddLicensePlateUseCase;
import com.pitstop.interactors.get.GetCarImagesArrayUseCase;
import com.pitstop.interactors.get.GetCarStyleIDUseCase;
import com.pitstop.interactors.get.GetLicensePlateUseCase;
import com.pitstop.interactors.remove.RemoveCarUseCase;
import com.pitstop.interactors.set.SetUserCarUseCase;
import com.pitstop.network.RequestError;
import com.pitstop.ui.Presenter;
import com.pitstop.ui.scan_car.ScanCarContract;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsPresenter implements Presenter<VehicleSpecsView>{

    private VehicleSpecsView view;
    private final static String TAG = VehicleSpecsPresenter.class.getSimpleName();
    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private boolean updating;

    public static final String BASE_URL_PHOTO = "https://media.ed.edmunds-media.com";

    public VehicleSpecsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }
    @Override
    public void subscribe(VehicleSpecsView view) {
        this.view  = view;
    }

    @Override
    public void unsubscribe() {
        this.view = null;
    }

    public void onUpdateLicensePlateDialogConfirmClicked(int carID, String s) {
        Log.d(TAG, "onUpdateLicensePlateDialogConfirmClicked()");
        if(this.view == null|| updating)return;
        updating = true;
        useCaseComponent.addLicensePlateUseCase().execute(carID, s, new AddLicensePlateUseCase.Callback() {
            @Override
            public void onLicensePlateStored(String licensePlate) {
                updating = false;
                view.showLicensePlate(licensePlate);
            }

            @Override
            public void onError(RequestError error) {}
        });
    }

    public void getCarImage(String Vin){
        if (view == null || updating)return;
        updating = true;
        Log.d(TAG, "getCarImage()");
        useCaseComponent.getCarStyleIDUseCase().execute(Vin, new GetCarStyleIDUseCase.Callback() {
            @Override
            public void onStyleIDGot(String styleID) {
                Log.d(TAG, styleID);
                useCaseComponent.getCarImagesArrayUseCase().execute(styleID, new GetCarImagesArrayUseCase.Callback() {
                    @Override
                    public void onArrayGot(String imageLink) {
                        updating = false;
                        view.showImage(BASE_URL_PHOTO + imageLink);
                    }
                    @Override
                    public void onError(RequestError error) {
                        updating = false;
                        view.showDealershipBanner();
                        Log.d(TAG, error.getMessage());
                    }
                });
            }
            @Override
            public void onError(RequestError error) {
                updating = false;
                view.showDealershipBanner();
                Log.d(TAG, error.getMessage());
            }
        });
    }



    public void getLicensePlate(int carID){
        Log.d(TAG, "getLicensePlate()");
        if (this.view == null) return;

        useCaseComponent.getLicensePlateUseCase().execute(carID, new GetLicensePlateUseCase.Callback() {
            @Override
            public void onLicensePlateGot(String licensePlate) {
                Log.d(TAG, "licensePlateGot");
                view.showLicensePlate(licensePlate);
            }
            @Override
            public void onError(RequestError error) {
                Log.d(TAG, "gettingLicensePlateFailed");
            }
        });
    }

    public void makeCarCurrent(int carID) {
        Log.d(TAG, "makeCarCurrent()");
        useCaseComponent.setUseCarUseCase().execute(carID, EventSource.SOURCE_MY_GARAGE, new SetUserCarUseCase.Callback() {
            @Override
            public void onUserCarSet() {
                view.toast("Current Car Set");
            }
            @Override
            public void onError(RequestError error) {
                view.toast(error.getMessage());
            }
        });
    }

    public void deleteCar(int carID){
        Log.d(TAG, "deleteCar()");
        useCaseComponent.removeCarUseCase().execute(carID, EventSource.SOURCE_MY_GARAGE, new RemoveCarUseCase.Callback() {
            @Override
            public void onCarRemoved() {

                view.openMyGarage();
            }

            @Override
            public void onError(RequestError error) {
                view.toast(error.getMessage());
            }
        });
    }


}