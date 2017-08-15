package com.pitstop.bluetooth;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.pitstop.R;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.ReadyDevice;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MixpanelHelper;

import java.util.Arrays;
import java.util.HashSet;

import static com.facebook.FacebookSdk.getApplicationContext;
import static com.pitstop.bluetooth.BluetoothAutoConnectService.notifID;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public class ParameterDataHandler {

    private Context context;
    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private BluetoothMixpanelTracker bluetoothMixpanelTracker;
    private DeviceVerificationObserver deviceVerificationObserver;

    public ParameterDataHandler(Context context, BluetoothConnectionObservable bluetoothConnectionObservable
            , BluetoothMixpanelTracker bluetoothMixpanelTracker
            , DeviceVerificationObserver deviceVerificationObserver) {

        this.context = context;
        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
        this.bluetoothMixpanelTracker = bluetoothMixpanelTracker;
        this.deviceVerificationObserver = deviceVerificationObserver;
    }

    public void handleParameterData(ParameterPackage parameterPackage, long terminalRTCTime
            , boolean ignoreVerification, boolean deviceIsVerified){
        if (parameterPackage == null) return;

        //Change null to empty
        if (parameterPackage.value == null){
            parameterPackage.value = "";
        }

        final String TAG = getClass().getSimpleName() + ".parameterData()";

        LogUtils.debugLogD(TAG, "parameterData() parameterPackage: " + parameterPackage.toString()
                , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

        if (parameterPackage.paramType == ParameterPackage.ParamType.VIN){
            bluetoothMixpanelTracker.trackBluetoothEvent(MixpanelHelper.BT_VIN_GOT,parameterPackage.deviceId
                    ,parameterPackage.value);
        }
        else if (parameterPackage.paramType == ParameterPackage.ParamType.RTC_TIME){
            bluetoothMixpanelTracker.trackBluetoothEvent(MixpanelHelper.BT_RTC_GOT,parameterPackage.deviceId
                    ,parameterPackage.value);
        }

        if (parameterPackage.paramType == ParameterPackage.ParamType.VIN){
            bluetoothConnectionObservable.notifyVin(parameterPackage.value);
        }

        //Get terminal RTC time
        if (parameterPackage.paramType == ParameterPackage.ParamType.RTC_TIME
                && terminalRTCTime == -1 && !ignoreVerification){
            terminalRTCTime = Long.valueOf(parameterPackage.value);

            //Check if device needs to sync rtc time
            final long YEAR = 32000000;
            long currentTime = System.currentTimeMillis() / 1000;
            long deviceRtcTime = Long.valueOf(parameterPackage.value);
            long diff = currentTime - deviceRtcTime;

            //Sync if difference is greater than a year
            if (diff > YEAR){
                notifySyncingDevice();
                syncObdDevice();
            }
            trackBluetoothEvent(MixpanelHelper.BT_SYNCING);
        }

        //If adding car connect to first recognized device
        else if (parameterPackage.paramType == ParameterPackage.ParamType.VIN
                && ignoreVerification && !deviceIsVerified){
            LogUtils.debugLogD(TAG, "ignoreVerification = true, setting deviceConState to CONNECTED"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            deviceVerificationObserver.onVerificationSuccess();

            deviceIsVerified = true;
            verificationInProgress = false;
            deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
            readyDevice = new ReadyDevice(parameterPackage.value,parameterPackage.deviceId
                    ,parameterPackage.deviceId);
            notifyDeviceReady(parameterPackage.value,parameterPackage.deviceId
                    ,parameterPackage.deviceId);
            getSupportedPids();
        }
        //Check to see if VIN is correct, unless adding a car then no comparison is needed
        else if(parameterPackage.paramType == ParameterPackage.ParamType.VIN
                && !ignoreVerification && !verificationInProgress && !deviceConnState.equals(BluetoothConnectionObservable.State.DISCONNECTED)
                && !deviceIsVerified){

            //Device verification starting
            notifyVerifyingDevice();

            verificationInProgress = true;
            deviceConnState = State.VERIFYING;

            useCaseComponent.handleVinOnConnectUseCase().execute(parameterPackage, new HandleVinOnConnectUseCase.Callback() {
                @Override
                public void onSuccess() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect: Success" +
                                    ", ignoreVerification?"+ignoreVerification
                            , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                    //ignore result if verification state changed mid use-case execution
                    if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
                        verificationInProgress = false;
                        return;
                    }
                    //Connected to device mid use-case execution, return
                    else if (ignoreVerification){
                        verificationInProgress = false;
                        if (deviceConnState.equals(State.VERIFYING)){
                            deviceConnState = State.SEARCHING;
                        }
                        return;
                    }

                    deviceVerificationObserver.onVerificationSuccess();

                    deviceIsVerified = true;
                    verificationInProgress = false;
                    deviceManager.onConnectDeviceValid();
                    deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
                    readyDevice = new ReadyDevice(parameterPackage.value,parameterPackage.deviceId
                            ,parameterPackage.deviceId);
                    notifyDeviceReady(parameterPackage.value,parameterPackage.deviceId
                            ,parameterPackage.deviceId);
                    sendConnectedNotification();
                    getSupportedPids(); //Get supported pids once verified
                }

                @Override
                public void onDeviceBrokenAndCarMissingScanner() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device ID needs to be overriden"
                                    +"ignoreVerification?"+ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                    //ignore result if verification state changed mid use-case execution
                    if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
                        verificationInProgress = false;
                        return;
                    }
                    //Connected to device mid use-case execution, return
                    else if (ignoreVerification){
                        verificationInProgress = false;
                        if (deviceConnState.equals(State.VERIFYING)){
                            deviceConnState = State.SEARCHING;
                        }
                        return;
                    }

                    bluetoothMixpanelTracker.trackBluetoothEvent(MixpanelHelper.BT_DEVICE_BROKEN);
                    deviceVerificationObserver.onVerificationDeviceBrokenAndCarMissingScanner();

                    MainActivity.allowDeviceOverwrite = true;
                    deviceIsVerified = true;
                    verificationInProgress = false;
                    deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
                    deviceManager.onConnectDeviceValid();
                    notifyDeviceNeedsOverwrite();
                    readyDevice = new ReadyDevice(parameterPackage.value,parameterPackage.deviceId
                            ,parameterPackage.deviceId);
                    notifyDeviceReady(parameterPackage.value,parameterPackage.deviceId
                            ,parameterPackage.deviceId);
                    sendConnectedNotification();
                    getSupportedPids(); //Get supported pids once verified
                }

                @Override
                public void onDeviceBrokenAndCarHasScanner(String scannerId) {
                    LogUtils.debugLogD(TAG, "Device missing id but user car has a scanner" +
                                    ", overwriting scanner id to "+scannerId+", ignoreVerification: "
                                    +ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                    trackBluetoothEvent(MixpanelHelper.BT_DEVICE_BROKEN);

                    //ignore result if verification state changed mid use-case execution
                    if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
                        verificationInProgress = false;
                        return;
                    }
                    //Connected to device mid use-case execution, return
                    else if (ignoreVerification){
                        verificationInProgress = false;
                        if (deviceConnState.equals(State.VERIFYING)){
                            deviceConnState = State.SEARCHING;
                        }
                        return;
                    }

                    setDeviceNameAndId(scannerId);
                    deviceIdOverwriteInProgress = true;
                    deviceIsVerified = true;
                    verificationInProgress = false;
                    deviceConnState = BluetoothConnectionObservable.State.CONNECTED;
                    deviceManager.onConnectDeviceValid();
                    readyDevice = new ReadyDevice(parameterPackage.value,parameterPackage.deviceId
                            ,parameterPackage.deviceId);
                    notifyDeviceReady(parameterPackage.value,scannerId, scannerId);
                    sendConnectedNotification();
                    getSupportedPids(); //Get supported pids once verified
                }

                @Override
                public void onDeviceInvalid() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device is invalid." +
                                    " ignoreVerification?"+ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                    //ignore result if verification state changed mid use-case execution
                    if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
                        verificationInProgress = false;
                        return;
                    }
                    //Connected to device mid use-case execution, return
                    else if (ignoreVerification){
                        verificationInProgress = false;
                        if (deviceConnState.equals(State.VERIFYING)){
                            deviceConnState = State.SEARCHING;
                        }
                        return;
                    }

                    deviceIsVerified = false;
                    verificationInProgress = false;

                    deviceManager.onConnectedDeviceInvalid();

                    if (deviceManager.moreDevicesLeft()){
                        deviceConnState = State.SEARCHING;
                        notifySearchingForDevice();
                    }
                    else{
                        deviceConnState = BluetoothConnectionObservable.State.DISCONNECTED;
                        notifyDeviceDisconnected();
                    }

                }

                @Override
                public void onDeviceAlreadyActive() {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect Device is already active" +
                                    ", ignoreVerification?"+ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                    //ignore result if verification state changed mid use-case execution
                    if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
                        verificationInProgress = false;
                        return;
                    }
                    //Connected to device mid use-case execution, return
                    else if (ignoreVerification){
                        verificationInProgress = false;
                        if (deviceConnState.equals(State.VERIFYING)){
                            deviceConnState = State.SEARCHING;
                        }
                        return;
                    }

                    deviceIsVerified = false;
                    verificationInProgress = false;
                    deviceManager.onConnectedDeviceInvalid();

                    if (deviceManager.moreDevicesLeft()){
                        deviceConnState = State.SEARCHING;
                        notifySearchingForDevice();
                    }
                    else{
                        deviceConnState = BluetoothConnectionObservable.State.DISCONNECTED;
                        notifyDeviceDisconnected();
                    }


                }

                @Override
                public void onError(RequestError error) {
                    LogUtils.debugLogD(TAG, "handleVinOnConnect error occurred" +
                                    ", ignoreVerification?"+ignoreVerification
                            ,true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());

                    trackBluetoothEvent(MixpanelHelper.BT_VERIFICATION_ERROR);

                    //ignore result if verification state changed mid use-case execution
                    if (deviceConnState.equals(BluetoothConnectionObservable.State.CONNECTED)){
                        verificationInProgress = false;
                        return;
                    }
                    //Connected to device mid use-case execution, return
                    else if (ignoreVerification){
                        verificationInProgress = false;
                        if (deviceConnState.equals(State.VERIFYING)){
                            deviceConnState = State.SEARCHING;
                        }
                        return;
                    }

                    deviceIsVerified = false;
                    verificationInProgress = false;
                    deviceManager.onConnectedDeviceInvalid();

                    if (deviceManager.moreDevicesLeft()){
                        deviceConnState = State.SEARCHING;
                        notifySearchingForDevice();
                    }
                    else{
                        deviceConnState = BluetoothConnectionObservable.State.DISCONNECTED;
                        notifyDeviceDisconnected();
                    }

                }
            });

        }

        if(parameterPackage.paramType == ParameterPackage.ParamType.SUPPORTED_PIDS) {
            Log.d(TAG, "parameterData: " + parameterPackage.toString());
            Log.i(TAG, "Supported pids returned");
            String[] pids = parameterPackage.value.split(","); // pids returned separated by commas
            HashSet<String> supportedPidsSet = new HashSet<>(Arrays.asList(pids));
            StringBuilder sb = new StringBuilder();
            int pidCount = 0;
            // go through the priority list and get the first 10 pids that are supported
            for(String dataType : PID_PRIORITY) {
                if(pidCount >= 10) {
                    break;
                }
                if(supportedPidsSet.contains(dataType)) {
                    sb.append(dataType);
                    sb.append(",");
                    ++pidCount;
                }
            }
            if(sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') { // remove comma at end
                supportedPids = sb.substring(0, sb.length() - 1);
            } else {
                supportedPids = DEFAULT_PIDS;
            }
            deviceManager.setPidsToSend(supportedPids);
        }
    }

    private void sendConnectedNotification(){

        //show a custom notification
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_push);

        final Car connectedCar = carAdapter.getCarByScanner(getCurrentDeviceId());

        String carName = connectedCar == null ? "Click here to find out more" :
                connectedCar.getYear() + " " + connectedCar.getMake() + " " + connectedCar.getModel();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                        .setLargeIcon(icon)
                        .setColor(getResources().getColor(R.color.highlight))
                        .setContentTitle("Car is Connected")
                        .setContentText(carName);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra(MainActivity.FROM_NOTIF, true);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        stackBuilder.addParentStack(MainActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifID, mBuilder.build());
    }
}
