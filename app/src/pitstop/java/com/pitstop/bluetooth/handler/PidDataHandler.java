package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.util.Log;

import com.pitstop.BuildConfig;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCarByVinUseCase;
import com.pitstop.interactors.other.HandlePidDataUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.utils.BluetoothDataVisualizer;
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

    private final int PID_COUNT_DEFAULT = 10;
    private final int PID_COUNT_SAFE = 5;
    private final int TIME_INTERVAL_DEFAULT = 4;
    private final int TIME_INTERVAL_SAFE = 120;

    private final String TAG = getClass().getSimpleName();

    private final String DEFAULT_PIDS = "2105,2106,210b,210c,210d,210e,210f,2110,2124,212d";
    private final String DEFAULT_PIDS_SAFE = "2105,2106,210b,210c,210d";
    private final LinkedList<String> PID_PRIORITY = new LinkedList<>();

    private BluetoothDataHandlerManager bluetoothDataHandlerManager;
    private List<PidPackage> pendingPidPackages = new ArrayList<>();
    private UseCaseComponent useCaseComponent;
    private Context context;

    public PidDataHandler(BluetoothDataHandlerManager bluetoothDataHandlerManager
            , Context context){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        this.context = context;
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
                    BluetoothDataVisualizer.visualizePidDataSent(true,context);
                    Log.d(TAG,"Successfully handled pids.");
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"Error handling pids. Message: "+error.getMessage());
                    if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA) || BuildConfig.DEBUG){
                        BluetoothDataVisualizer.visualizePidDataSent(false,context);
                    }
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

    public void setDefaultPidCommunicationParameters(String vin){
        Log.d(TAG,"setDefaultPidCommunicationParameters() vin: "+vin);
        useCaseComponent.getGetCarByVinUseCase().execute(vin, new GetCarByVinUseCase.Callback() {
            @Override
            public void onGotCar(Car car) {
                if (car.getMake().equalsIgnoreCase(Car.Make.CHEVROLET)
                        || car.getMake().equalsIgnoreCase(Car.Make.DODGE)
                        || car.getMake().equalsIgnoreCase(Car.Make.CHRYSLER)
                        || car.getMake().equalsIgnoreCase(Car.Make.JEEP)){

                    bluetoothDataHandlerManager.setPidsToBeSent(DEFAULT_PIDS_SAFE,TIME_INTERVAL_SAFE);
                }
                else{
                    bluetoothDataHandlerManager.setPidsToBeSent(DEFAULT_PIDS,TIME_INTERVAL_DEFAULT);
                }
            }

            @Override
            public void onNoCarFound() {
                Log.d(TAG,"setPidCommunicationParameters() getCarByVinUseCase().onNoCarFound()");
                //Do nothing, car is probably being added and will handle supported pids again
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"setPidCommunicationParameters() getCarByVinUseCase().onError()");
            }
        });
    }

    public void setPidCommunicationParameters(String[] pids, String vin){
        Log.d(TAG,"setPidCommunicationParameters() pids: "+pids+", vin: "+vin);

        useCaseComponent.getGetCarByVinUseCase().execute(vin, new GetCarByVinUseCase.Callback() {
            @Override
            public void onGotCar(Car car) {
                if (car.getMake().equalsIgnoreCase(Car.Make.CHEVROLET)
                        || car.getMake().equalsIgnoreCase(Car.Make.DODGE)
                        || car.getMake().equalsIgnoreCase(Car.Make.CHRYSLER)
                        || car.getMake().equalsIgnoreCase(Car.Make.JEEP)){

                    String supportedPids = getSupportedPid(pids,PID_COUNT_SAFE);
                    Log.d(TAG,"setPidCommunicationParameters() Car make matches Chevrolet, Dodge" +
                            ", Chrystler or Jeep setting pid time interval to "+TIME_INTERVAL_SAFE
                            +", and supported pids to: "+supportedPids);
                    bluetoothDataHandlerManager.setPidsToBeSent(supportedPids,TIME_INTERVAL_SAFE);
                }
                else{
                    String supportedPids = getSupportedPid(pids,PID_COUNT_DEFAULT);
                    Log.d(TAG,"setPidCommunicationParameters() Car make doesn't match" +
                            " any of the 'safe cars' setting supported pids to "+supportedPids);
                    bluetoothDataHandlerManager.setPidsToBeSent(supportedPids,TIME_INTERVAL_DEFAULT);
                }
            }

            @Override
            public void onNoCarFound() {
                Log.d(TAG,"setPidCommunicationParameters() getCarByVinUseCase().onNoCarFound()");
                //Do nothing, car is probably being added and will handle supported pids again
            }

            @Override
            public void onError(RequestError error) {
                Log.d(TAG,"setPidCommunicationParameters() getCarByVinUseCase().onError()");
            }
        });
    }

    private String getSupportedPid(String[] pids, int max){
        HashSet<String> supportedPidsSet = new HashSet<>(Arrays.asList(pids));
        StringBuilder sb = new StringBuilder();
        int pidCount = 0;
        // go through the priority list and get the first 10 pids that are supported
        for(String dataType : PID_PRIORITY) {
            if(pidCount >= max) {
                break;
            }
            if(supportedPidsSet.contains(dataType)) {
                sb.append(dataType);
                sb.append(",");
                ++pidCount;
            }
        }
        if(sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') { // remove comma at end
            return sb.substring(0, sb.length() - 1);
        } else {
            return DEFAULT_PIDS;
        }
    }

}
