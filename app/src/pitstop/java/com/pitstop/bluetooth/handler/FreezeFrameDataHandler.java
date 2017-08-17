package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public class FreezeFrameDataHandler {

    private final String TAG = getClass().getSimpleName();

    private NetworkHelper networkHelper;
    private BluetoothConnectionObservable bluetoothConnectionObservable;

    private List<FreezeFramePackage> pendingFreezeFrames = new ArrayList<>();

    public FreezeFrameDataHandler(BluetoothConnectionObservable bluetoothConnectionObservable
            , Context context){

        this.networkHelper = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(context))
                .build()
                .networkHelper();
        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
    }

    public void handleFreezeFrameData(FreezeFramePackage freezeFramePackage){

        Log.d(TAG,"handleFreezeFrameData() ff: "+freezeFramePackage);

        //Queue freeze frames until device is verified
        pendingFreezeFrames.add(freezeFramePackage);
        if (!bluetoothConnectionObservable.getDeviceState()
                .equals(BluetoothConnectionObservable.State.CONNECTED)){
            return;
        }

        for (FreezeFramePackage p: pendingFreezeFrames){
            saveFreezeFrame(p);

        }
        pendingFreezeFrames.clear();
    }

    private void saveFreezeFrame(FreezeFramePackage ffPackage){
        Log.d(TAG,"saveFreezeFrame()");
        networkHelper.postFreezeFrame(ffPackage, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    Log.d(TAG,"Freeze frame save error!"+ requestError.getMessage());
                }else{
                    Log.d(TAG,"Freeze frame saved successfully!");
                }
            }
        });
    }
}
