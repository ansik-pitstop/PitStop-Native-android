package com.pitstop.interactors.other;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.ObdScanner;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.RepositoryResponse;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public class HandleVinOnConnectUseCaseImpl implements HandleVinOnConnectUseCase {

    private final String TAG = getClass().getSimpleName();

    private ScannerRepository scannerRepository;
    private UserRepository userRepository;
    private CarRepository carRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private Callback callback;
    private String vin;
    private String deviceId;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public HandleVinOnConnectUseCaseImpl(ScannerRepository scannerRepository
            ,CarRepository carRepository, UserRepository userRepository, Handler useCaseHandler
            , Handler mainHandler){

        this.scannerRepository = scannerRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(String vin, String deviceId, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case started execution: vin="+vin+", deviceId="+deviceId
                , DebugMessage.TYPE_USE_CASE);
        this.vin = vin;
        this.deviceId = deviceId;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onSuccess(){
        Logger.getInstance().logI(TAG,"Use case finished: success"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onSuccess());
    }

    private void onDeviceBrokenAndCarMissingScanner(){
        Logger.getInstance().logI(TAG,"Use case finished: device broken and car missing scanner"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onDeviceBrokenAndCarMissingScanner());
    }

    private void onDeviceBrokenAndCarHasScanner(String scannerId){
        Logger.getInstance().logI(TAG,"Use case finished: device broken and car has scanner"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onDeviceBrokenAndCarHasScanner(scannerId));
    }

    private void onDeviceInvalid(){
        Logger.getInstance().logI(TAG,"Use case finished: device invalid"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onDeviceInvalid());
    }

    private void onDeviceAlreadyActive(){
        Logger.getInstance().logI(TAG,"Use case finished: device already active"
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onDeviceAlreadyActive());
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        compositeDisposable.clear();
        mainHandler.post(() -> callback.onError(error));
    }

    private boolean isValidVin(final String vin) {
        return vin != null && (vin.length() == 17);
    }

    @Override
    public void run() {

        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                //user has no car set
                if (!data.hasMainCar()){
                    HandleVinOnConnectUseCaseImpl.this.onError(RequestError.getUnknownError());
                    return;
                }
                boolean[] usedLocal = new boolean[1];
                usedLocal[0] = false;
                //Get user car
                Disposable disposable = carRepository.get(data.getCarId(), Repository.DATABASE_TYPE.BOTH)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.computation(),true)
                    .doOnError(err -> HandleVinOnConnectUseCaseImpl.this.onError(new RequestError(err)))
                    .doOnNext(response -> {

                        //Use local data if present and scanner id is present
                        if (usedLocal[0] && !response.isLocal() ){
                            //Don't process remote data because local has been used
                            return;
                        }else if (!response.isLocal()){
                            Log.d(TAG,"using remote response");
                        }

                        if (response.isLocal() && response.getData() != null
                                && response.getData().getScannerId() != null
                                && !response.getData().getScannerId().isEmpty()){
                            //Set used local flag so the remote response isn't processed
                            usedLocal[0] = true;
                            Log.d(TAG,"using local response scanner: "+response.getData().getScannerId());
                        }else if (response.isLocal() && response.getData() == null
                                || response.getData().getScannerId() == null
                                || response.getData().getScannerId().isEmpty()){
                            //Invalid local data so return
                            return;
                        }


                        //Remote data is invalid
                        if (response.getData() == null){
                            Log.d(TAG,"Received empty car response.");
                            HandleVinOnConnectUseCaseImpl.this.onError(RequestError.getUnknownError());
                            return;
                        }
                        Car car = response.getData();
                        boolean deviceVinValid = vin != null
                                && (vin.length() == 17);
                        boolean carScannerValid = car.getScannerId() != null
                                && !car.getScannerId().isEmpty()
                                && car.getScannerId().equals(deviceId);
                        boolean carScannerExists = car.getScannerId() != null
                                && !car.getScannerId().isEmpty();
                        boolean deviceIdValid = deviceId != null
                                && !deviceId.isEmpty();

                        //Car has a valid scanner so nothing needs to be done
                        if (carScannerValid){
                            Log.d(TAG,"Car scanner matches, onSuccess()");
                            HandleVinOnConnectUseCaseImpl.this.onSuccess();
                            return;
                        }

                        /*Invalid vin and device, connect to the device by default
                        *since there is no way to verify it, and most users have one device*/
                        if (!deviceIdValid && !deviceVinValid){
                            Log.d(TAG,"No device id and no VIN, onSuccess()");
                            HandleVinOnConnectUseCaseImpl.this.onSuccess();
                            return;
                        }

                        if (deviceVinValid && !car.getVin().equals(vin)){
                            Log.d(TAG,"Device vin is valid but does not match car VIN");
                            HandleVinOnConnectUseCaseImpl.this.onDeviceInvalid();
                            return;
                        }

                            /*Otherwise a scanner needs to be created there are three cases
                            * 1. User has a good scanner, but the car doesn't have it stored
                            *   1.1 Vin is valid (Returned properly by device)
                            *   1.2 Vin is invalid (Not supported or not being returned)
                            *   Both of the above cases will be treated the same for now.
                            * 2. User has a broken scanner, and the car doesn't have it stored
                            * 3. User has a broken scanner and the car has it stored
                            * In case 2 we need to wait for the user to override the ID, this isn't addressed
                            * in this use case. YET*/

                        //Case 3, use car scanner id as the device id
                        if (carScannerExists && !deviceIdValid){
                            HandleVinOnConnectUseCaseImpl.this.onDeviceBrokenAndCarHasScanner(car.getScannerId());
                            return;
                        }

                        //Case 2, address this later
                        if (!carScannerExists && !deviceIdValid){
                            Log.d(TAG,"No car scanner and device id invalid, onDeviceBrokenAndCarMissingScanner()");

                            //Check if car has a scanner id, if so use that one
                            HandleVinOnConnectUseCaseImpl.this.onDeviceBrokenAndCarMissingScanner();
                            return;
                        }

                        //Anything below is case 1

                        boolean DeviceIdValidDeviceVinNotValidCarHasNoScanner[] = new boolean[1];
                        //1.1 Check if VINs match, scanner still not present on car so add it
                        // in logic below, but don't produce another return
                        if (deviceVinValid && vin.equals(car.getVin())){
                            DeviceIdValidDeviceVinNotValidCarHasNoScanner[0] = false;
                            Log.d(TAG,"Vin matches");
                            HandleVinOnConnectUseCaseImpl.this.onSuccess();
                        }
                        //Device id is valid and device vin is not valid and car has no scanner, so we go to server to add it to the car
                        // and based on the return we either allow for connection or if offline error or other error we don't connect
                        else{
                            Log.d(TAG,"Device id valid, device vin not valid, car has no scanner");
                            DeviceIdValidDeviceVinNotValidCarHasNoScanner[0] = true;
                        }

                        /*We need to check whether the car has no scanner at all, or whether it is being changed
                        '* If the scanner is being changed, the old one needs to be deactived*/

                        ObdScanner obdScanner = new ObdScanner(car.getId(),deviceId); //Scanner to be added
                        obdScanner.setStatus(true); //Set to active

                        if (carScannerExists){
                            ObdScanner oldCarScanner = new ObdScanner(car.getId(),car.getScannerId());
                            oldCarScanner.setStatus(false); //Set to inactive

                            scannerRepository.updateScanner(oldCarScanner, new Repository.Callback<Object>() {
                                @Override
                                public void onSuccess(Object data) {

                                    //Scanner set to inactive, now add the new one
                                    addScanner(obdScanner, new AddScannerCallback() {
                                        @Override
                                        public void onDeviceAlreadyActive() {
                                            //Another user has this scanner
                                            Log.d(TAG,"Adding scanner that is already active, onDeviceAlreadyActive()");
                                            if (DeviceIdValidDeviceVinNotValidCarHasNoScanner[1]){
                                                HandleVinOnConnectUseCaseImpl.this.onDeviceAlreadyActive();
                                            }
                                        }

                                        @Override
                                        public void onScannerCreated() {
                                            Log.d(TAG,"Overwrote scanner id, onSuccess()");
                                            if (DeviceIdValidDeviceVinNotValidCarHasNoScanner[1]){
                                                HandleVinOnConnectUseCaseImpl.this.onSuccess();
                                            }
                                        }

                                        @Override
                                        public void onError(RequestError error) {
                                            if (DeviceIdValidDeviceVinNotValidCarHasNoScanner[1]){
                                                HandleVinOnConnectUseCaseImpl.this.onError(error);
                                            }}
                                    });
                                }

                                @Override
                                public void onError(RequestError error) {
                                    if (DeviceIdValidDeviceVinNotValidCarHasNoScanner[1]){
                                        HandleVinOnConnectUseCaseImpl.this.onError(error);
                                    }
                                }
                            });

                        }

                        //Car does not have a scanner so simply create one
                        else{
                            addScanner(obdScanner, new AddScannerCallback() {
                                @Override
                                public void onDeviceAlreadyActive() {
                                    //Another user has this scanner
                                    if (DeviceIdValidDeviceVinNotValidCarHasNoScanner[1]){
                                        HandleVinOnConnectUseCaseImpl.this.onDeviceAlreadyActive();
                                    }
                                }

                                @Override
                                public void onScannerCreated() {
                                    Log.d(TAG,"Created new scanner, onSuccess()");
                                    //Scanner created
                                    if (DeviceIdValidDeviceVinNotValidCarHasNoScanner[1]){
                                        HandleVinOnConnectUseCaseImpl.this.onSuccess();
                                    }
                                }

                                @Override
                                public void onError(RequestError error) {
                                    if (DeviceIdValidDeviceVinNotValidCarHasNoScanner[1]){
                                        HandleVinOnConnectUseCaseImpl.this.onError(error);
                                    }
                                }
                            });
                        }

                    }).onErrorReturn(err -> new RepositoryResponse<>(null,false))
                    .subscribe();
                compositeDisposable.add(disposable);
            }

            @Override
            public void onError(RequestError error) {
                HandleVinOnConnectUseCaseImpl.this.onError(error);
            }
        });
    }

    interface AddScannerCallback {
        void onDeviceAlreadyActive();
        void onScannerCreated();
        void onError(RequestError error);
    }

    //Add scanner, but first verify that it isn't already active
    private void addScanner(ObdScanner obdScanner, AddScannerCallback callback){
        scannerRepository.getScanner(obdScanner.getScannerId(), new Repository.Callback<ObdScanner>() {

            @Override
            public void onSuccess(ObdScanner data) {

                //device exists and is already active, do not store
                if (data != null && data.getStatus()){
                    callback.onDeviceAlreadyActive();
                    return;
                }


                //Create scanner otherwise
                obdScanner.setStatus(true);
                scannerRepository.createScanner(obdScanner, new Repository.Callback() {

                    @Override
                    public void onSuccess(Object data) {
                        callback.onScannerCreated();
                    }

                    @Override
                    public void onError(RequestError error) {
                        callback.onError(error);
                    }
                });

            }

            @Override
            public void onError(RequestError error) {
                callback.onError(error);
            }
        });
    }


}
