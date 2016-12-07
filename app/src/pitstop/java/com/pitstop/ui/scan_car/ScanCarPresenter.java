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
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.TimeoutTimer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class ScanCarPresenter implements ScanCarContract.Presenter {

    private static final String TAG = ScanCarPresenter.class.getSimpleName();

    private ScanCarContract.View viewCallback;
    private MixpanelHelper mixpanelHelper;
    private NetworkHelper networkHelper;
    private LocalCarAdapter localCarAdapter;
    private Car dashboardCar;

    private GlobalApplication application;
    private BluetoothAutoConnectService mAutoConnectService;
    private ServiceConnection mServiceConnection;

    public ScanCarPresenter(ScanCarContract.View viewCallback, GlobalApplication application, Car dashboardCar) {
        this.viewCallback   = viewCallback;
        this.dashboardCar   = dashboardCar;
        this.application    = application;
        mixpanelHelper      = new MixpanelHelper(application);
        networkHelper       = new NetworkHelper(application);
        localCarAdapter     = new LocalCarAdapter(application);
        mServiceConnection = new BluetoothServiceConnection(application, viewCallback.getActivity());
        bindBluetoothService();
    }

    @Override
    public void connectToDevice() {

    }

    @Override
    public void updateMileage(String input) {

    }

    @Override
    public void getEngineCodes() {

    }

    @Override
    public void getIssues() {

    }

    @Override
    public void finish() {

    }

    @Override
    public void bindBluetoothService() {
        viewCallback.getActivity().bindService(
                new Intent(application, BluetoothAutoConnectService.class),
                mServiceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override
    public void unbindBluetoothService() {
        viewCallback.getActivity().unbindService(mServiceConnection);
    }

    @Override
    public void getBluetoothState(int state) {

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

    }

    @Override
    public void parameterData(ParameterPackage parameterPackage) {

    }

    @Override
    public void pidData(PidPackage pidPackage) {

    }

    private Set<String> dtcCodes = new HashSet<>();

    @Override
    public void dtcData(DtcPackage dtcPackage) {
        Log.i(TAG, "DTC data received: " + dtcPackage.dtcNumber);
        if(dtcPackage.dtcs != null && isAskingForDtcs) {
            dtcCodes.addAll(Arrays.asList(dtcPackage.dtcs));
        }
    }

    @Override
    public void ffData(FreezeFramePackage ffPackage) {

    }

    @Override
    public void start() {

    }

    private final TimeoutTimer connectCarTimer = new TimeoutTimer(15, 3) {
        @Override
        public void onRetry() {
            if (isConnected()) {
                this.cancel();
                return;
            }
            mAutoConnectService.startBluetoothSearch();
        }

        @Override
        public void onTimeout() {
            if (isConnected()) return;
            viewCallback.onConnectingTimeout();
            // TODO: 16/12/7 Remember what to do if it connects before timeout
        }

        private boolean isConnected(){
            return mAutoConnectService.getState() == BluetoothCommunicator.CONNECTED
                    && mAutoConnectService.isCommunicatingWithDevice();
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
            viewCallback.onEngineCodesRetrieved(dtcCodes);
        }
    };

    private boolean realTimeDataRetrieved = false;
    private final TimeoutTimer checkRealtimeTimer = new TimeoutTimer(30, 0) {
        @Override
        public void onRetry() {
            // do nothing
        }

        @Override
        public void onTimeout() {

        }
    };


}
