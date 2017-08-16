package com.pitstop.bluetooth.handler;

import android.content.Context;

import com.pitstop.bluetooth.BluetoothMixpanelTracker;
import com.pitstop.observer.DeviceVerificationObserver;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MixpanelHelper;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Karol Zdebel on 8/16/2017.
 */

public class VinDataHandler {

    private final String TAG = getClass().getSimpleName();

    private Context context;
    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private BluetoothMixpanelTracker bluetoothMixpanelTracker;
    private DeviceVerificationObserver deviceVerificationObserver;
    private UseCaseComponent useCaseComponent;

    private boolean verificationInProgress = false;

    public VinDataHandler(Context context, BluetoothConnectionObservable bluetoothConnectionObservable
            , BluetoothMixpanelTracker bluetoothMixpanelTracker
            , DeviceVerificationObserver deviceVerificationObserver){

        this.context = context;
        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
        this.bluetoothMixpanelTracker = bluetoothMixpanelTracker;
        this.deviceVerificationObserver = deviceVerificationObserver;
        this.useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
    }

    public void handleVinData(String vin, String deviceId, boolean ignoreVerification){

        bluetoothConnectionObservable.notifyVin(vin);
        bluetoothMixpanelTracker.trackBluetoothEvent(MixpanelHelper.BT_VIN_GOT,deviceId
                ,vin);
        boolean deviceIsVerified = bluetoothConnectionObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.CONNECTED);

        //If adding car connect to first recognized device
        if (ignoreVerification && !deviceIsVerified){
            LogUtils.debugLogD(TAG, "ignoreVerification = true, setting deviceConState to CONNECTED"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            deviceVerificationObserver.onVerificationSuccess(vin,deviceId);
        }
        //Check to see if VIN is correct, unless adding a car then no comparison is needed
        else if(!ignoreVerification && !verificationInProgress && !deviceIsVerified
                && !bluetoothConnectionObservable.getDeviceState().equals(BluetoothConnectionObservable.State.DISCONNECTED)){

            //Device verification starting
            bluetoothConnectionObservable.notifyVerifyingDevice();
            verificationInProgress = true;

            useCaseComponent.handleVinOnConnectUseCase().execute(vin,deviceId
                    , new HandleVinOnConnectUseCase.Callback() {
                @Override
                public void onSuccess() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect: Success" +
                                    ", ignoreVerification?"+ignoreVerification
                            , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    deviceVerificationObserver.onVerificationSuccess(vin,deviceId);
                }

                @Override
                public void onDeviceBrokenAndCarMissingScanner() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device ID needs to be overriden"
                                    +"ignoreVerification?"+ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    deviceVerificationObserver.onVerificationDeviceBrokenAndCarMissingScanner(
                            vin,deviceId);
                }

                @Override
                public void onDeviceBrokenAndCarHasScanner(String scannerId) {
                    LogUtils.debugLogD(TAG, "Device missing id but user car has a scanner" +
                                    ", overwriting scanner id to "+scannerId+", ignoreVerification: "
                                    +ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    deviceVerificationObserver.onVerificationDeviceBrokenAndCarHasScanner(
                            vin,scannerId);
                }

                @Override
                public void onDeviceInvalid() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device is invalid." +
                                    " ignoreVerification?"+ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    deviceVerificationObserver.onVerificationDeviceInvalid();
                }

                @Override
                public void onDeviceAlreadyActive() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device is already active" +
                                    ", ignoreVerification?"+ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    deviceVerificationObserver.onVerificationDeviceAlreadyActive();

                }

                @Override
                public void onError(RequestError error) {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect error occurred" +
                                    ", ignoreVerification?"+ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
                    deviceVerificationObserver.onVerificationError();

                }
            });

        }
    }
}
