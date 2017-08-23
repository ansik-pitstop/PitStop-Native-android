package com.pitstop.ui.scan_car;

import android.util.Log;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
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
                .equals(BluetoothConnectionObservable.State.CONNECTED);
    }

    private boolean isDisconnected(){
        if (bluetoothObservable == null) return false;
        return bluetoothObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.DISCONNECTED);
    }

    @Override
    public void startScan() {
        Log.d(TAG,"startScan()");
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
    public void getEngineCodes() {
        if (!isDeviceConnected()) return;

        isAskingForDtcs = true;
        bluetoothObservable.requestDtcData();
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

        mCallback.onScanInterrupted(errorMessage);
    }

    @Override
    public void onGotAllPid(HashMap<String,String> pid){
        Log.d(TAG,"All pids received, pidPackage:"+pid);
        mCallback.onScanInterrupted("Error getting all pid from device");
    }

    @Override
    public void onErrorGettingAllPid() {
        Log.d(TAG,"onErrorGettingAllPid()");
    }

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
    public void onGotDtc(HashMap<Boolean,String> dtc) {
        Log.i(TAG, "DTC data received: " + dtc);

        if (!isAskingForDtcs) return;
        mCallback.onEngineCodesRetrieved(new HashSet<String>(dtc.values()));
    }

    @Override
    public void onErrorGettingDtc() {
        Log.d(TAG,"onErrorGettingDtc()");
        mCallback.onScanInterrupted("Error retrieving DTC from device");
    }

}
