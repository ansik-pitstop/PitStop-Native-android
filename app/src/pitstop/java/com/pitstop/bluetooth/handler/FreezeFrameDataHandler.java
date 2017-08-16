package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public class FreezeFrameDataHandler {

    private final String TAG = getClass().getSimpleName();

    private NetworkHelper networkHelper;
    private BluetoothConnectionObservable bluetoothConnectionObservable;

    private List<FreezeFramePackage> pendingFreezeFrames = new ArrayList<>();
    private List<FreezeFramePackage> processedFreezeFrames = new ArrayList<>();

    public FreezeFrameDataHandler(BluetoothConnectionObservable bluetoothConnectionObservable
            , Context context){

        this.networkHelper = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(context))
                .build()
                .networkHelper();
        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
    }

    public void handleFreezeFrameData(FreezeFramePackage freezeFramePackage){

        //Queue freeze frames until device is verified
        if (!bluetoothConnectionObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.CONNECTED)){
            LogUtils.debugLogD(TAG, "FreezeFrane added to pending list, device not verified!"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            pendingFreezeFrames.add(freezeFramePackage);
            return;
        }

        if (!pendingFreezeFrames.contains(freezeFramePackage) && pendingFreezeFrames.size() > 0){
            LogUtils.debugLogD(TAG, "Going through pending freeze frames"
                    , true, DebugMessage.TYPE_BLUETOOTH, getApplicationContext());
            for (FreezeFramePackage p: pendingFreezeFrames){

                //Set device id if it is missing
                if (p.deviceId == null || p.deviceId.isEmpty()){
                    //ffPackage must have device id otherwise we wouldve returned
                    p.deviceId = freezeFramePackage.deviceId;
                }

                processedFreezeFrames.add(p);
                handleFreezeFrameData(p);
            }
            pendingFreezeFrames.removeAll(processedFreezeFrames);
            LogUtils.debugLogD(TAG, "Pending freeze frames size() after removal: "
                            + pendingFreezeFrames.size(), true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());

        }

        saveFreezeFrame(freezeFramePackage);
    }

    private void saveFreezeFrame(FreezeFramePackage ffPackage){
        networkHelper.postFreezeFrame(ffPackage, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    Log.d("Save FF", requestError.getError());
                    Log.d("Save FF", requestError.getMessage());
                }
            }
        });
    }
}
