package com.pitstop.ui.add_car.device_search;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;

import com.continental.rvd.mobile_sdk.BindingQuestion;
import com.continental.rvd.mobile_sdk.EBindingQuestionType;
import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error;
import com.pitstop.EventBus.EventSource;
import com.pitstop.R;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.bluetooth.BluetoothService;
import com.pitstop.bluetooth.dataPackages.PidPackage;
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

import org.jetbrains.annotations.NotNull;

import io.reactivex.disposables.Disposable;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class DeviceSearchPresenter implements BluetoothConnectionObserver, BluetoothVinObserver{

    private final String TAG = getClass().getSimpleName();

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private DeviceSearchView view;
    private ReadyDevice readyDevice = new ReadyDevice("","","");
    private boolean searchingForVin;
    private boolean searchingForDevice;
    private boolean addingCar = false;
    private boolean connectingToDevice = false;
    private BluetoothDeviceManager.DeviceType deviceType = BluetoothDeviceManager.DeviceType.OBD215;

    //Try to get VIN 2 times, every 6 seconds
    private final int GET_VIN_RETRY_TIME = 6;
    private final int GET_VIN_RETRY_AMOUNT = 1;
    private final TimeoutTimer getVinTimer = new TimeoutTimer(GET_VIN_RETRY_TIME
            , GET_VIN_RETRY_AMOUNT) {
        @Override
        public void onRetry() {
            Log.d(TAG,"getVinTimer.onRetry()");

            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.ADD_CAR_RETRY_GET_VIN);
            if (view != null)
                view.getBluetoothService().take(1).subscribe(BluetoothService::requestVin);
        }

        @Override
        public void onTimeout() {
            Log.d(TAG,"getVinTimer.onTimeout()");

            if (view == null || !searchingForVin) return;

            searchingForVin = false;
            int mileage = 0;
            try{
                mileage = Integer.valueOf(view.getMileage());
            }catch(NumberFormatException e){
                e.printStackTrace();
            }
            if (readyDevice == null){

                view.onVinRetrievalFailed("", "", mileage);
            }
            else{
                view.onVinRetrievalFailed(readyDevice.getScannerName()
                        , readyDevice.getScannerId(), mileage);
            }

            if (view.isBluetoothServiceRunning()){
                view.endBluetoothService();
            }

            view.hideLoading(null);
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.ADD_CAR_NOT_SUPPORT_VIN);

        }
    };



    private final int CONNECTING_TIMER_RETRY_TIMEOUT = 30;
    private final int CONNECTING_RETRY_AMOUNT = 0;
    private final TimeoutTimer connectionTimer = new TimeoutTimer(CONNECTING_TIMER_RETRY_TIMEOUT,CONNECTING_RETRY_AMOUNT ) {
        @Override
        public void onRetry() {
            Log.d(TAG, "connectionTimer.onRetry()");
        }

        @Override
        public void onTimeout() {
            Log.d(TAG,"connectionTimer.onTimeout()");
            if (view == null) return;
            connectingToDevice = false;
            view.onCouldNotConnectToDevice();
            if (view.isBluetoothServiceRunning()){
                view.endBluetoothService();
            }
            view.hideLoading(null);

        }
    };

    @Override
    public void onConnectingToDevice() {
        Log.d(TAG, "onConnectingToDevice() searchingForDevice? = " + Boolean.toString(searchingForDevice)
                +", connectingToDevice? "+connectingToDevice);
        //Return if found devices hasn't been called before this, and prompt isn't visible
        //The connectingToDevice flag is set once onFoundDevices() has been called
        if (connectingToDevice) {
            view.connectingToDevice();
        }
    }

    private final int FIND_DEVICE_RETRY_TIME = 12;
    private final int FIND_DEVICE_RETRY_AMOUNT = 1;
    private final TimeoutTimer findDeviceTimer = new TimeoutTimer(FIND_DEVICE_RETRY_TIME
            , FIND_DEVICE_RETRY_AMOUNT) {
        @Override
        public void onRetry() {
            Log.d(TAG, "onRetry()");
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                    , MixpanelHelper.ADD_CAR_BLUETOOTH_RETRY);
            if (view != null) {
                Disposable d = view.getBluetoothService().take(1)
                        .subscribe((next) -> next.requestDeviceSearch(true, true
                                , deviceType));
            }
        }

        @Override
        public void onTimeout() {
            Log.d(TAG,"findDeviceTimer.onTimeout()");

            if (view == null) return;

            searchingForDevice = false;
            view.onCannotFindDevice();
            if (view.isBluetoothServiceRunning()){
                view.endBluetoothService();
            }
            view.hideLoading(null);
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                    , MixpanelHelper.FAIL);
        }

    };


    public DeviceSearchPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){

        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void setBluetoothConnectionObservable(
            BluetoothConnectionObservable bluetoothConnectionObservable){

        //If view is subscribed, subscribe to bluetooth service
        if (view != null){
            bluetoothConnectionObservable.subscribe(this);
        }
    }

    public void subscribe(DeviceSearchView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
        if (view != null)
            view.getBluetoothService().take(1).subscribe((next)->next.subscribe(this));
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");

        if (view != null)
            view.getBluetoothService().take(1).subscribe((next)->next.unsubscribe(this));
        findDeviceTimer.cancel();
        connectionTimer.cancel();
        getVinTimer.cancel();

        searchingForVin = false;
        connectingToDevice = false;
        searchingForVin = false;
        this.view = null;
    }

    public void startSearch(){
        Log.d(TAG,"startSearch()");

        if (view == null) return;

        //Already searching, no need to start another search
        if (searchingForDevice) return;

        //Start service if not already running
        if (!view.isBluetoothServiceRunning()){
            view.startBluetoothService();
        }

        switch(view.getDeviceType()){
            case "215B":
                deviceType = BluetoothDeviceManager.DeviceType.OBD215;
                break;
            case "212B":
                deviceType = BluetoothDeviceManager.DeviceType.OBD212;
                break;
            case "ELM327":
                deviceType = BluetoothDeviceManager.DeviceType.ELM327;
                break;
            case "RVD Continental":
                deviceType = BluetoothDeviceManager.DeviceType.RVD;
                break;
            default:
                deviceType = BluetoothDeviceManager.DeviceType.OBD215;
        }

        if (deviceType == BluetoothDeviceManager.DeviceType.RVD){
            view.displayBindingDialog(new BindingDialog.AnswerListener() {
                @Override
                public void onAnswerProvided(@NotNull String answer, @NotNull BindingQuestion question) {
                    Log.d(TAG,"onAnswerProvided() answer: "+answer+", question: "+question);
                    Disposable d = view.getBluetoothService().take(1)
                            .subscribe(next -> next.answerBindingQuestion(question.questionType,answer));
                }

                @Override
                public void onBackPressed(@NotNull BindingQuestion question) {
                    Log.d(TAG,"onBackPressed()");
                    Disposable d = view.getBluetoothService().take(1)
                            .subscribe(next -> next.answerBindingQuestion(question.questionType,BindingQuestion.BIDING_ANSWER_BACK));
                }

                @Override
                public void onCancelPressed(@NotNull BindingQuestion question) {
                    Log.d(TAG,"onCancelPressed()");
                    Disposable d = view.getBluetoothService().take(1)
                            .subscribe(next -> next.cancelBinding());
                }
            });
            view.displayBindingQuestion(new BindingQuestion(EBindingQuestionType.VIN,"Please enter your car's VIN"));
            view.displayBindingProgress(0.4f);
            return;
        }

        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                , MixpanelHelper.PENDING);

        //Check if mileage is valid
        if (!AddCarUtils.isMileageValid(view.getMileage())){
            view.onMileageInvalid();
            return;
        }
        //Check if permissions are granted
        if (!view.checkPermissions()){
            Log.d(TAG,"Permissions not granted, cannot start search");
            return;
        }

        //Check if already connected to device
        Disposable d = view.getBluetoothService().take(1).subscribe((next) -> {
            if (next.getDeviceState()
                    .equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){

                mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                        , MixpanelHelper.SUCCESS);

                readyDevice = next.getReadyDevice();

                //Check if retrieved VIN is valid, otherwise begin timer
                if (!AddCarUtils.isVinValid(readyDevice.getVin())){
                    view.showLoading(((android.support.v4.app.Fragment)view).getString(R.string.getting_vin));
                    searchingForVin = true;
                    next.requestVin();
                    getVinTimer.start();
                }

                //Add the car
                else{
                    addCar(readyDevice);
                }

            }
            //Otherwise request search and wait for callback
            else{
                //Try to start search or check if state isn't disconnected and therefore already searching
                if (next.requestDeviceSearch(true, true, deviceType)
                        || !next.getDeviceState().equals(BluetoothConnectionObservable.State.DISCONNECTED)){
                    view.showLoading(((android.support.v4.app.Fragment)view).getString(R.string.searching_for_device_action_bar));
                    searchingForDevice = true;
                    findDeviceTimer.start();

                } else{
                    view.displayToast(R.string.request_search_failed_add_car_message);
                }
            }
        });
    }

    @Override
    public void onSearchingForDevice() {
        Log.d(TAG,"onSearchingForDevice()");
    }

    @Override
    public void onDeviceReady(ReadyDevice readyDevice) {
        Log.d(TAG,"onDeviceReady()");
        if (view == null) return;


        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH
                , MixpanelHelper.SUCCESS);
        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                , MixpanelHelper.PENDING);

        connectingToDevice = false;
        searchingForDevice = false;
        connectionTimer.cancel();
        findDeviceTimer.cancel();
        this.readyDevice = readyDevice;

        //Check for valid vin
        if (AddCarUtils.isVinValid(readyDevice.getVin())){

            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.SUCCESS);

            searchingForVin = false;
            //Begin  adding car
            addCar(readyDevice);

        }
        else{
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN
                    , MixpanelHelper.PENDING);

            view.showLoading(((android.support.v4.app.Fragment)view).getString(R.string.getting_vin));

            //Try to get valid VIN
            searchingForVin = true;
            Disposable d = view.getBluetoothService()
                    .take(1)
                    .subscribe((next)->{
                        next.requestVin();
                    });
            getVinTimer.start();
        }
    }

    @Override
    public void onDeviceDisconnected() {
        Log.d(TAG,"onDeviceDisconnected() searchingForVin? "+searchingForVin+", connectingToDevice? "+connectingToDevice);
        if (connectingToDevice){
            connectingToDevice = false;
            connectionTimer.cancel();
            view.onCouldNotConnectToDevice();
            if (view.isBluetoothServiceRunning()){
                view.endBluetoothService();
            }
            view.hideLoading(null);
        }
        if (searchingForVin){
            searchingForVin = false;
            getVinTimer.cancel();
            view.onCouldNotConnectToDevice();
            if (view.isBluetoothServiceRunning()){
                view.endBluetoothService();
            }
            view.hideLoading(null);
        }
    }

    @Override
    public void onFoundDevices() {
        Log.d(TAG, "onFoundDevices() searchingForDevice? = " + Boolean.toString(searchingForDevice));
        if (searchingForDevice) {
            searchingForDevice = false;
            findDeviceTimer.cancel();
            connectingToDevice = true;
            connectionTimer.start();
            view.showLoading(R.string.found_devices);
        }

    }

    @Override
    public void onGotPid(PidPackage pidPackage) {
        Log.d(TAG,"onGotPid() pidPackage: "+pidPackage);
    }

    @Override
    public void onDeviceVerifying() {
        Log.d(TAG,"onDeviceVerifying()");
        if (view == null) return;
        if (!searchingForDevice) return;
        Log.wtf(TAG, "error, verifying in add car, should not happen");
        view.showLoading(((android.support.v4.app.Fragment)view)
                .getString(R.string.verifying_device_action_bar));
    }

    @Override
    public void onDeviceSyncing() {
        Log.d(TAG,"onDeviceDisconnected()");
    }

    private void addCar(ReadyDevice readyDevice){
        Log.d(TAG,"addCar() addingCar?"+addingCar);
        //Dont allow two adds
        if (addingCar) return;
        //Check whether mileage is valid again, just in case it somehow changed
        if (!AddCarUtils.isMileageValid(view.getMileage())){
            view.onMileageInvalid();
            return;
        }
        view.showLoading(((android.support.v4.app.Fragment)view).getString(R.string.saving_car_message));

        addingCar = true;
        double mileage = Double.valueOf(view.getMileage());

        useCaseComponent.addCarUseCase().execute(readyDevice.getVin(), mileage
                , readyDevice.getScannerId(), readyDevice.getScannerName()
                , EventSource.SOURCE_ADD_CAR, new AddCarUseCase.Callback() {

                    @Override
                    public void onCarAlreadyAdded(Car car){
                        Log.d(TAG,"addCarUseCase().onCarAlreadyAdded() car: "+car);
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SAVE_TO_SERVER
                                ,MixpanelHelper.ADD_CAR_CAR_EXISTS);
                        addingCar = false;
                        if (view == null) return;

                        view.onCarAlreadyAdded(car);
                        view.hideLoading(null);
                    }

                    @Override
                    public void onCarAddedWithBackendShop(Car car) {
                        Log.d(TAG,"addCarUseCase().onCarAddedWithBackendShop() car: "+car);
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SAVE_TO_SERVER
                                ,MixpanelHelper.SUCCESS);
                        addingCar = false;
                        if (view == null) return;

                        view.onCarAddedWithShop(car);
                        view.hideLoading(((android.support.v4.app.Fragment)view).getString(R.string.car_added_successfully_toast_message));
                    }

                    @Override
                    public void onCarAdded(Car car) {
                        Log.d(TAG,"addCarUseCase().onCarAdded() car: "+car);
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SAVE_TO_SERVER
                                ,MixpanelHelper.SUCCESS);
                        addingCar = false;
                        if (view == null) return;

                        view.onCarAddedWithoutShop(car);
                        view.hideLoading(((android.support.v4.app.Fragment)view).getString(R.string.car_added_successfully_toast_message));
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"addCarUseCase().onError() error: "+error.getMessage());
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SAVE_TO_SERVER
                                ,MixpanelHelper.FAIL);
                        addingCar = false;
                        if (view == null) return;

                        if (error.getError().equals(RequestError.ERR_OFFLINE)){
                            view.beginPendingAddCarActivity(readyDevice.getVin(),mileage
                                    ,readyDevice.getScannerId());
                            view.hideLoading(((android.support.v4.app.Fragment)view).getString(R.string.connect_to_internet_toast_message));
                        }else if (error.getError().equals(RequestError.ERR_UNKNOWN)){
                            view.onErrorAddingCar(((Fragment) view).getContext().getString(R.string.unexpected_car_adding_error_message));
                            view.hideLoading(null);
                        } else{
                            view.onErrorAddingCar(error.getMessage());
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
                    , MixpanelHelper.SUCCESS);
            getVinTimer.cancel();
            searchingForVin = false;
            readyDevice.setVin(vin);
            addCar(readyDevice);
        }
    }

    public void onProgressDialogKeyPressed(int keyCode){
        Log.d(TAG,"onProgressDialogKeyPressed(), backButton?"+(keyCode == KeyEvent.KEYCODE_BACK));
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
        }
    }

    public void onGotPendingActivityResults(String vin, int mileage, String scannerId
            , String scannerName){

        Log.d(TAG,"onGotPendingActivityResults() vin: "+vin+", mileage:"+mileage+", scannerId:"
                +scannerId+", scanner");
        if (view == null) return;
        readyDevice.setVin(vin);
        readyDevice.setScannerId(scannerId);
        readyDevice.setScannerName(scannerName);
        addCar(readyDevice);
    }

    public void onBackPressed(){
        Log.d(TAG,"onBackPressed() searchingForVin?"+searchingForVin+", searchingForDevice?"
                +searchingForDevice+", addingCar?"+addingCar);

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
        else if (connectingToDevice){
            connectionTimer.cancel();
            connectingToDevice = false;
            view.hideLoading("");
        }

        else if (addingCar){
            //Do nothing
        }
        else{
            view.showAskHasDeviceView();
            view.hideLoading("");
        }

    }

    @Override
    public void onGotSuportedPIDs(String value) {

    }

    @Override
    public void onBindingRequired() {

    }

    @Override
    public void onBindingQuestionPrompted(@NotNull BindingQuestion question) {

    }

    @Override
    public void onBindingProgress(float progress) {

    }

    @Override
    public void onBindingFinished() {

    }

    @Override
    public void onBindingError(@NotNull Error err) {

    }

    @Override
    public void onFirmwareInstallationRequired() {

    }

    @Override
    public void onFirmwareInstallationProgress(float progress) {

    }

    @Override
    public void onFirmwareInstallationFinished() {

    }

    @Override
    public void onFirmwareInstallationError(@NotNull Error error) {

    }
}
