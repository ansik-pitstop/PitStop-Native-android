package com.pitstop.ui.add_car.device_search;

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

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private DeviceSearchView view;
    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private boolean searchingForVin;
    private boolean searchingForDevice;
    private ReadyDevice readyDevice;

    //Try to get VIN 8 times, every 6 seconds
    private final TimeoutTimer getVinTimer = new TimeoutTimer(6, 8) {
        @Override
        public void onRetry() {
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.ADD_CAR_RETRY_GET_VIN);
            bluetoothConnectionObservable.requestVin();
        }

        @Override
        public void onTimeout() {
            if (view == null) return;

            if (readyDevice == null){
                view.onVinRetrievalFailed("","");
            }
            else{
                view.onVinRetrievalFailed(readyDevice.getScannerName(),readyDevice.getScannerId());
            }

            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.ADD_CAR_NOT_SUPPORT_VIN);

        }
    };

    private final TimeoutTimer findDeviceTimer = new TimeoutTimer(60, 0) {
        @Override
        public void onRetry() {
        }

        @Override
        public void onTimeout() {
            if (view == null) return;

            view.onCannotFindDevice();
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                    , MixpanelHelper.ADD_CAR_STEP_RESULT_FAILED);

        }
    };

    public DeviceSearchPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper
            , BluetoothConnectionObservable bluetoothConnectionObservable){

        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
    }

    public void subscribe(DeviceSearchView view){
        this.view = view;
        bluetoothConnectionObservable.subscribe(this);
    }

    public void unsubscribe(){
        this.view = null;
        bluetoothConnectionObservable.unsubscribe(this);
    }

    public void startSearch(){
        if (view == null) return;

        //Already searching, no need to start another search
        if (searchingForDevice) return;

        //Check if mileage is valid
        if (!AddCarUtils.isMileageValid(view.getMileage())){
            view.onMileageInvalid();
            return;
        }

        //Check if already connected to device
        if (bluetoothConnectionObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.CONNECTED)){

            readyDevice = bluetoothConnectionObservable.getReadyDevice();

            //Check if retrieved VIN is valid, otherwise begin timer
            if (!AddCarUtils.isVinValid(readyDevice.getVin())){
                view.showLoading("Retrieving VIN");
                searchingForVin = true;
                getVinTimer.start();
            }

            //Add the car
            else{
                addCar();
            }


        }
        //Otherwise request search and wait for callback
        else{
            view.showLoading("Searching for Device");
            searchingForDevice = true;
            bluetoothConnectionObservable.requestDeviceSearch(true);
        }
    }

    @Override
    public void onSearchingForDevice() {

    }

    @Override
    public void onDeviceReady(ReadyDevice readyDevice) {
        if (view == null) return;
        if (!searchingForDevice) return;

        searchingForDevice = false;
        findDeviceTimer.cancel();
        this.readyDevice = readyDevice;

        //Check for valid vin
        if (AddCarUtils.isVinValid(readyDevice.getVin())){
            searchingForVin = false;
            //Begin  adding car
            addCar();

        }
        else{
            view.showLoading("Getting VIN");

            //Try to get valid VIN
            searchingForVin = true;
            getVinTimer.start();
        }
    }

    @Override
    public void onDeviceDisconnected() {

    }

    @Override
    public void onDeviceVerifying() {

    }

    @Override
    public void onDeviceSyncing() {

    }

    private void addCar(){

        view.showLoading("Saving Car");

        useCaseComponent.addCarUseCase().execute(readyDevice.getScannerId(), view.getMileage()
                , readyDevice.getScannerId(), readyDevice.getScannerName()
                , EventSource.SOURCE_ADD_CAR, new AddCarUseCase.Callback() {
                    @Override
                    public void onCarAddedWithBackendShop(Car car) {
                        if (view == null) return;

                        view.onCarAddedWithShop(car);
                        view.hideLoading("Car Successfully Added");
                    }

                    @Override
                    public void onCarAdded(Car car) {
                        if (view == null) return;

                        view.onCarAddedWithoutShop(car);
                        view.hideLoading("Car Successfully Added");
                    }

                    @Override
                    public void onError(RequestError error) {
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
        if (view == null) return;
        if (!searchingForVin) return;

        if (AddCarUtils.isVinValid(vin)){
            getVinTimer.cancel();
            searchingForVin = false;
            addCar();
        }
    }
}
