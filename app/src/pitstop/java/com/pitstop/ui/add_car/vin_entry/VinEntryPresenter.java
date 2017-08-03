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
    private boolean addingCar = false;


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
        Log.d(TAG,"addVehicle() vin:"+vin+", addingCar?"+addingCar);
        if (view == null) return;
        if (addingCar) return;

        view.setLoadingCancelable(false); //Do not allow back button to cancel loading prompt
        addingCar = true;

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
                addingCar = false;
                if (view == null) return;

                view.setLoadingCancelable(true);
                view.onCarAlreadyAdded(car);
                view.hideLoading(null);
            }

            @Override
            public void onCarAddedWithBackendShop(Car car) {
                Log.d(TAG,"addCarUseCase().onCarAddedWithBackendShop() car: "+car);
                addingCar = false;
                if (view == null) return;

                view.setLoadingCancelable(true);
                view.onCarAddedWithShop(car);
                view.hideLoading("Added Car Successfully");
            }

            @Override
            public void onCarAdded(Car car) {
                Log.d(TAG,"addCarUseCase().onCarAdded() car: "+car);
                addingCar = false;
                if (view == null) return;

                view.setLoadingCancelable(true);
                view.onCarAddedWithoutShop(car);
                view.hideLoading("Added Car Successfully");
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"addCarUseCase().onError() error: "+error.getMessage());
                addingCar = false;
                if (view == null) return;

                view.setLoadingCancelable(true);
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

    public void onGotVinScanResult(String result){
        Log.d(TAG,"onGotVinScanResult() result: "+result);
        if (view == null || result == null) return;

        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_SCAN_VIN_BARCODE
                ,MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

        view.displayVin(result);

        if (AddCarUtils.isVinValid(result)){
            view.displayScannedVinValid();
            view.onValidVinInput();
        }
        else{
            view.displayScannedVinInvalid();
            view.onInvalidVinInput();
        }
    }

    public void gotDeviceInfo(String scannerName, String scannerId){
        Log.d(TAG,"gotDeviceInfo() scannerName: "+scannerName+", scannerId: "+scannerId);

        this.scannerName = scannerName;
        this.scannerId = scannerId;
    }

    public void onBackPressed(){
        if (view == null) return;

        //Ignore back press if adding car
        if (!addingCar){
            view.showAskHasDeviceView();
        }
    }

}
