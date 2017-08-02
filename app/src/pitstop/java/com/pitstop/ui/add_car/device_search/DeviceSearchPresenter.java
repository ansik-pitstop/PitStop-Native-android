package com.pitstop.ui.add_car.device_search;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.ReadyDevice;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.observer.BluetoothVinObserver;
import com.pitstop.ui.add_car.FragmentSwitcher;
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
    private FragmentSwitcher fragmentSwitcher;

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
            view.onVinRetrievalFailed();
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
            view.onCannotFindDevice();
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                    , MixpanelHelper.ADD_CAR_STEP_RESULT_FAILED);

        }
    };

    public DeviceSearchPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper
            , BluetoothConnectionObservable bluetoothConnectionObservable
            , FragmentSwitcher fragmentSwitcher){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
        this.fragmentSwitcher = fragmentSwitcher;
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
        //Check if already connected to device
        if (bluetoothConnectionObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.CONNECTED)){

            ReadyDevice readyDevice = bluetoothConnectionObservable.getReadyDevice();

            //Check if retrieved VIN is valid, otherwise begin timer
            if (!isVinValid(readyDevice.getVin())){
                searchingForVin = true;
                getVinTimer.start();
            }

            //Add the car
            else{

            }


        }
        //Otherwise request search and wait for callback
        else{
            searchingForDevice = true;
            bluetoothConnectionObservable.requestDeviceSearch(true);
        }

    }

    //Bluetooth callbacks below

    @Override
    public void onSearchingForDevice() {

    }

    @Override
    public void onDeviceReady(String vin, String scannerId, String scannerName) {
        if (!searchingForDevice) return;

        searchingForDevice = false;

        //Check for valid vin
        if (isVinValid(vin)){
            findDeviceTimer.cancel();
            searchingForVin = false;

            //Begin  adding car
        }
        else{
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

    @Override
    public void onGotVin(String vin) {
        if (!searchingForVin) return;

        if (isVinValid(vin)){
            getVinTimer.cancel();
            searchingForVin = false;
        }
    }

    private String removeWhitespace(String s){
        return s.replace(" ","").replace("\n","").replace("\t","");
    }

    private boolean isVinValid(String vin){
        vin = removeWhitespace(vin);
        return vin != null && (vin.length() == 17);
    }
}
