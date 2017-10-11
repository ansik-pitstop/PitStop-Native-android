package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.database.LocalCarStorage;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dtc;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public class DtcDataHandler{

    private final String TAG = getClass().getSimpleName();

    private static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_BLUETOOTH_AUTO_CONNECT);

    private ArrayList<Dtc> dtcsToSend = new ArrayList<>();
    private List<DtcPackage> pendingDtcPackages = new ArrayList<>();
    private BluetoothDataHandlerManager bluetoothDataHandlerManager;
    private UseCaseComponent useCaseComponent;

    public DtcDataHandler(BluetoothDataHandlerManager bluetoothDataHandlerManager
            , UseCaseComponent useCaseComponent){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
        this.useCaseComponent = useCaseComponent;
    }

    public void handleDtcData(DtcPackage dtcPackage){
        Log.d(TAG,"handleDtcData() dtcPackage: "+dtcPackage);
        String deviceId = dtcPackage.deviceId;

        pendingDtcPackages.add(dtcPackage);
        if (!bluetoothDataHandlerManager.isDeviceVerified() || deviceId.isEmpty()){
            Log.d(TAG, "Dtc data added to pending list, device not verified!");
            return;
        }

        for (DtcPackage p: pendingDtcPackages){

            if(dtcPackage.dtcs.size() > 0) {
                saveDtcs(p);
                bluetoothDataHandlerManager.requestFreezeData();
            }
        }
        pendingDtcPackages.clear();
    }

    private void notifyEventBus(EventType eventType){
        CarDataChangedEvent carDataChangedEvent
                = new CarDataChangedEvent(eventType,EVENT_SOURCE);
        EventBus.getDefault().post(carDataChangedEvent);
    }

    //TODO: Re-do method below.
    private void saveDtcs(final DtcPackage dtcPackage) {
        Car car = localCarStorage.getCarByScanner(dtcPackage.deviceId);
        Log.d(TAG,"saveDtcs() called, car retrieved from local storage: "+car);

        useCaseComponent.

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
                                    dtcPackage.dtcs.remove((issue.getItem()));
                                }

                                Log.d(TAG,"dtc list size after removing duplicates: "
                                        +dtcPackage.dtcs.keySet().size());
                                for (final String dtc: dtcPackage.dtcs.keySet()) {
                                    final List<String> dtcListReference
                                            = new ArrayList<>(dtcPackage.dtcs.keySet());

                                    networkHelper.addNewDtc(car.getId(), car.getTotalMileage(),
                                            dtcPackage.rtcTime, dtc, dtcPackage.dtcs.get(dtc),
                                            (response1, requestError1) -> {
                                                Log.i(TAG, "DTC added: " + dtc);
                                                if (requestError1 != null){
                                                    Log.d(TAG,"Error adding new dtc, error: "+ requestError1.getMessage());
                                                    return;
                                                }
                                                //INCLUDE THIS INSIDE USE CASE WHEN REFACTORING
                                                //Notify that dtcs have been updated once
                                                // the last one has been sent successfully
                                                if (dtcListReference.indexOf(dtc)
                                                        == dtcListReference.size()-1){

                                                    notifyEventBus(new EventTypeImpl(
                                                            EventType.EVENT_DTC_NEW));
                                                }
                                            });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        else{
                            Log.d(TAG,"Error saving dtcs, message: "+requestError.getMessage());
                        }
                    }
                });
            }
        } else if(car != null) {
            Log.i(TAG, "Saving dtcs offline");
            for (final Map.Entry<String,Boolean> dtc: dtcPackage.dtcs.entrySet()) {
                dtcsToSend.add(new Dtc(car.getId(), car.getTotalMileage(), dtcPackage.rtcTime,
                        dtc.getKey(), dtc.getValue()));
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

}
