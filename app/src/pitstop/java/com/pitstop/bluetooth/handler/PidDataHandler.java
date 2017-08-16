package com.pitstop.bluetooth.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.castel.obd.info.DataPackageInfo;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalPidAdapter;
import com.pitstop.database.LocalPidResult4Adapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.models.Car;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Pid;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.facebook.FacebookSdk.getApplicationContext;
import static io.fabric.sdk.android.Fabric.TAG;

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

public class PidDataHandler implements BluetoothDataHandler{

    private static final int PID_CHUNK_SIZE = 15;
    private final String DEFAULT_PIDS = "2105,2106,210b,210c,210d,210e,210f,2110,2124,212d";
    private final LinkedList<String> PID_PRIORITY = new LinkedList<>();

    private String deviceId = null;
    private BluetoothDataHandlerManager bluetoothDataHandlerManager;
    private LocalPidAdapter localPidStorage;
    private LocalPidResult4Adapter localPidResult4;
    private LocalCarAdapter localCarStorage;
    private NetworkHelper networkHelper;
    private SharedPreferences sharedPreferences;
    private List<PidPackage> pendingPidPackages = new ArrayList<>();
    private List<PidPackage> processedPidPackages = new ArrayList<>();
    private File databasePath;

    private String supportedPids = "";
    private boolean isSendingPids = false;
    private int lastTripId = -1; // from backend
    private final String pfTripId = "lastTripId";
    private String currentDeviceId = "";

    public PidDataHandler(BluetoothDataHandlerManager bluetoothDataHandlerManager
            , Context context){

        this.bluetoothDataHandlerManager = bluetoothDataHandlerManager;
        this.localPidStorage = new LocalPidAdapter(context);
        this.localPidResult4 = new LocalPidResult4Adapter(context);
        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(context))
                .build();
        this.networkHelper = tempNetworkComponent.networkHelper();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.localCarStorage = new LocalCarAdapter(context);
        this.databasePath = context.getDatabasePath(LocalDatabaseHelper.DATABASE_NAME);
        this.lastTripId = sharedPreferences.getInt(pfTripId, -1);

