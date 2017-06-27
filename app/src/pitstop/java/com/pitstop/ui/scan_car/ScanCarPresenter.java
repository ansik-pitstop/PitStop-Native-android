package com.pitstop.ui.scan_car;

import android.util.Log;

import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.BasePresenter;
import com.pitstop.ui.BaseView;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.TimeoutTimer;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class ScanCarPresenter implements ScanCarContract.Presenter {

    private static final String TAG = ScanCarPresenter.class.getSimpleName();

    public static final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_SCAN);

    private ScanCarContract.View mCallback;
    private MixpanelHelper mixpanelHelper;
    private NetworkHelper networkHelper;
    private LocalCarAdapter localCarAdapter;
    private Car dashboardCar;
    private BluetoothAutoConnectService mAutoConnectService;
    private UseCaseComponent useCaseComponent;

    public ScanCarPresenter(IBluetoothServiceActivity activity, UseCaseComponent useCaseComponent) {

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(activity))
                .build();
        networkHelper = tempNetworkComponent.networkHelper();
        this.useCaseComponent = useCaseComponent;
        localCarAdapter = new LocalCarAdapter(activity.getApplicationContext());
        mAutoConnectService = activity.autoConnectService;

    }

    @Override
    public double getLatestMileage() {
        if (dashboardCar == null) return 0;

        Car car = localCarAdapter.getCar(dashboardCar.getId());
        return car != null ? car.getDisplayedMileage() : 0;
    }

    @Override
    public void update() {
        if (mCallback == null) return;

        mCallback.showLoading("Loading...");
        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                if (mCallback != null){
                    mCallback.hideLoading("Loading...");
                    mCallback.onLoadedMileage(car.getTotalMileage());
                }
                dashboardCar = car;
            }

            @Override
            public void onNoCarSet() {

            }

            @Override
            public void onError() {

            }
        });
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
        if (dashboardCar == null) return;

        mAutoConnectService.manuallyUpdateMileage = true;
        mCallback.showLoading("Updating mileage...");
        networkHelper.updateCarMileage(dashboardCar.getId(), input, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    mCallback.onNetworkError(requestError.getMessage());
                    return;
                }

                mCallback.hideLoading("Mileage updated!");

                if (mAutoConnectService.getState() == BluetoothCommunicator.CONNECTED && mAutoConnectService.getLastTripId() != -1){
                    networkHelper.updateMileageStart(input, mAutoConnectService.getLastTripId(), null);
                }

                dashboardCar.setDisplayedMileage(input);
                dashboardCar.setTotalMileage(input);
                localCarAdapter.updateCar(dashboardCar);
                mCallback.onInputtedMileageUpdated(input);

                EventType type = new EventTypeImpl(EventType.EVENT_MILEAGE);
                EventBus.getDefault().post(new CarDataChangedEvent(type,EVENT_SOURCE));
            }
        });
    }

    @Override
    public void checkRealTime() {
        realTimeDataRetrieved = false;
        checkRealTimeTimer.cancel();
        checkRealTimeTimer.start();
    }

    @Override
    public void getEngineCodes() {
        retrievedDtcs = new HashSet<>(); // clear previous result
        isAskingForDtcs = true;
        mAutoConnectService.getDTCs();
        checkEngineIssuesTimer.cancel();
        checkEngineIssuesTimer.start();
    }

    private Set<CarIssue> services;
    private Set<CarIssue> recalls;

    @Override
    public void getServicesAndRecalls() {
        Log.d(TAG, "getServicesAndRecalls");

        if (dashboardCar == null) return;

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

                    Log.d(TAG, "Total issue size: " + issues.size());

                    for (CarIssue issue : issues) {
                        if (issue.getStatus().equals(CarIssue.ISSUE_DONE)) continue;
                        if (issue.getIssueType().contains(CarIssue.RECALL)) {
                            recalls.add(issue);
                        } else if (issue.getIssueType().contains(CarIssue.SERVICE)) {
                            services.add(issue);
                        }
                    }
                    mCallback.onServicesRetrieved(services);
                    mCallback.onRecallRetrieved(recalls);
                    localCarAdapter.updateCar(Car.createCar(response));

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
        mCallback.onRecallRetrieved(recalls);
        mCallback.onServicesRetrieved(services);
        mCallback.onEngineCodesRetrieved(retrievedDtcs);
    }

    @Override
    public void onActivityFinish() {
    }

    @Override
    public void bindBluetoothService() {

    }

    @Override
    public void onServiceBound(BluetoothAutoConnectService service) {
    }

    @Override
    public void unbindBluetoothService() {
    }

    @Override
    public void onServiceUnbind() {
    }

    @Override
    public void checkBluetoothService() {
    }

    @Override
    public void getBluetoothState(int state) {
        Log.i(TAG, "Bluetooth state updateCarIssue");
        switch (state) {
            case BluetoothCommunicator.CONNECTED:
                mCallback.onDeviceConnected();
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

        if (dashboardCar == null) return;

        if (tripInfoPackage.flag == TripInfoPackage.TripFlag.UPDATE) { // live mileage updateCarIssue
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
        if (!realTimeDataRetrieved && pidPackage.realTime){
            realTimeDataRetrieved = true;
            mCallback.onRealTimeDataRetrieved();
        }
    }

    private Set<String> retrievedDtcs;

    @Override
    public void dtcData(DtcPackage dtcPackage) {
        Log.i(TAG, "DTC data received: " + dtcPackage.dtcNumber);

        if (dtcPackage.dtcs != null && isAskingForDtcs) {
            retrievedDtcs.addAll(Arrays.asList(dtcPackage.dtcs));
        }
    }

    @Override
    public void ffData(FreezeFramePackage ffPackage) {

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
            if (isConnectedToDevice() || mCallback == null) return;
            mCallback.onConnectingTimeout();
        }
    };

    /**
     * If after 20 seconds we are still unable to retrieve any DTCs, we consider it as there
     * is no DTCs currently.
     */
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
        }
    };

    private boolean realTimeDataRetrieved = true;
    private final TimeoutTimer checkRealTimeTimer = new TimeoutTimer(30, 0) {
        @Override
        public void onRetry() {
            // do nothing
        }

        @Override
        public void onTimeout() {
            if (mCallback == null) return;
            if (realTimeDataRetrieved || !mCallback.isScanning()) return;
            mCallback.onGetRealTimeDataTimeout();
        }
    };


    @Override
    public void bind(BaseView<? extends BasePresenter> view) {
        mCallback = (ScanCarContract.View) view;
        mAutoConnectService.addCallback(this);
    }

    @Override
    public void unbind() {
        mCallback.hideLoading(null);
        mCallback = null;
        mAutoConnectService.removeCallback(this);
    }
}
