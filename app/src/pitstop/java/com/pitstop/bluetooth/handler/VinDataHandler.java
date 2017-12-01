package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.util.Log;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.network.RequestError;
import com.pitstop.observer.DeviceVerificationObserver;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public class VinDataHandler{

    private final String TAG = getClass().getSimpleName();

    private BluetoothDataHandlerManager bluetoothDataHandlerManager;
    private DeviceVerificationObserver deviceVerificationObserver;
    private UseCaseComponent useCaseComponent;

    private boolean verificationInProgress = false;
    private String vinBeingVerified = "";

    public VinDataHandler(Context context, BluetoothDataHandlerManager bluetoothDataHandlerManager
            , DeviceVerificationObserver deviceVerificationObserver){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
        this.deviceVerificationObserver = deviceVerificationObserver;
        this.useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
    }

    public void vinVerificationFlagChange(boolean ignoreVerification){
        if (ignoreVerification && verificationInProgress){
            deviceVerificationObserver.onVerificationSuccess(vinBeingVerified);
            verificationInProgress = false;
        }
    }

    public void handleVinData(String vin, String deviceId, boolean ignoreVerification){
        Log.d(TAG,"handleVinData() vin:"+vin+", deviceId:"+deviceId
                +", ignoreVerification?"+ignoreVerification);

        bluetoothDataHandlerManager.onHandlerReadVin(vin);
        boolean deviceIsVerified = bluetoothDataHandlerManager.isDeviceVerified();

        //If adding car connect to first recognized device
        if (ignoreVerification && !deviceIsVerified){
            deviceVerificationObserver.onVerificationSuccess(vin);
        }
        //Check to see if VIN is correct, unless adding a car then no comparison is needed
        else if(!ignoreVerification && !verificationInProgress && !deviceIsVerified){
            vinBeingVerified = vin;
            bluetoothDataHandlerManager.onHandlerVerifyingDevice();
            verificationInProgress = true;

            useCaseComponent.handleVinOnConnectUseCase().execute(vin,deviceId
                    , new HandleVinOnConnectUseCase.Callback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "handleVinOnConnect: Success" +
                                    ", ignoreVerification?"
                                    +ignoreVerification);
                    if (!verificationInProgress) return;
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationSuccess(vin);
                }

                @Override
                public void onDeviceBrokenAndCarMissingScanner() {
                    Log.d(TAG, "handleVinOnConnect Device ID needs to be overriden"
                                    +"ignoreVerification?"
                                    +ignoreVerification);
                    if (!verificationInProgress) return;
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationDeviceBrokenAndCarMissingScanner(vin);
                }

                @Override
                public void onDeviceBrokenAndCarHasScanner(String scannerId) {
                    Log.d(TAG, "Device missing id but user car has a scanner" +
                                    ", overwriting scanner id to "+scannerId+", ignoreVerification: "
                                    +ignoreVerification);
                    if (!verificationInProgress) return;
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationDeviceBrokenAndCarHasScanner(
                            vin,scannerId);
                }

                @Override
                public void onDeviceInvalid() {
                    Log.d(TAG, "handleVinOnConnect Device is invalid." +
                                    " ignoreVerification?"
                                    +ignoreVerification);
                    if (!verificationInProgress) return;
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationDeviceInvalid(vin);
                }

                @Override
                public void onDeviceAlreadyActive() {
                    Log.d(TAG, "handleVinOnConnect Device is already active" +
                                    ", ignoreVerification?"
                                    +ignoreVerification);
                    if (!verificationInProgress) return;
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationDeviceAlreadyActive(vin);
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG, "handleVinOnConnect error occurred" +
                                    ", ignoreVerification?");
                    if (!verificationInProgress) return;
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationError(vin);

                }
            });

        }
    }
}
