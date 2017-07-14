package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.models.Car;
import com.pitstop.models.ObdScanner;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public class HandleVinOnConnectUseCaseImpl implements HandleVinOnConnectUseCase {

    private ScannerRepository scannerRepository;
    private UserRepository userRepository;
    private Handler handler;
    private Callback callback;
    private ParameterPackage parameterPackage;

    public HandleVinOnConnectUseCaseImpl(ScannerRepository scannerRepository
            , UserRepository userRepository, Handler handler){
        this.scannerRepository = scannerRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(ParameterPackage parameterPackage, Callback callback) {
        this.parameterPackage = parameterPackage;
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {
        if (!parameterPackage.paramType.equals(ParameterPackage.ParamType.VIN)){
            callback.onError();
            return;
        }

        final String deviceVin = parameterPackage.value;
        final String deviceId = parameterPackage.deviceId;

        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {
            @Override
            public void onGotCar(Car car) {

                //Device VIN invalid, get a different one
                if (!car.getVin().equals(deviceVin)){
                    callback.onDeviceInvalid();
                    return;
                }

                boolean carScannerValid = car.getScannerId() != null
                        && car.getScannerId().isEmpty()
                        && car.getScannerId().equals(deviceId);
                boolean deviceIdValid = deviceId != null
                        && !deviceId.isEmpty();

                //Car has a valid scanner so nothing needs to be done
                if (carScannerValid){
                    callback.onSuccess();
                    return;
                }

                /*Otherwise a scanner needs to be created there are two cases
                * 1. User has a good scanner, but the car doesn't have it stored
                * 2. User has a broken scanner, and the car doesn't have it stored
                * In case 2 we need to wait for the user to override the ID, this isn't addressed
                * in this use case. YET*/

                //Case 2, address this later
                if (!carScannerValid && !deviceIdValid){
                    callback.onDeviceIdOverrideNeeded();
                    return;
                }
                //Anything below is case 1

                /*We need to check whether the car has no scanner at all, or whether it is being changed
                * If the scanner is being changed, the old one needs to be deactived*/

                ObdScanner obdScanner = new ObdScanner(car.getId(),deviceId); //Scanner to be added
                obdScanner.setStatus(true); //Set to active

                if (car.getScannerId() != null && !car.getScannerId().isEmpty()){
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
                                    callback.onDeviceAlreadyActive();
                                }

                                @Override
                                public void onScannerCreated() {
                                    callback.onSuccess();
                                }

                                @Override
                                public void onError() {
                                    callback.onError();
                                }
                            });
                        }

                        @Override
                        public void onError(int error) {
                            callback.onError();
                        }
                    });

                }

                //Car does not have a scanner so simply create one
                else{
                    addScanner(obdScanner, new AddScannerCallback() {
                        @Override
                        public void onDeviceAlreadyActive() {
                            //Another user has this scanner
                            callback.onDeviceAlreadyActive();
                        }

                        @Override
                        public void onScannerCreated() {
                            //Scanner created
                            callback.onSuccess();
                        }

                        @Override
                        public void onError() {
                            callback.onError();
                        }
                    });
                }

            }

            @Override
            public void onNoCarSet() {
                callback.onError();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });

    }

    interface AddScannerCallback {
        void onDeviceAlreadyActive();
        void onScannerCreated();
        void onError();
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
                    public void onError(int error) {
                        callback.onError();
                    }
                });

            }

            @Override
            public void onError(int error) {
                callback.onError();
            }
        });
    }


}
