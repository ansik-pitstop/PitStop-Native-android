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
import com.pitstop.interactors.add.AddDtcUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dtc;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
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

        useCaseComponent.addDtcUseCase().execute(dtcPackage, new AddDtcUseCase.Callback() {
            @Override
            public void onDtcAdded() {
                notifyEventBus(new EventTypeImpl(
                        EventType.EVENT_DTC_NEW));
            }

            @Override
            public void onError(@NotNull RequestError requestError) {

            }
        });
    }

    public void clearPendingData(){
        pendingDtcPackages.clear();
    }

}
