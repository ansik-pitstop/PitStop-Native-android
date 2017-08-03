package com.pitstop.ui.add_car.device_search;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.ReadyDevice;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.observer.BluetoothVinObserver;
import com.pitstop.utils.AddCarUtils;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.TimeoutTimer;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class DeviceSearchPresenter implements BluetoothConnectionObserver, BluetoothVinObserver{

    private final String TAG = getClass().getSimpleName();

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private DeviceSearchView view;
    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private boolean searchingForVin;
    private boolean searchingForDevice;
    private ReadyDevice readyDevice = new ReadyDevice("","","");

    //Try to get VIN 8 times, every 6 seconds
    private final TimeoutTimer getVinTimer = new TimeoutTimer(6, 8) {
        @Override
        public void onRetry() {
            Log.d(TAG,"getVinTimer.onRetry()");

            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.ADD_CAR_RETRY_GET_VIN);
            bluetoothConnectionObservable.requestVin();
        }

        @Override
        public void onTimeout() {
            Log.d(TAG,"getVinTimer.onTimeout()");

            if (view == null) return;

            if (readyDevice == null){
                view.onVinRetrievalFailed("","");
            }
            else{
                view.onVinRetrievalFailed(readyDevice.getScannerName(),readyDevice.getScannerId());
            }

            view.hideLoading(null);
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.ADD_CAR_NOT_SUPPORT_VIN);

        }
    };

    private final TimeoutTimer findDeviceTimer = new TimeoutTimer(13, 5) {
        @Override
        public void onRetry() {
            if (bluetoothConnectionObservable != null){
                bluetoothConnectionObservable.requestDeviceSearch(true, true);
            }
        }

        @Override
        public void onTimeout() {
            Log.d(TAG,"gindDeviceTimer.onTimeout()");

            if (view == null) return;

            searchingForDevice = false;
            view.onCannotFindDevice();
            view.hideLoading(null);
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                    , MixpanelHelper.ADD_CAR_STEP_RESULT_FAILED);

        }
    };

    public DeviceSearchPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){

        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void setBluetoothConnectionObservable(
            BluetoothConnectionObservable bluetoothConnectionObservable){

        this.bluetoothConnectionObservable = bluetoothConnectionObservable;

        //If view is subscribed, subscribe to bluetooth service
        if (view != null){
            bluetoothConnectionObservable.subscribe(this);
        }
    }

    public void subscribe(DeviceSearchView view){
        Log.d(TAG,"subscribe()");
        this.view = view;

        if (bluetoothConnectionObservable != null){
            bluetoothConnectionObservable.subscribe(this);
        }
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;

        if (bluetoothConnectionObservable != null){
            bluetoothConnectionObservable.unsubscribe(this);
        }
    }

    public void startSearch(){
        Log.d(TAG,"startSearch()");

        if (view == null || bluetoothConnectionObservable == null) return;

        //Already searching, no need to start another search
        if (searchingForDevice) return;

        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                , MixpanelHelper.ADD_CAR_STEP_RESULT_PENDING);

        //Check if mileage is valid
        if (!AddCarUtils.isMileageValid(view.getMileage())){
            view.onMileageInvalid();
            return;
        }

        //Check if already connected to device
        if (bluetoothConnectionObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.CONNECTED)){

            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                    , MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

            readyDevice = bluetoothConnectionObservable.getReadyDevice();

            //Check if retrieved VIN is valid, otherwise begin timer
            if (!AddCarUtils.isVinValid(readyDevice.getVin())){
                view.showLoading("Retrieving VIN");
                searchingForVin = true;
                getVinTimer.start();
            }

            //Add the car
            else{
                addCar(readyDevice);
            }

        }
        //Otherwise request search and wait for callback
        else{
            view.showLoading("Searching for Device");
            searchingForDevice = true;
            findDeviceTimer.start();
            bluetoothConnectionObservable.requestDeviceSearch(true, true);
        }
    }

    @Override
    public void onSearchingForDevice() {
        Log.d(TAG,"onSearchingForDevice()");
    }

    @Override
    public void onDeviceReady(ReadyDevice readyDevice) {
        Log.d(TAG,"onDeviceReady()");
        if (view == null) return;
        if (!searchingForDevice) return;

        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                , MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);
        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                , MixpanelHelper.ADD_CAR_STEP_RESULT_PENDING);

        searchingForDevice = false;
        findDeviceTimer.cancel();
        this.readyDevice = readyDevice;

        //Check for valid vin
        if (AddCarUtils.isVinValid(readyDevice.getVin())){

            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

            searchingForVin = false;
            //Begin  adding car
            addCar(readyDevice);

        }
        else{

            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.ADD_CAR_STEP_RESULT_PENDING);

            view.showLoading("Getting VIN");

            //Try to get valid VIN
            searchingForVin = true;
            getVinTimer.start();
        }
    }

    @Override
    public void onDeviceDisconnected() {
        Log.d(TAG,"onDeviceDisconnected()");
    }

    @Override
    public void onDeviceVerifying() {
        Log.d(TAG,"onDeviceDisconnected()");
    }

    @Override
    public void onDeviceSyncing() {
        Log.d(TAG,"onDeviceDisconnected()");
    }

    private void addCar(ReadyDevice readyDevice){
        Log.d(TAG,"addCar()");

        view.showLoading("Saving Car");

        useCaseComponent.addCarUseCase().execute(readyDevice.getVin(), view.getMileage()
                , readyDevice.getScannerId(), readyDevice.getScannerName()
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
                        view.hideLoading("Car Successfully Added");
                    }

                    @Override
                    public void onCarAdded(Car car) {
                        Log.d(TAG,"addCarUseCase().onCarAdded() car: "+car);
                        if (view == null) return;

                        view.onCarAddedWithoutShop(car);
                        view.hideLoading("Car Successfully Added");
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"addCarUseCase().onError() error: "+error.getMessage());
                        if (view == null) return;

                        if (error.getError().equals(RequestError.ERR_OFFLINE)){
                            view.onErrorAddingCar("Please connect to the internet to add " +
                                    "your vehicle.");
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

    @Override
    public void onGotVin(String vin) {
        Log.d(TAG,"onGotVin() vin: "+vin);
        if (view == null) return;
        if (!searchingForVin) return;

        if (AddCarUtils.isVinValid(vin)){
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);
            getVinTimer.cancel();
            searchingForVin = false;
            readyDevice.setVin(vin);
            addCar(readyDevice);
        }
    }

    public void onBackPressed(){
        if (view == null) return;

        if (searchingForVin){
            getVinTimer.cancel();
            searchingForVin = false;
            view.hideLoading("");
        }
        else if (searchingForDevice){
            findDeviceTimer.cancel();
            searchingForDevice = false;
            view.hideLoading("");
        }
        else{
            //Saving car, do not allow back press
        }
    }
}
