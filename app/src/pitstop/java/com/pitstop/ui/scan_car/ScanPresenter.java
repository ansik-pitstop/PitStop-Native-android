package com.pitstop.ui.scan_car;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.ReadyDevice;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.BasePresenter;
import com.pitstop.ui.BaseView;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.TimeoutTimer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class ScanPresenter implements ScanCarContract.Presenter {

    private static final String TAG = ScanPresenter.class.getSimpleName();
    public static final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_SCAN);

    private ScanCarContract.View mCallback;
    private NetworkHelper networkHelper;
    private Car dashboardCar;
    private UseCaseComponent useCaseComponent;
    private BluetoothConnectionObservable bluetoothObservable;

    private boolean isAskingForDtcs = false;
    private boolean realTimeDataRetrieved = true;

    private Set<String> retrievedDtcs;
    private Set<CarIssue> services;
    private Set<CarIssue> recalls;

    public ScanPresenter(BluetoothConnectionObservable observable
            , UseCaseComponent useCaseComponent, NetworkHelper networkHelper) {

        bluetoothObservable = observable;
        this.networkHelper = networkHelper;
        this.useCaseComponent = useCaseComponent;

    }

    private boolean isDeviceConnected(){
        if (bluetoothObservable == null) return false;
        return bluetoothObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED);
    }

    private boolean isDisconnected(){
        if (bluetoothObservable == null) return false;
        return bluetoothObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.DISCONNECTED);
    }

    @Override
    public void startScan() {
        if (mCallback == null || bluetoothObservable == null) return;

        if (isDeviceConnected()){
            mCallback.onScanStarted();
            bluetoothObservable.requestAllPid();
            getServicesAndRecalls();
            getEngineCodes();
        }
        else{
            bluetoothObservable.requestDeviceSearch(true, false);
            mCallback.onStartScanFailed(ERR_START_DC);
        }
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

                    //Check whether car change occurred
                    if (dashboardCar != null){
                        if (car.getId() != dashboardCar.getId()){

                            //show prompt if scanning and car change occurred
                            if (mCallback.isScanning()){
                                interruptScan(ERR_INTERRUPT_GEN);
                            }
                            else{
                                mCallback.resetUI();
                            }
                        }
                    }
                }

                dashboardCar = car;
            }

            @Override
            public void onNoCarSet() {

            }

            @Override
            public void onError(RequestError error) {

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
        if (!isDeviceConnected()) return;

        retrievedDtcs = new HashSet<>(); // clear previous result
        isAskingForDtcs = true;
        bluetoothObservable.requestDtcData();
        checkEngineIssuesTimer.cancel();
        checkEngineIssuesTimer.start();
    }

    @Override
    public void getServicesAndRecalls() {
        Log.d(TAG, "getServicesAndRecalls");

        if (dashboardCar == null) return;

        networkHelper.getCarsById(dashboardCar.getId(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (mCallback == null) return;

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
    public void interruptScan(String errorMessage) {
        if (mCallback == null) return;
        if (!mCallback.isScanning()) return;

        cancelAllTimers();
        mCallback.onScanInterrupted(errorMessage);
    }

    private void cancelAllTimers() {
        checkEngineIssuesTimer.cancel();
        checkRealTimeTimer.cancel();
    }
    /**
     * If after 20 seconds we are still unable to retrieve any DTCs, we consider it as there
     * is no DTCs currently.
     */
    private final TimeoutTimer checkEngineIssuesTimer = new TimeoutTimer(5, 4) {
        @Override
        public void onRetry() {
            if (retrievedDtcs.isEmpty() && bluetoothObservable != null){
                bluetoothObservable.requestDtcData();
            }
        }

        @Override
        public void onTimeout() {
            if (mCallback == null || !isAskingForDtcs ) return;

            isAskingForDtcs = false;
            mCallback.onEngineCodesRetrieved(retrievedDtcs);
        }

    };

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
        Log.d(TAG,"bind()");
        if (mCallback != null) return;

        mCallback = (ScanCarContract.View) view;
        if(bluetoothObservable != null){
            bluetoothObservable.subscribe(this);
        }
    }

    @Override
    public void unbind() {
        Log.d(TAG,"unbind()");

        if (mCallback == null) return;

        if (bluetoothObservable != null){
            bluetoothObservable.unsubscribe(this);
        }

        if (mCallback.isScanning()){
            interruptScan(ERR_INTERRUPT_GEN);
        }

        mCallback.hideLoading(null);
        mCallback = null;
    }

    @Override
    public void onSearchingForDevice() {

    }

    @Override
    public void onDeviceReady(ReadyDevice readyDevice) {

    }

    @Override
    public void onDeviceDisconnected() {
        if (mCallback.isScanning()){
            interruptScan(ERR_INTERRUPT_DC);
        }
    }

    @Override
    public void onDeviceVerifying() {

    }

    @Override
    public void onDeviceSyncing() {

    }

    @Override
    public void onGotDtc(DtcPackage dtcPackage) {
        Log.i(TAG, "DTC data received: " + dtcPackage.dtcNumber);

        if (!isAskingForDtcs) return;
        //Got DTC
        if (dtcPackage.dtcs != null && dtcPackage.dtcs.length > 0) {
            retrievedDtcs.addAll(Arrays.asList(dtcPackage.dtcs));
            mCallback.onEngineCodesRetrieved(retrievedDtcs);
            isAskingForDtcs = false;
            checkEngineIssuesTimer.cancel();
        }
    }

    @Override
    public void onHistoricalDataStateChanged(boolean historicalEnabled) {

    }
}
