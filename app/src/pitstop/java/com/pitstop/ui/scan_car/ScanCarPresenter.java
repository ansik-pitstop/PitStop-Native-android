package com.pitstop.ui.scan_car;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;

import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothServiceConnection;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.TimeoutTimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class ScanCarPresenter implements ScanCarContract.Presenter {

    private static final String TAG = ScanCarPresenter.class.getSimpleName();

    private ScanCarContract.View mCallback;
    private MixpanelHelper mixpanelHelper;
    private NetworkHelper networkHelper;
    private LocalCarAdapter localCarAdapter;
    private Car dashboardCar;

    private GlobalApplication application;
    private BluetoothAutoConnectService mAutoConnectService;
    private ServiceConnection mServiceConnection;

    private double baseMileage;

    public ScanCarPresenter(ScanCarContract.View viewCallback, GlobalApplication application, Car dashboardCar) {
        this.mCallback = viewCallback;
        this.dashboardCar = dashboardCar;
        this.application = application;
        baseMileage = dashboardCar.getTotalMileage();
        mixpanelHelper = new MixpanelHelper(application);
        networkHelper = new NetworkHelper(application);
        localCarAdapter = new LocalCarAdapter(application);
        mServiceConnection = new BluetoothServiceConnection(application, mCallback.getActivity(), this);
        bindBluetoothService();
    }

    @Override
    public void connectToDevice() {
        if (isConnectedToDevice()) {
            mCallback.onDeviceConnected();
        } else {
            mCallback.showLoading("Connecting to car");
            connectToCarWithTimeout();
        }
    }

    @Override
    public void updateMileage(final double input) {
        mAutoConnectService.manuallyUpdateMileage = true;
        networkHelper.updateCarMileage(dashboardCar.getId(), input, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    mCallback.onNetworkError(requestError.getMessage());
                    return;
                }

                if (mAutoConnectService.getState() == BluetoothCommunicator.CONNECTED && mAutoConnectService.getLastTripId() != -1){
                    networkHelper.updateMileageStart(input, mAutoConnectService.getLastTripId(), null);
                }

                dashboardCar.setDisplayedMileage(input);
                dashboardCar.setTotalMileage(input);
                localCarAdapter.updateCar(dashboardCar);
                mCallback.onInputtedMileageUpdated(input);
            }
        });
    }

    private Set<CarIssue> services;
    private Set<CarIssue> recalls;

    @Override
    public void getEngineCodes() {
        retrievedDtcs = new HashSet<>();
        isAskingForDtcs = true;
        mAutoConnectService.getDTCs();
        checkEngineIssuesTimer.cancel();
        checkEngineIssuesTimer.start();
    }

    @Override
    public void getServicesAndRecalls() {
        networkHelper.getCarsById(dashboardCar.getId(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    Log.e(TAG, String.valueOf(requestError.getStatusCode()));
                    Log.e(TAG, requestError.getError());
                    Log.e(TAG, requestError.getMessage());
                    mCallback.onNetworkError(requestError.getMessage());
                    mCallback.onRecallRetrieved(null);
                    mCallback.onServicesRetrieved(null);
                    return;
                }

                try {
                    Object issuesArr = new JSONObject(response).get("issues");
                    ArrayList<CarIssue> issues = new ArrayList<>();
                    if (issuesArr instanceof JSONArray) {
                        issues = CarIssue.createCarIssues((JSONArray) issuesArr, dashboardCar.getId());
                    }

                    services = new HashSet<>();
                    recalls = new HashSet<>();

                    for (CarIssue issue : issues) {
                        if (issue.getStatus().equals(CarIssue.ISSUE_DONE)) continue;
                        if (issue.getIssueType().equals(CarIssue.RECALL)) {
                            recalls.add(issue);
                        } else if (issue.getIssueType().equals(CarIssue.SERVICE)) {
                            services.add(issue);
                        }
                    }

                    checkProgress();

                } catch (JSONException e) {
                    e.printStackTrace();
                    mCallback.onNetworkError("Unknown network error happened, please retry later.");
                    mCallback.onServicesRetrieved(null);
                    mCallback.onRecallRetrieved(null);
                }
            }
        });
    }

    @Override
    public void finishScan() {
        if (!mCallback.isScanning()) return;
        cancelAllTimers();
    }

    @Override
    public void onActivityFinish() {
        unbindBluetoothService();
    }

    @Override
    public void bindBluetoothService() {
        mCallback.getActivity().bindService(
                new Intent(application, BluetoothAutoConnectService.class),
                mServiceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override
    public void onServiceBound(BluetoothAutoConnectService service) {
        mAutoConnectService = service;
        mAutoConnectService.setCallbacks(this);
    }

    @Override
    public void unbindBluetoothService() {
        mCallback.getActivity().unbindService(mServiceConnection);
    }

    @Override
    public void checkBluetoothService() {
        if (mAutoConnectService == null) {
            mAutoConnectService = mCallback.getAutoConnectService();
            mAutoConnectService.setCallbacks(this);
        }
    }

    @Override
    public void getBluetoothState(int state) {
        Log.i(TAG, "Bluetooth state update");
        switch (state) {
            case BluetoothCommunicator.CONNECTED:
                mCallback.onDeviceConnected();
                break;
            case BluetoothCommunicator.DISCONNECTED:
                mCallback.onDeviceDisconnected();
                break;
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }

    @Override
    public void tripData(TripInfoPackage tripInfoPackage) {
        if (tripInfoPackage.flag == TripInfoPackage.TripFlag.UPDATE) { // live mileage update
            final double newTotalMileage = ((int) ((dashboardCar.getTotalMileage() + tripInfoPackage.mileage) * 100)) / 100.0; // round to 2 decimal places

            Log.v(TAG, "Mileage updated: tripMileage: " + tripInfoPackage.mileage + ", baseMileage: " + dashboardCar.getTotalMileage() + ", newMileage: " + newTotalMileage);

            if (dashboardCar.getDisplayedMileage() < newTotalMileage) {
                dashboardCar.setDisplayedMileage(newTotalMileage);
                localCarAdapter.updateCar(dashboardCar);
            }
            mCallback.onTripMileageUpdated(newTotalMileage);
        } else if (tripInfoPackage.flag == TripInfoPackage.TripFlag.END) { // uploading historical data
            dashboardCar = localCarAdapter.getCar(dashboardCar.getId());
            final double newBaseMileage = dashboardCar.getTotalMileage();
            mCallback.onTripMileageUpdated(newBaseMileage);
        }
    }

    @Override
    public void parameterData(ParameterPackage parameterPackage) {

    }

    @Override
    public void pidData(PidPackage pidPackage) {

    }

    private Set<String> retrievedDtcs;

    @Override
    public void dtcData(DtcPackage dtcPackage) {
        Log.i(TAG, "DTC data received: " + dtcPackage.dtcNumber);
        if (dtcPackage.dtcs != null && isAskingForDtcs) {
            retrievedDtcs.addAll(Arrays.asList(dtcPackage.dtcs));
            mCallback.onEngineCodesRetrieved(retrievedDtcs);
        }
    }

    @Override
    public void ffData(FreezeFramePackage ffPackage) {

    }

    @Override
    public void start() {
        mixpanelHelper.trackTimeEventStart(MixpanelHelper.TIME_EVENT_SCAN_CAR);
    }

    private void checkProgress() {

    }

    private void cancelAllTimers() {
        connectCarTimer.cancel();
        checkEngineIssuesTimer.cancel();
        checkRealTimeTimer.cancel();
    }

    private boolean isConnectedToDevice() {
        return mAutoConnectService.getState() == BluetoothCommunicator.CONNECTED
                && mAutoConnectService.isCommunicatingWithDevice();
    }

    private void connectToCarWithTimeout() {
        mAutoConnectService.startBluetoothSearch();
        connectCarTimer.cancel();
        connectCarTimer.start();
    }

    private final TimeoutTimer connectCarTimer = new TimeoutTimer(15, 3) {
        @Override
        public void onRetry() {
            if (isConnectedToDevice()) {
                this.cancel();
                return;
            }
            mAutoConnectService.startBluetoothSearch();
        }

        @Override
        public void onTimeout() {
            if (isConnectedToDevice()) return;
            mCallback.onConnectingTimeout();
            // TODO: 16/12/7 Remember what to do if it connects before timeout
        }
    };

    private boolean isAskingForDtcs = false;
    private final TimeoutTimer checkEngineIssuesTimer = new TimeoutTimer(20, 0) {
        @Override
        public void onRetry() {
            // do nothing
        }

        @Override
        public void onTimeout() {
            if (!isAskingForDtcs) return;
            isAskingForDtcs = false;
            mCallback.onEngineCodesRetrieved(retrievedDtcs);
            checkProgress();
        }
    };

    private boolean realTimeDataRetrieved = false;
    private final TimeoutTimer checkRealTimeTimer = new TimeoutTimer(30, 0) {
        @Override
        public void onRetry() {
            // do nothing
        }

        @Override
        public void onTimeout() {
            if (realTimeDataRetrieved) return;
            mCallback.onGetRealtimeDataTimeout();
        }
    };


}
