package com.pitstop.bluetooth.handler;

import android.content.Context;

import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.observer.DeviceVerificationObserver;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MixpanelHelper;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public class VinDataHandler{

    private final String TAG = getClass().getSimpleName();

    private BluetoothDataHandlerManager bluetoothDataHandlerManager;
    private DeviceVerificationObserver deviceVerificationObserver;
    private UseCaseComponent useCaseComponent;

    private boolean verificationInProgress = false;

    public VinDataHandler(Context context, BluetoothDataHandlerManager bluetoothDataHandlerManager
            , DeviceVerificationObserver deviceVerificationObserver){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
        this.deviceVerificationObserver = deviceVerificationObserver;
        this.useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
    }

    public void handleVinData(String vin, String deviceId){

        bluetoothDataHandlerManager.onHandlerReadVin(vin);

        bluetoothDataHandlerManager.trackBluetoothEvent(MixpanelHelper.BT_VIN_GOT,deviceId
                ,vin);
        boolean deviceIsVerified = bluetoothDataHandlerManager.isDeviceVerified();

        //If adding car connect to first recognized device
        if (bluetoothDataHandlerManager.isVerificationIgnored() && !deviceIsVerified){
            LogUtils.debugLogD(TAG, "ignoreVerification = true, setting deviceConState to CONNECTED"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            deviceVerificationObserver.onVerificationSuccess(vin,deviceId);
        }
        //Check to see if VIN is correct, unless adding a car then no comparison is needed
        else if(!bluetoothDataHandlerManager.isVerificationIgnored()
                && !verificationInProgress && !deviceIsVerified
                && bluetoothDataHandlerManager.isConnectionInProgress()){

            //Device verification starting
            bluetoothDataHandlerManager.onHandlerVerifyingDevice();
            verificationInProgress = true;

            useCaseComponent.handleVinOnConnectUseCase().execute(vin,deviceId
                    , new HandleVinOnConnectUseCase.Callback() {
                @Override
                public void onSuccess() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect: Success" +
                                    ", ignoreVerification?"
                                    +bluetoothDataHandlerManager.isVerificationIgnored()
                            , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationSuccess(vin,deviceId);
                }

                @Override
                public void onDeviceBrokenAndCarMissingScanner() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device ID needs to be overriden"
                                    +"ignoreVerification?"
                                    +bluetoothDataHandlerManager.isVerificationIgnored()
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationDeviceBrokenAndCarMissingScanner(
                            vin,deviceId);
                }

                @Override
                public void onDeviceBrokenAndCarHasScanner(String scannerId) {
                    LogUtils.debugLogD(TAG, "Device missing id but user car has a scanner" +
                                    ", overwriting scanner id to "+scannerId+", ignoreVerification: "
                                    +bluetoothDataHandlerManager.isVerificationIgnored()
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationDeviceBrokenAndCarHasScanner(
                            vin,scannerId);
                }

                @Override
                public void onDeviceInvalid() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device is invalid." +
                                    " ignoreVerification?"
                                    +bluetoothDataHandlerManager.isVerificationIgnored()
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationDeviceInvalid();
                }

                @Override
                public void onDeviceAlreadyActive() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device is already active" +
                                    ", ignoreVerification?"
                                    +bluetoothDataHandlerManager.isVerificationIgnored()
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationDeviceAlreadyActive();

                }

                @Override
                public void onError(RequestError error) {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect error occurred" +
                                    ", ignoreVerification?"
                                    +bluetoothDataHandlerManager.isVerificationIgnored()
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    verificationInProgress = false;
                    deviceVerificationObserver.onVerificationError();

                }
            });

        }
    }
}
