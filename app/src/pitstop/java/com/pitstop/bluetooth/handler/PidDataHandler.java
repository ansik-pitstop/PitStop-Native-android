package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

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
import com.pitstop.utils.Logger;
import com.pitstop.utils.PIDParser;

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

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static boolean pidDataSentVisible = false;

    private int networkChunkSize = 10;

    private Handler handler = new Handler();
    private final Runnable periodicPidStatsLogger = new Runnable() {
        @Override
        public void run() {
            Logger.getInstance().logI(TAG,"Pid retrieval info for last 60 seconds: total="
                    +pidsReceived+", invalid="+nullPidsReceived+", sent="+pidsSavedToServer, true
                    , DebugMessage.TYPE_BLUETOOTH);
            handler.postDelayed(this,60000);
        }
    };

    //Tracking variables for logs
    private int pidsReceived = 0;
    private int nullPidsReceived = 0;
    private int pidsSavedToServer = 0;

    private final int PID_COUNT_DEFAULT = 10;
    private final int PID_COUNT_SAFE = 5;
    private final int TIME_INTERVAL_DEFAULT = 4;
    private final int TIME_INTERVAL_SAFE = 120;

    private final static  String TAG = PidDataHandler.class.getSimpleName();

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
        handler.post(periodicPidStatsLogger);
    }

    public void clearPendingData(){
        pendingPidPackages.clear();
    }

    public void handlePidData(PidPackage pidPackage){
        pidsReceived++;
        if (pidPackage == null){
            nullPidsReceived++;
            return;
        }

        String deviceId = pidPackage.deviceId;
        Log.d(TAG,"handlePidData() deviceId:"+deviceId+", pidPackage: "+pidPackage);
        // logging the pid based on receiving data from device
        if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA) || BuildConfig.DEBUG){
            Logger.getInstance().logD(TAG, "Received idr pid data: "+ PIDParser.pidPackageToDecimalValue(pidPackage)
                            + " real time?  " + pidPackage.realTime
                    , true, DebugMessage.TYPE_BLUETOOTH);
            visualizePidReceived(pidPackage,getApplicationContext());
        }

        pendingPidPackages.add(pidPackage);
        if (!bluetoothDataHandlerManager.isDeviceVerified()){
            Logger.getInstance().logD(TAG, "Pid data added to pending list, device not verified"
                    , true, DebugMessage.TYPE_BLUETOOTH);
            return;
        }
        for (PidPackage p: pendingPidPackages){
            useCaseComponent.handlePidDataUseCase().execute(p, new HandlePidDataUseCase.Callback() {
                @Override
                public void onDataSent(int size) {
                    if (BuildConfig.DEBUG  || BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA)) {
                        Log.d(TAG, p.timestamp + "Data sent to Server");
                        visualizePidDataSent(true, context, p.timestamp);
                        pidsSavedToServer += size;
                    }
                    else {
                        Log.d(TAG, "notRightBuild");
                    }
                    Log.d(TAG,"Successfully handled pids.");
                }

                @Override
                public void onError(RequestError error) {
                    Log.d(TAG,"Error handling pids. Message: "+error.getMessage());
                    if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA) || BuildConfig.DEBUG){
                        visualizePidDataSent(false,context, null);
                    }
                    if (error.getMessage().contains("not found")){
                        //Let trip handler know to get his shit together
                    }
                }

                @Override
                public void onDataStored() {
                    Log.d(TAG, p + " Stored in local database");

                }
            }, this.networkChunkSize);
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
                if (car.getMake().equalsIgnoreCase(Car.Make.RAM)
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
        // the interval being -1 lets the method know that this isnt a overwrite and to use default parameters for time interval
        setDevicePIDs(pids, vin, 1, false);
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


    public static void visualizePidReceived(PidPackage pidPackage, Context context){
        if (pidPackage == null){
            Log.d(TAG,"visualizePidReceived() pidPackage = null");
            Toast.makeText(context,"NULL pid values received",Toast.LENGTH_LONG).show();
        }else{
            Log.d(TAG,"visualizePidReceived() pidPackage.pids: "+pidPackage.pids.keySet());
            int rpm = Integer.parseInt(pidPackage.pids.get("210C"),16);
            Toast.makeText(context,"Pid values received, RPM: "+rpm,Toast.LENGTH_LONG).show();
        }
    }

    public static void visualizePidDataSent(boolean success, Context context, String timeStampFirst){
        Log.d(TAG,"visualizePidDataSent() success ? "+success);
        if (pidDataSentVisible) return;
        if (success&& timeStampFirst!= null) {
            Toast.makeText(context, "Pid values sent to server successfully", Toast.LENGTH_SHORT)
                    .show();
            Logger.getInstance().logD(TAG,"Pid values: " +timeStampFirst + " sent to server sucessfully"
                    ,true, DebugMessage.TYPE_NETWORK);
        }
        else {
            Toast.makeText(context, "Pid values failed to send to server: ", Toast.LENGTH_SHORT)
                    .show();
            Logger.getInstance().logD(TAG, "Pid values failed to send to server: "
                    , true, DebugMessage.TYPE_NETWORK);
        }
        pidDataSentVisible = true;
        //Only allow one toast showing failure every 15 seconds
        mainHandler.postDelayed(() -> pidDataSentVisible = false, 15000);
    }

    private void setDevicePIDs(String[] pids, String vin, int interval, boolean fromDrawer){

        // if device interval is less than 1, it means that it is being called by default add car process
        // if not then it is from debug drawer and doesnt use default parameters
        Log.d(TAG,"setDevicePIDs,  pids: "+pids+", vin: "+vin + ", Interval: " + interval);
        useCaseComponent.getGetCarByVinUseCase().execute(vin, new GetCarByVinUseCase.Callback() {
            @Override
            public void onGotCar(Car car) {
                if(fromDrawer){
                    String supportedPids = getSupportedPid(pids,PID_COUNT_SAFE);
                    int timeInterval = interval;
                    bluetoothDataHandlerManager.setPidsToBeSent(supportedPids,timeInterval);
                    Log.d(TAG,"setDeviceRTCInterval()setting pid time interval to "+timeInterval
                            +", and supported pids to: "+supportedPids);
                }
                else {
                    if (car.getMake().equalsIgnoreCase(Car.Make.RAM)
                            || car.getMake().equalsIgnoreCase(Car.Make.DODGE)
                            || car.getMake().equalsIgnoreCase(Car.Make.CHRYSLER)
                            || car.getMake().equalsIgnoreCase(Car.Make.JEEP)) {

                        String supportedPids = getSupportedPid(pids, PID_COUNT_SAFE);
                        bluetoothDataHandlerManager.setPidsToBeSent(supportedPids, TIME_INTERVAL_SAFE);
                        Logger.getInstance().logI(TAG," Setting pid time interval: interval=" + TIME_INTERVAL_SAFE
                                        + ", supportedPid=" + supportedPids, true,DebugMessage.TYPE_BLUETOOTH);
                    } else {
                        String supportedPids = getSupportedPid(pids, PID_COUNT_DEFAULT);
                        bluetoothDataHandlerManager.setPidsToBeSent(supportedPids, TIME_INTERVAL_DEFAULT);
                        Logger.getInstance().logI(TAG, "Setting pid time interval: interval=" + TIME_INTERVAL_DEFAULT +
                                ", supportedPid-" + supportedPids, true,DebugMessage.TYPE_BLUETOOTH);
                    }
                }
            }
            @Override
            public void onNoCarFound() {
                Logger.getInstance().logE(TAG,"Setting pid time interval: Error could not retrieve car(not set)"
                        ,true,DebugMessage.TYPE_BLUETOOTH);
                //Do nothing, car is probably being added and will handle supported pids again
            }
            @Override
            public void onError(RequestError error) {
                Logger.getInstance().logE(TAG,"Setting pid time interval: Error could not retrieve car(error returned by use case)"
                        ,true,DebugMessage.TYPE_BLUETOOTH);
            }
        });

    }

    public void setDeviceRtcInterval(String[] pids, String vin, int interval){
       setDevicePIDs(pids, vin, interval, true);
    }

    public void setChunkSize(int size) {
        this.networkChunkSize = size;
    }
}
