package com.pitstop.ui.add_car.vin_entry;

import android.content.res.Resources;
import android.util.Log;
import android.view.KeyEvent;

import com.pitstop.EventBus.EventSource;
import com.pitstop.R;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;
import com.pitstop.utils.AddCarUtils;
import com.pitstop.utils.MixpanelHelper;

import static android.view.KeyEvent.KEYCODE_BACK;

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
    private int mileage = 0;


    public VinEntryPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){

        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(VinEntryView view){
        Log.d(TAG,"subscribe()");

        this.view = view;

        //Get scanner id and scanner name in case they were set before presenter was subscribed
        this.scannerId = view.getScannerId();
        this.scannerName = view.getScannerName();
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");

        this.view = null;
        this.scannerId = "";
        this.scannerName = "";
        this.mileage = 0;
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

    void loadInfo(){
        if (view != null)
            view.displayMileage(view.getTransferredMileage());
    }

    private void addVehicleToServer(String vin, int mileage, String scannerId, String scannerName){
        Log.d(TAG,"addVehicleToServer() vin:"+vin+", mileage:"+mileage+", scannerId:"+scannerId
                +", scannerName:"+scannerName);

        view.showLoading(Resources.getSystem().getString(R.string.saving_car_message));
        useCaseComponent.addCarUseCase().execute(vin, mileage, scannerId, scannerName
                , EventSource.SOURCE_ADD_CAR, new AddCarUseCase.Callback() {

                    @Override
                    public void onCarAlreadyAdded(Car car){
                        Log.d(TAG,"addCarUseCase().onCarAlreadyAdded() car: "+car);
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SAVE_TO_SERVER
                                ,MixpanelHelper.ADD_CAR_CAR_EXISTS);addingCar = false;
                        if (view == null) return;

                        view.setLoadingCancelable(true);
                        view.onCarAlreadyAdded(car);
                        view.hideLoading(null);
                    }

                    @Override
                    public void onCarAddedWithBackendShop(Car car) {
                        Log.d(TAG,"addCarUseCase().onCarAddedWithBackendShop() car: "+car);
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SAVE_TO_SERVER
                                ,MixpanelHelper.SUCCESS);addingCar = false;
                        if (view == null) return;

                        view.setLoadingCancelable(true);
                        view.onCarAddedWithShop(car);
                        view.hideLoading(Resources.getSystem().getString(R.string.car_added_successfully_toast_message));
                    }

                    @Override
                    public void onCarAdded(Car car) {
                        Log.d(TAG,"addCarUseCase().onCarAdded() car: "+car);
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SAVE_TO_SERVER
                                ,MixpanelHelper.SUCCESS);addingCar = false;
                        if (view == null) return;

                        view.setLoadingCancelable(true);
                        view.onCarAddedWithoutShop(car);
                        view.hideLoading(Resources.getSystem().getString(R.string.car_added_successfully_toast_message));
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"addCarUseCase().onError() error: "+error.getMessage());
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SAVE_TO_SERVER
                                ,MixpanelHelper.FAIL);addingCar = false;
                        if (view == null) return;

                        view.setLoadingCancelable(true);
                        if (error.getError().equals(RequestError.ERR_OFFLINE)){
                            view.beginPendingAddCarActivity(vin,mileage,scannerId);
                            view.hideLoading(Resources.getSystem().getString(R.string.connect_to_internet_toast_message));
                        }
                        else{
                            view.onErrorAddingCar(Resources.getSystem().getString(R.string.unexpected_car_adding_error_message));
                            view.hideLoading(null);
                        }
                    }
                });
    }

    public void addVehicle(){
        Log.d(TAG,"addVehicle() vin:"+view.getVin()+", addingCar?"+addingCar +", scannerId: "
                +scannerId+", scannerName: "+scannerName);

        if (view == null) return;
        if (addingCar) return;

        final String correctVin = AddCarUtils.removeWhitespace(view.getVin());

        //Check for valid vin again, even though it should be valid here
        if (!AddCarUtils.isVinValid(correctVin)){
            view.onInvalidVinInput();
            return;
        }

        //Check for valid mileage
        if (!AddCarUtils.isMileageValid(view.getMileage())){
            view.onInvalidMileage();
            return;
        }

        view.setLoadingCancelable(false); //Do not allow back button to cancel loading prompt
        addingCar = true;   //Do not allow multiple calls to add car

        //Got VIN manually with success
        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                , MixpanelHelper.SUCCESS);

        //Add vehicle logic below
        int mileage = Integer.valueOf(view.getMileage());
        addVehicleToServer(correctVin,mileage,scannerId,scannerName);


    }

    public void onGotVinScanResult(String result){
        Log.d(TAG,"onGotVinScanResult() result: "+result);
        if (view == null || result == null) return;

        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_SCAN_VIN_BARCODE
                ,MixpanelHelper.SUCCESS);

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

    public void gotDeviceInfo(String scannerName, String scannerId, int mileage){
        Log.d(TAG,"gotDeviceInfo() scannerName: "+scannerName+", scannerId: "+scannerId
                +", mileage: "+mileage);

        this.scannerName = scannerName;
        this.scannerId = scannerId;
        this.mileage = mileage;

        if (view != null)
            view.displayMileage(mileage);
    }

    public void onProgressDialogKeyPressed(int keyCode){
        Log.d(TAG,"onProgressDialogKeyPressed(), backButton?"+(keyCode == KeyEvent.KEYCODE_BACK));
        if (keyCode == KEYCODE_BACK) {
            onBackPressed();
        }
    }

    public void onBackPressed(){
        Log.d(TAG,"onBackPressed()");
        if (view == null) return;

        //Ignore back press if adding car
        if (!addingCar){
            view.showAskHasDeviceView();
            view.hideLoading("");
        }
    }

    public void onGotPendingActivityResults(String vin, int mileage, String scannerId
            , String scannerName){

        Log.d(TAG,"onGotPendingActivityResults() vin: "+vin+", mileage:"+mileage+", scannerId:"
                +scannerId+", scanner");

        if (view == null) return;

        this.scannerId = scannerId;

        if (!AddCarUtils.isVinValid(vin)){
            view.onInvalidVinInput();
        }
        else if (!AddCarUtils.isMileageValid(mileage)){
            view.onInvalidMileage();
        }
        else{
            addVehicleToServer(vin,mileage,scannerId,scannerName);
        }
    }

}