        initPidPriorityList();
    }

    public void clearPendingData(){
        pendingPidPackages.clear();
    }

    public void handlePidData(PidPackage pidPackage){

        if(pidPackage.deviceId != null && !pidPackage.deviceId.isEmpty()) {
            deviceId = pidPackage.deviceId;
        }

        if (!bluetoothDataHandlerManager.isDeviceVerified()){
            LogUtils.debugLogD(TAG, "Pid data added to pending list, device not verified"
                    , true, DebugMessage.TYPE_BLUETOOTH
                    , getApplicationContext());
            pendingPidPackages.add(pidPackage);

            return;
        }

        for (PidPackage p: pendingPidPackages){
            if (deviceId != null){
                //pidPackage must have device id otherwise we would've returned
                p.deviceId = pidPackage.deviceId;
            }

            //Send pid data through to server
            Pid pidDataObject = getPidDataObject(pidPackage);

            if(pidDataObject.getMileage() >= 0 && pidDataObject.getCalculatedMileage() >= 0) {
                localPidStorage.createPIDData(pidDataObject);
            }

            if(localPidStorage.getPidDataEntryCount() >= PID_CHUNK_SIZE
                    && localPidStorage.getPidDataEntryCount() % PID_CHUNK_SIZE == 0) {
                sendPidDataToServer(pidPackage.rtcTime, pidPackage.deviceId, pidPackage.tripId);
            }

        }

        pendingPidPackages.removeAll(processedPidPackages);
    }

    private void sendPidDataToServer(final String rtcTime, final String deviceId, final String tripId) {
        if(isSendingPids) {
            Log.i(TAG, "Already sending pids");
            return;
        }
        isSendingPids = true;
        Log.i(TAG, "sending PID data");
        List<Pid> pidDataEntries = localPidStorage.getAllPidDataEntries();

        int chunks = pidDataEntries.size() / PID_CHUNK_SIZE + 1; // sending pids in size PID_CHUNK_SIZE chunks
        JSONArray[] pidArrays = new JSONArray[chunks];

        try {
            for(int chunkNumber = 0 ; chunkNumber < chunks ; chunkNumber++) {
                JSONArray pidArray = new JSONArray();
                for (int i = 0; i < PID_CHUNK_SIZE; i++) {
                    if (chunkNumber * PID_CHUNK_SIZE + i >= pidDataEntries.size()) {
                        continue;
                    }
                    Pid pidDataObject = pidDataEntries.get(chunkNumber * PID_CHUNK_SIZE + i);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("dataNum", pidDataObject.getDataNumber());
                    jsonObject.put("rtcTime", Long.parseLong(pidDataObject.getRtcTime()));
                    jsonObject.put("tripMileage", pidDataObject.getMileage());
                    jsonObject.put("tripIdRaw", pidDataObject.getTripId());
                    jsonObject.put("calculatedMileage", pidDataObject.getCalculatedMileage());
                    jsonObject.put("pids", new JSONArray(pidDataObject.getPids()));
                    pidArray.put(jsonObject);
                }
                pidArrays[chunkNumber] = pidArray;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(lastTripId != -1) {
            for(JSONArray pids : pidArrays) {
                if(pids.length() == 0) {
                    isSendingPids = false;
                    continue;
                }
                networkHelper.savePids(lastTripId, deviceId, pids,
                        new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                isSendingPids = false;
                                if (requestError == null) {
                                    Log.i(TAG, "PIDS saved");
                                    localPidStorage.deleteAllPidDataEntries();
                                } else {
                                    Log.e(TAG, "save pid error: " + requestError.getMessage());
                                    if (databasePath.length() > 10000000L) { // delete pids if db size > 10MB
                                        localPidStorage.deleteAllPidDataEntries();
                                        localPidResult4.deleteAllPidDataEntries();
                                    }
                                }
                            }
                        });
            }
        } else {
            isSendingPids = false;
        }
    }

    private Pid getPidDataObject(PidPackage pidPackage){

        Pid pidDataObject = new Pid();
        JSONArray pids = new JSONArray();

        Car car = localCarStorage.getCarByScanner(pidPackage.deviceId);

        double mileage;
        double calculatedMileage;

        if(pidPackage.tripMileage != null && !pidPackage.tripMileage.isEmpty()) {
            mileage = Double.parseDouble(pidPackage.tripMileage) / 1000;
            calculatedMileage = car == null ? 0 : mileage + car.getTotalMileage();
        }

        //FIX OR LOOK INTO
// } else if(lastData != null && lastData.tripMileage != null && !lastData.tripMileage.isEmpty()) {
//            mileage = Double.parseDouble(lastData.tripMileage)/1000;
//            calculatedMileage = car == null ? 0 : mileage + car.getTotalMileage();
//        }
         else {
            mileage = 0;
            calculatedMileage = 0;
         }

        pidDataObject.setMileage(mileage); // trip mileage from device
        pidDataObject.setCalculatedMileage(calculatedMileage);
        pidDataObject.setDataNumber("");  //FIX OR LOOK INTO
        pidDataObject.setTripId(Long.parseLong(pidPackage.tripId));
        pidDataObject.setRtcTime(pidPackage.rtcTime);
        pidDataObject.setTimeStamp(String.valueOf(System.currentTimeMillis() / 1000));

        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> pidEntry : pidPackage.pids.entrySet()) {
            sb.append(pidEntry.getKey());
            sb.append(": ");
            sb.append(pidEntry.getValue());
            sb.append(" / ");
            try {
                JSONObject pid = new JSONObject();
                pid.put("id", pidEntry.getKey());
                pid.put("data", pidEntry.getValue());
                pids.put(pid);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "PIDs received: " + sb.toString());

        pidDataObject.setPids(pids.toString());

        return pidDataObject;
    }

    /**
     * Send pid data on result 4 to server on 50 data points received
     *
     * @param data
     */
    public void sendPidDataResult4ToServer(DataPackageInfo data) { // TODO: Replace all usages with sendPidDataToServer
        if(isSendingPids) {
            Log.i(TAG, "Already sending pids");
            return;
        }
        isSendingPids = true;
        Log.i(TAG, "sending PID result 4 data");
        List<Pid> pidDataEntries = localPidResult4.getAllPidDataEntries();

        int chunks = pidDataEntries.size() / PID_CHUNK_SIZE + 1; // sending pids in size PID_CHUNK_SIZE chunks
        JSONArray[] pidArrays = new JSONArray[chunks];

        try {
            for (int chunkNumber = 0; chunkNumber < chunks; chunkNumber++) {
                JSONArray pidArray = new JSONArray();
                for (int i = 0; i < PID_CHUNK_SIZE; i++) {
                    if (chunkNumber * PID_CHUNK_SIZE + i >= pidDataEntries.size()) {
                        continue;
                    }
                    Pid pidDataObject = pidDataEntries.get(chunkNumber * PID_CHUNK_SIZE + i);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("dataNum", pidDataObject.getDataNumber());
                    jsonObject.put("rtcTime", Long.parseLong(pidDataObject.getRtcTime()));
                    jsonObject.put("tripMileage", pidDataObject.getMileage());
                    jsonObject.put("tripId", pidDataObject.getTripId());
                    jsonObject.put("calculatedMileage", pidDataObject.getCalculatedMileage());
                    jsonObject.put("pids", new JSONArray(pidDataObject.getPids()));
                    pidArray.put(jsonObject);
                }
                pidArrays[chunkNumber] = pidArray;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (lastTripId != -1) {
            for (JSONArray pids : pidArrays) {
                if (pids.length() == 0) {
                    isSendingPids = false;
                    continue;
                }
                networkHelper.savePids(lastTripId, deviceId, pids,
                        new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if (requestError == null) {
                                    Log.i(TAG, "PIDS result 4 saved");
                                    localPidResult4.deleteAllPidDataEntries();
                                } else {
                                    Log.e(TAG, "save pid result 4 error: " + requestError.getMessage());
                                    if (databasePath.length() > 10000000L) { // delete pids if db size > 10MB
                                        localPidResult4.deleteAllPidDataEntries();
                                    }
                                }
                                isSendingPids = false;
                            }
                        });
            }
        } else {
            isSendingPids = false;
            //tripRequestQueue.add(new TripStart(lastDeviceTripId, data.rtcTime, currentDeviceId));
            //executeTripRequests();
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

    @Override
    public void setDeviceId(String deviceId) {
        this.currentDeviceId = deviceId;
    }
}
