package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.models.Car;
import com.pitstop.models.ObdScanner;
import com.pitstop.models.Settings;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.CarRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.ScannerRepository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public class HandleVinOnConnectUseCaseImpl implements HandleVinOnConnectUseCase {

    private final String TAG = getClass().getSimpleName();

    private ScannerRepository scannerRepository;
    private UserRepository userRepository;
    private CarRepository carRepository;
    private Handler handler;
    private Callback callback;
    private ParameterPackage parameterPackage;

    public HandleVinOnConnectUseCaseImpl(ScannerRepository scannerRepository
            ,CarRepository carRepository, UserRepository userRepository, Handler handler){

        this.scannerRepository = scannerRepository;
        this.carRepository = carRepository;
        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(ParameterPackage parameterPackage, Callback callback) {
        this.parameterPackage = parameterPackage;
        this.callback = callback;
        handler.post(this);
    }

    private boolean isValidVin(final String vin) {
        return vin != null && (vin.length() == 17);
    }

    @Override
    public void run() {
        //ADD LOGS
        //Log.d(TAG,"")

        if (!parameterPackage.paramType.equals(ParameterPackage.ParamType.VIN)){
            callback.onError(RequestError.getUnknownError());
            return;
        }

        final String deviceVin = parameterPackage.value;
        final String deviceId = parameterPackage.deviceId;

        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {

                //user has no car set
                if (!data.hasMainCar()){
                    callback.onError(RequestError.getUnknownError());
                    return;
                }

                //Get user car
                carRepository.get(data.getCarId(), data.getUserId(), new CarRepository.Callback<Car>() {
                    @Override
                    public void onSuccess(Car car) {

                        boolean carVinValid = deviceVin != null
                                && (deviceVin.length() == 17);
                        boolean carScannerValid = car.getScannerId() != null
                                && !car.getScannerId().isEmpty()
                                && car.getScannerId().equals(deviceId);
                        boolean carScannerExists = car.getScannerId() != null
                                && !car.getScannerId().isEmpty();
                        boolean deviceIdValid = deviceId != null
                                && !deviceId.isEmpty();

                        //Car has a valid scanner so nothing needs to be done
                        if (carScannerValid){
                            callback.onSuccess();
                            return;
                        }

                        //Check if device vin didn't match, only if the car scanner exists
                        if (carScannerExists && !car.getVin().equals(deviceVin)){
                            callback.onDeviceInvalid();
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
                            callback.onDeviceBrokenAndCarHasScanner(car.getScannerId());
                            return;
                        }

                        //Case 2, address this later
                        if (!carScannerExists && !deviceIdValid){

                            //Check if car has a scanner id, if so use that one
                            callback.onDeviceBrokenAndCarMissingScanner();
                            return;
                        }
                        //Anything below is case 1


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
                                            callback.onDeviceAlreadyActive();
                                        }

                                        @Override
                                        public void onScannerCreated() {
                                            callback.onSuccess();
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
                                public void onError(RequestError error) {
                                    callback.onError(error);
                                }
                            });
                        }

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
