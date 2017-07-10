package com.pitstop.interactors;

import android.os.Handler;

import com.pitstop.models.Car;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public class FoundBluetoothDeviceUseCaseImpl implements FoundBluetoothDeviceUseCase {

    private UserRepository userRepository;
    private Handler handler;
    private Callback callback;
    private String scannerId;
    private String scannerName;

    public FoundBluetoothDeviceUseCaseImpl(UserRepository userRepository, Handler handler) {

        this.userRepository = userRepository;
        this.handler = handler;
    }

    @Override
    public void execute(String scannerId, String scannerName, Callback callback) {
        this.scannerId = scannerId;
        this.scannerName = scannerName;
        this.callback = callback;
        handler.post(this);
    }

    @Override
    public void run() {

        //Not  IDD device
        if (scannerName == null || !scannerName.contains(BT_DEVICE_NAME)){
            callback.onDeviceNotMatch();
            return;
        }

        userRepository.getUserCar(new UserRepository.UserGetCarCallback() {
            @Override
            public void onGotCar(Car car) {

                //Car doesn't have scanner so connect to device
                if (car.getScannerId().isEmpty()){
                    callback.onDeviceNotMatch();
                }

                //Match between found device and current cars scannerID
                else if (car.getScannerId().equals(scannerId)){
                    callback.onDeviceMatchesCurrentCar();
                }

                //Car has device that could POSSIBLY be this device
                else if (scannerName.startsWith(BT_DEVICE_NAME_212)
                        && scannerName.endsWith(BT_DEVICE_NAME_BROKEN)){

                    callback.onDevice215Broken();
                }

                //Not a match, certainly
                else{
                    callback.onDeviceNotMatch();
                }
            }

            @Override
            public void onNoCarSet() {
                callback.onDeviceNotMatch();
            }

            @Override
            public void onError() {
                callback.onError();
            }
        });


    }
}
