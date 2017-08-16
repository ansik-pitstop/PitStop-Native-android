package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Dtc;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;
import static io.fabric.sdk.android.Fabric.TAG;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public class DtcDataHandler implements BluetoothDataHandler{

    private static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_BLUETOOTH_AUTO_CONNECT);

    private ArrayList<Dtc> dtcsToSend = new ArrayList<>();
    private List<DtcPackage> pendingDtcPackages = new ArrayList<>();
    private List<DtcPackage> processedDtcPackages = new ArrayList<>();
    private LocalCarAdapter localCarStorage;
    private BluetoothDataHandlerManager bluetoothDataHandlerManager;
    private NetworkHelper networkHelper;
    private String deviceId = "";

    public DtcDataHandler(BluetoothDataHandlerManager bluetoothDataHandlerManager, Context context){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
        this.localCarStorage = new LocalCarAdapter(context);

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        this.networkHelper = tempNetworkComponent.networkHelper();

    }

    public void handleDtcData(DtcPackage dtcPackage){
        boolean deviceIdMissing = dtcPackage.deviceId == null
                || dtcPackage.deviceId.isEmpty();

        if (!bluetoothDataHandlerManager.isDeviceVerified() || deviceIdMissing){
            LogUtils.debugLogD(TAG, "Dtc data added to pending list, device not verified!"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            pendingDtcPackages.add(dtcPackage);
            return;
        }

        if (!pendingDtcPackages.contains(dtcPackage) && pendingDtcPackages.size() > 0){
            LogUtils.debugLogD(TAG, "Going through pending dtc packages, length: "
                            +pendingDtcPackages.size(), true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());

            for (DtcPackage p: pendingDtcPackages){
                processedDtcPackages.add(p);
                handleDtcData(p);
            }

            pendingDtcPackages.removeAll(processedDtcPackages);
            LogUtils.debugLogD(TAG, "Pending dtc packages list length after removal: "
                            +pendingDtcPackages.size(), true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());
        }

        if(dtcPackage.dtcNumber > 0) {
            saveDtcs(dtcPackage);
            bluetoothDataHandlerManager.requestFreezeData();
        }

    }

    private void notifyEventBus(EventType eventType){
        CarDataChangedEvent carDataChangedEvent
                = new CarDataChangedEvent(eventType,EVENT_SOURCE);
        EventBus.getDefault().post(carDataChangedEvent);
    }

    private void saveDtcs(final DtcPackage dtcPackage) {
        Car car = localCarStorage.getCarByScanner(dtcPackage.deviceId);

        if(networkHelper.isConnected()) {
            if (car != null) {
                networkHelper.getCarsById(car.getId(), new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            try {
                                Car car = Car.createCar(response);

                                HashSet<String> dtcNames = new HashSet<>();
                                for (CarIssue issue : car.getActiveIssues()) {
                                    dtcNames.add(issue.getItem());
                                }

                                List<String> dtcList = new ArrayList<String>();
                                for (String dtc: dtcPackage.dtcs){
                                    dtcList.add(dtc);
                                }
                                List<String> toRemove = new ArrayList<>();
                                for (String dtc: dtcList){
                                    if (dtcNames.contains(dtc)){
                                        toRemove.add(dtc);
                                    }
                                }
                                dtcList.removeAll(toRemove);

                                for (final String dtc: dtcList) {
                                    final int dtcListSize = dtcList.size();
                                    final List<String> dtcListReference = dtcList;

                                    networkHelper.addNewDtc(car.getId(), car.getTotalMileage(),
                                            dtcPackage.rtcTime, dtc, dtcPackage.isPending,
                                            new RequestCallback() {
                                                @Override
                                                public void done(String response, RequestError requestError) {
                                                    Log.i(TAG, "DTC added: " + dtc);

                                                    //INCLUDE THIS INSIDE USE CASE WHEN REFACTORING
                                                    //Notify that dtcs have been updated once
                                                    // the last one has been sent successfully
                                                    if (dtcListReference.indexOf(dtc)
                                                            == dtcListReference.size()-1){

                                                        notifyEventBus(new EventTypeImpl(
                                                                EventType.EVENT_DTC_NEW));
                                                    }
                                                }
                                            });
                                }
                                localCarStorage.updateCar(car);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        } else if(car != null) {
            Log.i(TAG, "Saving dtcs offline");
            for (final String dtc : dtcPackage.dtcs) {
                dtcsToSend.add(new Dtc(car.getId(), car.getTotalMileage(), dtcPackage.rtcTime,
                        dtc, dtcPackage.isPending));
            }
        }
    }

    public void sendLocalDtcs(){
        for (final Dtc dtc : dtcsToSend) {
            networkHelper.addNewDtc(dtc.getCarId(), dtc.getMileage(),
                    dtc.getRtcTime(), dtc.getDtcCode(), dtc.isPending(),
                    new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            Log.i(TAG, "DTC added: " + dtc);

                            //INCLUDE THIS IN USE CASE IN LATER REFACTOR
                            //Send notification that dtcs have been updated
                            // after last one has been sent
                            if (dtcsToSend.indexOf(dtc) == dtcsToSend.size()-1){
                                notifyEventBus(new EventTypeImpl(EventType
                                        .EVENT_DTC_NEW));
                            }
                        }
                    });
        }
    }

    public void clearPendingData(){
        pendingDtcPackages.clear();
    }

    @Override
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
