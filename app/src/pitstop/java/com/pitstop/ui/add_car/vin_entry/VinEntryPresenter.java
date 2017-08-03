package com.pitstop.ui.add_car.vin_entry;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;
import com.pitstop.utils.AddCarUtils;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class VinEntryPresenter {

    private final String TAG = getClass().getSimpleName();

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private VinEntryView view;
    private String scannerName = "";
    private String scannerId = "";


    public VinEntryPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){

        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(VinEntryView view){
        Log.d(TAG,"subscribe()");

        this.view = view;
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");

        this.view = null;
    }

    public void vinChanged(String vin){
        Log.d(TAG,"vinChanged() vin:"+vin);
        if (view == null) return;

        if (AddCarUtils.isVinValid(vin)){
            view.onValidVinInput();
        }
        else{
            view.onInvalidVinInput();
        }
    }

    public void addVehicle(String vin){
        Log.d(TAG,"addVehicle() vin:"+vin);
        if (view == null) return;

        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                , MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

        vin = AddCarUtils.removeWhitespace(vin);
        int mileage = view.getMileage();

        //Check for valid vin again, even though it should be valid here
        if (!AddCarUtils.isVinValid(vin)){
            view.onInvalidVinInput();
            return;
        }

        //Check for valid mileage
        if (!AddCarUtils.isMileageValid(mileage)){
            view.onInvalidMileage();
            return;
        }

        //Add vehicle logic below
        view.showLoading("Saving Car");
        useCaseComponent.addCarUseCase().execute(vin, mileage, scannerId, scannerName
                , EventSource.SOURCE_ADD_CAR, new AddCarUseCase.Callback() {

            @Override
            public void onCarAlreadyAdded(Car car){
                Log.d(TAG,"addCarUseCase().onCarAlreadyAdded() car: "+car);
                if (view == null) return;

                view.onCarAlreadyAdded(car);
                view.hideLoading(null);
            }

            @Override
            public void onCarAddedWithBackendShop(Car car) {
                Log.d(TAG,"addCarUseCase().onCarAddedWithBackendShop() car: "+car);
                if (view == null) return;

                view.onCarAddedWithShop(car);
                view.hideLoading("Added Car Successfully");
            }

            @Override
            public void onCarAdded(Car car) {
                Log.d(TAG,"addCarUseCase().onCarAdded() car: "+car);
                if (view == null) return;

                view.onCarAddedWithoutShop(car);
                view.hideLoading("Added Car Successfully");
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"addCarUseCase().onError() error: "+error.getMessage());
                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    view.onErrorAddingCar("Please connect to the internet to add your vehicle.");
                    view.hideLoading(null);
                }
                else{
                    view.onErrorAddingCar("Unexpected error occurred adding car" +
                            ", please restart the app and try again.");
                    view.hideLoading(null);
                }
            }
        });

    }

    public void gotDeviceInfo(String scannerName, String scannerId){
        Log.d(TAG,"gotDeviceInfo() scannerName: "+scannerName+", scannerId: "+scannerId);

        this.scannerName = scannerName;
        this.scannerId = scannerId;
    }

}
