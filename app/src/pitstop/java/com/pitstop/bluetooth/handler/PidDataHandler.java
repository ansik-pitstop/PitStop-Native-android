package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.other.HandlePidDataUseCase;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.utils.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public class PidDataHandler {

    private final String TAG = getClass().getSimpleName();

    private static final int PID_CHUNK_SIZE = 15;
    private final String DEFAULT_PIDS = "2105,2106,210b,210c,210d,210e,210f,2110,2124,212d";
    private final LinkedList<String> PID_PRIORITY = new LinkedList<>();

    private BluetoothDataHandlerManager bluetoothDataHandlerManager;
    private List<PidPackage> pendingPidPackages = new ArrayList<>();
    private UseCaseComponent useCaseComponent;
    private String supportedPids = "";

    public PidDataHandler(BluetoothDataHandlerManager bluetoothDataHandlerManager
            , Context context){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();

        initPidPriorityList();
    }

    public void clearPendingData(){
        pendingPidPackages.clear();
    }

    public void handlePidData(PidPackage pidPackage){
        String deviceId = pidPackage.deviceId;
        Log.d(TAG,"handlePidData() deviceId:"+deviceId+", pidPackage: "+pidPackage);

        pendingPidPackages.add(pidPackage);
        if (!bluetoothDataHandlerManager.isDeviceVerified()){
            LogUtils.debugLogD(TAG, "Pid data added to pending list, device not verified"
                    , true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());
            return;
        }

        for (PidPackage p: pendingPidPackages){
            useCaseComponent.handlePidDataUseCase().execute(p, new HandlePidDataUseCase.Callback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG,"Successfully handled pids.");
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"Error handling pids. Message: "+error.getMessage());
                    if (error.getMessage().contains("not found")){
                        //Let trip handler know to get his shit together
                    }
                }
            });
        }
        pendingPidPackages.clear();
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

    public void handleSupportedPidResult(String[] pids){
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
        bluetoothDataHandlerManager.setPidsToBeSent(supportedPids);
    }

}
