package com.pitstop.ui.scan_car;

import android.util.Log;

import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
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


public class ScanCarPresenter implements ScanCarContract.Presenter {

    private static final String TAG = ScanCarPresenter.class.getSimpleName();
    public static final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_SCAN);

    private ScanCarContract.View mCallback;
    private NetworkHelper networkHelper;
    private Car dashboardCar;
    private UseCaseComponent useCaseComponent;
    private BluetoothConnectionObservable bluetoothObservable;

    public ScanCarPresenter(BluetoothConnectionObservable observable
            , UseCaseComponent useCaseComponent, NetworkHelper networkHelper) {

        bluetoothObservable = observable;
        this.networkHelper = networkHelper;
        this.useCaseComponent = useCaseComponent;

    }

    private boolean isDeviceReady(){
        return bluetoothObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.CONNECTED);
    }

    @Override
    public void startScan() {
        if (isDeviceReady()){
            mCallback.onScanStarted();
            getServicesAndRecalls();
            getEngineCodes();
        }
        else{
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
            public void onError() {

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
        if (!isDeviceReady()) return;

        retrievedDtcs = new HashSet<>(); // clear previous result
        isAskingForDtcs = true;
        bluetoothObservable.requestDtcData();
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
        if (mCallback != null && !realTimeDataRetrieved && pidPackage.realTime){
            realTimeDataRetrieved = true;
            mCallback.onRealTimeDataRetrieved();
        }
    }

    private Set<String> retrievedDtcs;

    @Override
    public void dtcData(DtcPackage dtcPackage) {

    }

    @Override
    public void ffData(FreezeFramePackage ffPackage) {

    }

    private void cancelAllTimers() {
        checkEngineIssuesTimer.cancel();
        checkRealTimeTimer.cancel();
    }
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
            if (mCallback == null || !isAskingForDtcs ) return;
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
        Log.d(TAG,"bind()");
        if (mCallback != null) return;

        mCallback = (ScanCarContract.View) view;

        bluetoothObservable.subscribe(this);

    }

    @Override
    public void unbind() {
        Log.d(TAG,"unbind()");

        if (mCallback == null) return;

        bluetoothObservable.unsubscribe(this);
        
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
    public void onDeviceReady(String vin, String scannerId, String scannerName) {

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

        if (dtcPackage.dtcs != null && isAskingForDtcs) {
            retrievedDtcs.addAll(Arrays.asList(dtcPackage.dtcs));
        }
    }
}
