package com.pitstop.bluetooth;

import android.content.Context;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.other.HandleVinOnConnectUseCase;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MixpanelHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public class ParameterDataHandler {

    private Context context;
    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private BluetoothMixpanelTracker bluetoothMixpanelTracker;
    private DeviceVerificationObserver deviceVerificationObserver;
    private UseCaseComponent useCaseComponent;

    private final LinkedList<String> PID_PRIORITY = new LinkedList<>();
    private String supportedPids = "";

    public ParameterDataHandler(Context context, BluetoothConnectionObservable bluetoothConnectionObservable
            , BluetoothMixpanelTracker bluetoothMixpanelTracker
            , DeviceVerificationObserver deviceVerificationObserver) {

        this.context = context;
        this.useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        this.bluetoothConnectionObservable = bluetoothConnectionObservable;
        this.bluetoothMixpanelTracker = bluetoothMixpanelTracker;
        this.deviceVerificationObserver = deviceVerificationObserver;
        initPidPriorityList();
    }

    public void handleParameterData(ParameterPackage parameterPackage, long terminalRTCTime
            , boolean ignoreVerification, boolean deviceIsVerified, boolean verificationInProgress){
        if (parameterPackage == null) return;

        //Change null to empty
        if (parameterPackage.value == null){
            parameterPackage.value = "";
        }

        final String TAG = getClass().getSimpleName() + ".parameterData()";

        if (parameterPackage.paramType == ParameterPackage.ParamType.VIN){
            bluetoothConnectionObservable.notifyVin(parameterPackage.value);
        }

        if(parameterPackage.paramType == ParameterPackage.ParamType.SUPPORTED_PIDS) {
            Log.d(TAG, "parameterData: " + parameterPackage.toString());
            Log.i(TAG, "Supported pids returned");
            String[] pids = parameterPackage.value.split(","); // pids returned separated by commas
            HashSet<String> supportedPidsSet = new HashSet<>(Arrays.asList(pids));
            StringBuilder sb = new StringBuilder();
            int pidCount = 0;
            // go through the priority list and get the first 10 pids that are supported
            for(String dataType : PID_PRIORITY) {
                if(pidCount >= 10) {
                    break;
                }
                if(supportedPidsSet.contains(dataType)) {
                    sb.append(dataType);
                    sb.append(",");
                    ++pidCount;
                }
            }
            if(sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') { // remove comma at end
                supportedPids = sb.substring(0, sb.length() - 1);
            } else {
                supportedPids = DEFAULT_PIDS;
            }
            deviceManager.setPidsToSend(supportedPids);
        }
    }

    // hardcoded linked list that is in the order of priority
    private void initPidPriorityList() {
        PID_PRIORITY.add("210C");
        PID_PRIORITY.add("210D");
        PID_PRIORITY.add("2106");
        PID_PRIORITY.add("2107");
        PID_PRIORITY.add("2110");
        PID_PRIORITY.add("2124");
        PID_PRIORITY.add("2105");
        PID_PRIORITY.add("210E");
        PID_PRIORITY.add("210F");
        PID_PRIORITY.add("2142");
        PID_PRIORITY.add("210A");
        PID_PRIORITY.add("210B");
        PID_PRIORITY.add("2104");
        PID_PRIORITY.add("2111");
        PID_PRIORITY.add("212C");
        PID_PRIORITY.add("212D");
        PID_PRIORITY.add("215C");
        PID_PRIORITY.add("2103");
        PID_PRIORITY.add("212E");
    }


}
