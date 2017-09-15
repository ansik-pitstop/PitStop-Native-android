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

    private ScanCarContract.View view;
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
                .equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED);
    }

    private boolean isDisconnected(){
        if (bluetoothObservable == null) return false;
        return bluetoothObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.DISCONNECTED);
    }

    @Override
    public void startScan() {
        Log.d(TAG,"startScan()");
        if (view == null) return;

        if (bluetoothObservable == null){
            bluetoothObservable = view.getBluetoothObservable();
            if (bluetoothObservable == null) return;
            bluetoothObservable.subscribe(this);
        }

        if (isDeviceConnected()){
            view.onScanStarted();
            bluetoothObservable.requestAllPid();
            getServicesAndRecalls();
            getEngineCodes();
        }
        else{
            bluetoothObservable.requestDeviceSearch(true, false);
            view.onStartScanFailed(ERR_START_DC);
        }
    }

    @Override
    public void update() {
        if (view == null) return;

        view.showLoading("Loading...");
        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                if (view == null) return;
                view.hideLoading("Loading...");

                //Check whether car change occurred
                if (dashboardCar != null){
                    if (car.getId() != dashboardCar.getId()){

                        //show prompt if scanning and car change occurred
                        if (view.isScanning()){
                            interruptScan(ERR_INTERRUPT_GEN);
                        }
                        else{
                            view.resetUI();
                        }
                    }
                }
                dashboardCar = car;
            }


            @Override
            public void onNoCarSet() {
                if (view == null) return;
                view.hideLoading(null);
            }

            @Override
            public void onError(RequestError error) {
                if (view == null) return;
                view.hideLoading(null);
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
                if (view == null) return;

                if (requestError != null) {
                    Log.e(TAG, String.valueOf(requestError.getStatusCode()));
                    Log.e(TAG, requestError.getError());
                    Log.e(TAG, requestError.getMessage());
                    view.onNetworkError(requestError.getMessage());
                    view.onRecallRetrieved(null);
                    view.onServicesRetrieved(null);
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
                    view.onServicesRetrieved(services);
                    view.onRecallRetrieved(recalls);

                } catch (JSONException e) {
                    e.printStackTrace();
                    view.onNetworkError("Unknown network error happened, please retry later.");
                    view.onServicesRetrieved(null);
                    view.onRecallRetrieved(null);
                }
            }
        });
    }

    @Override
    public void interruptScan(String errorMessage) {
        if (view == null) return;
        if (!view.isScanning()) return;

        view.onScanInterrupted(errorMessage);
    }

    @Override
    public void onGotAllPid(HashMap<String,String> pid){
        Log.d(TAG,"All pids received, pidPackage:"+pid);
    }

    @Override
    public void onErrorGettingAllPid() {
        Log.d(TAG,"onErrorGettingAllPid()");
    }

    @Override
    public void bind(BaseView<? extends BasePresenter> view) {
        Log.d(TAG,"bind()");
        if (this.view != null) return;

        this.view = (ScanCarContract.View) view;
        if(bluetoothObservable != null){
            bluetoothObservable.subscribe(this);
        }
    }

    @Override
    public void unbind() {
        Log.d(TAG,"unbind()");

        if (view == null) return;

        if (bluetoothObservable != null){
            bluetoothObservable.unsubscribe(this);
        }

        if (view.isScanning()){
            interruptScan(ERR_INTERRUPT_GEN);
        }

        view.hideLoading(null);
        view = null;
    }

    @Override
    public void onSearchingForDevice() {

    }

    @Override
    public void onDeviceReady(ReadyDevice readyDevice) {

    }

    @Override
    public void onDeviceDisconnected() {
        if (view.isScanning()){
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
    public void onGotDtc(HashMap<String ,Boolean> dtc) {
        Log.i(TAG, "DTC data received: " + dtc);

        if (!isAskingForDtcs) return;
        view.onEngineCodesRetrieved(new HashSet<String>(dtc.keySet()));
    }

    @Override
    public void onErrorGettingDtc() {
        Log.d(TAG,"onErrorGettingDtc()");
        view.onScanInterrupted("Error retrieving DTC from device");
    }

}
