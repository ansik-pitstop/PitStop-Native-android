package com.pitstop.ui.addCarFragments;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.ObdDataUtil;
import com.castel.obd.util.Utils;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.AddCarActivity;
import com.pitstop.ui.MainActivity;
import com.pitstop.ui.PendingAddCarActivity;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.ui.mainFragments.MainDashboardFragment;
import com.pitstop.bluetooth.BluetoothServiceConnection;
import com.pitstop.utils.LoadingActivityInterface;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by david on 7/21/2016.
 */
public class AddCarUtils implements ObdManager.IBluetoothDataListener {

    private static final String TAG = AddCarUtils.class.getSimpleName();

    private static final int HANDLER_MSG_SEARCH_CAR = 0;
    private static final int HANDLER_MSG_GET_VIN = 1;
    private static final int HANDLER_MSG_GET_DTC_TIMEOUT = 2;

    AddCarActivity callback;
    GlobalApplication context;

    Car pendingCar;

    private Car createdCar;

    public CallMashapeAsync mashapeAsync;
    private NetworkHelper networkHelper;
    private MixpanelHelper mixpanelHelper;
    private BluetoothAutoConnectService autoConnectService;
    private boolean needToSetTime = false;
    public static final int RC_PENDING_ADD_CAR = 1043;
    private int vinAttempts = 0;
    private int linkingAttempts = 0;

    long vinRetrievalStartTime = 0;
    private long searchTime = 0;
    private int connectionAttempts = 0;
    private boolean askForDTC = false;
    private boolean gotValidRTC = false;
    private boolean isSearchingForCar = false;
    private boolean isGettingVinAndCarIsConnected = false;

    public static boolean gotMileage = false;

    private final CountDownTimer setRTCtimer = new CountDownTimer(40000, 40000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            if (needToSetTime) {
                mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SET_RTC, "Failed");
            }
            this.cancel();
        }
    };

    private GetDTCTimeoutRunnable mGetDTCTimeoutRunnable;

    /**
     * Callbacks for service binding, passed to bindService()
     */
    private ServiceConnection serviceConnection;

    private String dtcs;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "Add car handler message received: " + msg.what);
            switch (msg.what) {
                case HANDLER_MSG_SEARCH_CAR:
                    if (connectionAttempts++ == 3) {
                        isSearchingForCar = false;
                        connectionAttempts = 0;
                        callback.openRetryDialog();

                        // Search for bluetooth device/car over 3 times, ask the user to retry
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH,
                                MixpanelHelper.ADD_CAR_STEP_RESULT_FAILED);

                    } else {
                        Log.i(TAG, "connection reattempt: " + connectionAttempts);
                        searchAndGetVin();
                    }
                    break;

                case HANDLER_MSG_GET_VIN:
                    if (autoConnectService.getState() == IBluetoothCommunicator.DISCONNECTED) {
                        autoConnectService.startBluetoothSearch(1);  // when getting vin and disconnected
                    } else {
                        vinAttempts++;
                        autoConnectService.getCarVIN();
                        vinRetrievalStartTime = System.currentTimeMillis();
                    }
                    break;

                case HANDLER_MSG_GET_DTC_TIMEOUT:
                    cancelAllRunnables();
                    mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_DTCS_TIMEOUT,
                            MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);
                    // If getting DTCs timeout, for the sake of keeping good UX, we skip it
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                            .putInt(MainDashboardFragment.pfCurrentCar, createdCar.getId()).commit();
                    networkHelper.setMainCar(context.getCurrentUserId(), createdCar.getId(), null);
                    callback.carSuccessfullyAdded(createdCar);
                    break;
            }
        }
    };

    public AddCarUtils(GlobalApplication context, AddCarActivity callback) {
        this.context = context;
        this.callback = callback;
        pendingCar = new Car();
        serviceConnection = new BluetoothServiceConnection(context, callback);
        mashapeAsync = new CallMashapeAsync();
        mixpanelHelper = new MixpanelHelper(context);
        networkHelper = new NetworkHelper(context);
        autoConnectService = callback.getAutoConnectService();
        callback.bindService(new Intent(context, BluetoothAutoConnectService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
        gotMileage = false;
    }

    public void cancelMashape() {
        if (mashapeAsync != null && mashapeAsync.getStatus().equals(AsyncTask.Status.RUNNING)) {
            mashapeAsync.cancel(true);
            mashapeAsync = new CallMashapeAsync();
        }
        isSearchingForCar = false;
        mHandler.removeCallbacks(carSearchRunnable);
        mHandler.removeCallbacks(vinDetectionRunnable);
    }

    public void unbindService() {
        callback.unbindService(serviceConnection);
        pendingCar = null;
    }

    public void setVin(String vin) {
        pendingCar.setVin(vin);
    }

    public static boolean isValidVin(String vin) {
        Log.i(TAG, "isValidVin()-- func");
        return vin != null && vin.length() == 17;
    }

    /**
     * Invoked when the user enters and confirms the mileage in the dialog which pops out in the step 2<br>
     * (When "Add Car" button is pressed in the dialog)<br>
     * @param mileage Entered mileage
     */
    public void updateMileage(String mileage) {
        // The user confirms adding this vehicle after entering the mileage
        try {
            JSONObject properties = new JSONObject();
            properties.put("Button", MixpanelHelper.ADD_CAR_CONFIRM_ADD_VEHICLE);
            properties.put("View", MixpanelHelper.ADD_CAR_VIEW);
            properties.put("Mileage", Integer.parseInt(mileage));
            properties.put("Method of Adding Car", AddCarActivity.addingCarWithDevice ?
                    MixpanelHelper.ADD_CAR_METHOD_DEVICE : MixpanelHelper.ADD_CAR_METHOD_MANUAL);
            mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mileage != null) {
            pendingCar.setBaseMileage(Integer.parseInt(mileage));
            gotMileage = true;
        }
        callback.postMileageInput();
    }

    /**
     * Invoked when the user cancels his mileage input in the dialog which pops out in the step 2<br>
     * (When "Cancel" button is pressed in the dialog)<br>
     */
    public void cancelUpdateMileage() {
        // The user cancels adding this vehicle (cancel mileage input)
        try {
            JSONObject properties = new JSONObject();
            properties.put("Button", MixpanelHelper.ADD_CAR_CANCEL_ADD_VEHICLE);
            properties.put("View", MixpanelHelper.ADD_CAR_VIEW);
            properties.put("Method of Adding Car", AddCarActivity.addingCarWithDevice ? MixpanelHelper.ADD_CAR_METHOD_DEVICE : MixpanelHelper.ADD_CAR_METHOD_MANUAL);
            mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * At step 2, after the user input the mileage,
     * We start searching for OBD devices. <br>
     * And when the OBD device is connected, we ask for VIN.
     */
    public void searchAndGetVin() {

        if (autoConnectService == null) {
            autoConnectService = callback.getAutoConnectService();
            autoConnectService.setCallbacks(this);
        }

        Log.i(TAG, "Add car to server, bluetooth state: " + autoConnectService.getState());

        callback.showLoading("Adding car");
        if (isValidVin(pendingCar.getVin())) { // valid VIN is already entered
            Log.i(TAG, "Vin is valid -- (searching for car)");
            makeCar();
        } else {
            if (ContextCompat.checkSelfPermission(context, MainActivity.LOC_PERMS[0]) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, MainActivity.LOC_PERMS[1]) != PackageManager.PERMISSION_GRANTED) {
                callback.hideLoading("Location permissions are required");
            } else if (BluetoothAdapter.getDefaultAdapter() == null) { // Device doesn't support bluetooth
                callback.hideLoading("Your device does not support bluetooth");
            } else {
                if (autoConnectService.getState() == IBluetoothCommunicator.CONNECTED) { // Already connected to module
                    callback.showLoading("Linking with Device, give it a few seconds");
                    Log.i(TAG, "Getting car vin with device connected");

                    // Linked with the peripheral, getting car vin

                    autoConnectService.getCarVIN();
                    vinRetrievalStartTime = System.currentTimeMillis();
                    isGettingVinAndCarIsConnected = true;
                    mHandler.postDelayed(vinDetectionRunnable, 3000);

                } else { // Need to search for module
                    callback.showLoading("Searching for Car");

                    Log.i(TAG, "Searching for car but device not connected");
                    autoConnectService.startBluetoothSearch(2);  // search for car
                    isSearchingForCar = true;
                    gotValidRTC = false;
                    searchTime = System.currentTimeMillis();
                    mHandler.postDelayed(carSearchRunnable, 3000);
                }
            }
        }
    }


    /**
     * Search for OBD device, if found then starts try to get VIN
     */
    public void searchForUnrecognizedDevice() {
        if (autoConnectService == null) {
            autoConnectService = callback.getAutoConnectService();
            autoConnectService.setCallbacks(this);
        }

        Log.i(TAG, "Search for unrecognized device: " + autoConnectService.getState());

        callback.showLoading("Searching for scanner, please make sure your car engine is on and OBD device is plugged in.");
        if (ContextCompat.checkSelfPermission(context, MainActivity.LOC_PERMS[0]) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, MainActivity.LOC_PERMS[1]) != PackageManager.PERMISSION_GRANTED) {
            callback.hideLoading("Location permissions are required");
        } else if (BluetoothAdapter.getDefaultAdapter() == null) { // Device doesn't support bluetooth
            callback.hideLoading("Your device does not support bluetooth");
        } else {
            if (autoConnectService.getState() == IBluetoothCommunicator.CONNECTED) { // Already connected to module
                Log.i(TAG, "OBD device is connected, reading VIN");
                callback.showLoading("We have found the device, verifying...");
                // Linked with the peripheral, getting car vin
                autoConnectService.getCarVIN();
                vinRetrievalStartTime = System.currentTimeMillis();
                isGettingVinAndCarIsConnected = true;
                mHandler.postDelayed(vinDetectionRunnable, 3000);
            } else { // Need to search for module
                Log.i(TAG, "Searching for car but device not connected");
                callback.showLoading("Searching for scanner, please make sure your car engine is on and OBD device is plugged in.");
                autoConnectService.startBluetoothSearch(2);  // search for car
                isSearchingForCar = true;
                gotValidRTC = false;
                searchTime = System.currentTimeMillis();
                mHandler.postDelayed(carSearchRunnable, 3000);
            }
        }
    }


    /**
     * Create a new Car
     */
    private void makeCar() {

        Log.i(TAG, "makeCar() -- function");

        Log.i(TAG, "Making car -- make car function");
        if (autoConnectService == null) {
            autoConnectService = callback.getAutoConnectService();
            autoConnectService.setCallbacks(this);
        }

        Log.i(TAG, "Make car, bluetooth state: " + autoConnectService.getState());

        Log.i(TAG, "Checking internet connection");
        if (NetworkHelper.isConnected(context)) {
            Log.i(TAG, "Internet connection found");

            Log.i(TAG, "Adding car --- make car func");

            runVinTask();
        } else {
            Log.i(TAG, "No Internet");
            callback.hideLoading("No Internet");
            if (!AddCarActivity.isPairingUnrecognizedDevice) {
                startPendingAddCarActivity();
            }
        }
    }

    /**
     * Prepare the intent and start PendingAddCarActivity;<br>
     * Extras that will be sent includes
     * <ul>
     * <li>pendingCar.getBaseMileage()</li>
     * <li>dtcs</li>
     * <li>pendingCar.getScannerId()</li>
     * <li>pendingCar.getVin()</li>
     * </ul>
     */
    private void startPendingAddCarActivity() {
        Intent intent = new Intent(context, PendingAddCarActivity.class);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_MILEAGE, pendingCar.getBaseMileage());
        intent.putExtra(PendingAddCarActivity.ADD_CAR_DTCS, dtcs);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_SCANNER, pendingCar.getScannerId());
        intent.putExtra(PendingAddCarActivity.ADD_CAR_VIN, pendingCar.getVin());
        callback.startActivityForResult(intent, RC_PENDING_ADD_CAR);
    }

    @Override
    public void getBluetoothState(int state) {
        Log.i(TAG, "getBluetoothState func--");
        if (state == IBluetoothCommunicator.BLUETOOTH_CONNECT_SUCCESS) {
            // Successfully connected to bluetooth
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

            if (isSearchingForCar) {
                isSearchingForCar = false;
                callback.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.showLoading("Linking with Device, give it a few seconds");
                    }
                });
                Log.i(TAG, "Getting car vin --- getBluetoothState");
                autoConnectService.getCarVIN();
                vinRetrievalStartTime = System.currentTimeMillis();
                isGettingVinAndCarIsConnected = true;
                gotValidRTC = false;
                mHandler.postDelayed(vinDetectionRunnable, 3000);
            }
        } else if (state == IBluetoothCommunicator.CONNECTING && isSearchingForCar) {
            callback.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callback.showLoading("Connecting to device");
                }
            });
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
    }

    /**
     * After resetting device time, the bluetooth connection is lost.
     */
    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {
        Log.i(TAG, "Set parameter");

        if ((responsePackageInfo.type + responsePackageInfo.value)
                .equals(ObdManager.RTC_TAG)) {
            // Once device time is reset, the obd device disconnects from mobile device
            linkingAttempts = 0;
            needToSetTime = false;
            setRTCtimer.cancel();
            Log.i(TAG, "Set parameter() device time is set-- starting bluetooth search");
            callback.showLoading("Device successfully linked");
            mHandler.postDelayed(vinDetectionRunnable, 2000);
        }
    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {

        Log.i(TAG, "Get parameter data, bluetooth state: " + autoConnectService.getState());
        Log.i(TAG, "getParameterData()");

        if (parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.RTC_TAG)) {

            // If RTC is off by more than a year, a lot of stuff get fucked up
            // So if that is the case, we reset the RTC using current time
            // In the meantime, since such task takes time (We are communicating with the Device), we only do it if it's off by more than a year

            Log.i(TAG, "Device time returned: " + parameterPackageInfo.value.get(0).value);
            long moreThanOneYear = 32000000;
            long deviceTime = Long.valueOf(parameterPackageInfo.value.get(0).value);
            long currentTime = System.currentTimeMillis() / 1000;
            long diff = currentTime - deviceTime;

            // Now we got RTC from the device
            mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_RTC, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

            try {
                JSONObject properties = new JSONObject();
                properties.put(MixpanelHelper.ADD_CAR_STEP, MixpanelHelper.ADD_CAR_STEP_GET_RTC)
                        .put("View", MixpanelHelper.ADD_CAR_VIEW)
                        .put("RTC Time", String.valueOf(deviceTime))
                        .put(MixpanelHelper.ADD_CAR_STEP_RESULT, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);
                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_ADD_CAR_PROCESS, properties);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (diff > moreThanOneYear) {
                // Set RTC time
                Log.i(TAG, "Device RTC time is off by more than one year");
                autoConnectService.syncObdDevice();
                needToSetTime = true;
                setRTCtimer.start();
                try {
                    JSONObject properties = new JSONObject();
                    properties.put(MixpanelHelper.ADD_CAR_STEP, MixpanelHelper.ADD_CAR_STEP_SET_RTC)
                            .put("RTC Time", String.valueOf(deviceTime))
                            .put(MixpanelHelper.ADD_CAR_STEP_RESULT, MixpanelHelper.ADD_CAR_STEP_RESULT_PENDING);
                    mixpanelHelper.trackCustom(MixpanelHelper.EVENT_ADD_CAR_PROCESS, properties);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.i(TAG, "Device RTC time is off by less than one year, start getting VIN");
                autoConnectService.saveSyncedDevice(parameterPackageInfo.deviceId);
                autoConnectService.getVinFromCar();
                gotValidRTC = true;
            }

        }

        if (parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.VIN_TAG)) {

            linkingAttempts = 0;
            Log.i(TAG, "VIN response received");
            Log.d(TAG, "isGettingVinAndCarIsConnected :" + isGettingVinAndCarIsConnected);

            callback.showLoading("Getting car VIN");

            isGettingVinAndCarIsConnected = false;
            pendingCar.setScannerId(parameterPackageInfo.deviceId);
            LogUtil.i("parameterPackage.size():"
                    + parameterPackageInfo.value.size());

            List<ParameterInfo> parameterValues = parameterPackageInfo.value;
            pendingCar.setVin(parameterValues.get(0).value);
            try {
                new MixpanelHelper(context).trackCustom("Retrieved VIN from device",
                        new JSONObject("{'VIN':'" + pendingCar.getVin() + "','View':'Add Car'}"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (isValidVin(pendingCar.getVin())) {

                vinAttempts = 0;
                Log.i(TAG, "VIN is valid");
                autoConnectService.setFixedUpload();
                callback.showLoading("Loaded car VIN");
                makeCar();

                if (!AddCarActivity.isPairingUnrecognizedDevice) {
                    // We got the valid Vin from the device
                    mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);
                }
            } else if (!needToSetTime || vinAttempts > 8) {

                if (AddCarActivity.isPairingUnrecognizedDevice){
                    callback.showSelectCarDialog(autoConnectService.getConnectedDeviceName(),
                            autoConnectService.getCurrentDeviceId());
                    return;
                }

                // same as in manual input plus vin hint
                Log.i(TAG, "Vin value returned not valid");
                vinAttempts = 0;
                callback.resetScreen();
                callback.hideLoading("Vin value returned not valid, please use Manual Input!");

                // Get vin failed (not support) for 8 times or the RTC on the device does not need to be set
                mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN, "Not Support");
            } else {
                Log.i(TAG, "VIN value returned not valid - attempt: " + vinAttempts);
                vinAttempts++;
                isGettingVinAndCarIsConnected = true;
                mHandler.postDelayed(vinDetectionRunnable, 2000);
            }
        }
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        Log.i(TAG, "getIOData()");
        Log.i(TAG, "result: " + dataPackageInfo.result);

        if (dataPackageInfo.result == 6 && askForDTC) {
            askForDTC = false;

            Log.i(TAG, "Result: " + dataPackageInfo.result + " Asking for dtcs --getIOData()");
            dtcs = "";
            if (dataPackageInfo.dtcData != null && dataPackageInfo.dtcData.length() > 0) {
                Log.i(TAG, "Parsing DTCs");
                String[] DTCs = dataPackageInfo.dtcData.split(",");
                for (String dtc : DTCs) {
                    dtcs += ObdDataUtil.parseDTCs(dtc) + ",";
                }
                // At this point we have retrieved DTCs
                mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_DTCS, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);
            } else if (dataPackageInfo.dtcData == null || dataPackageInfo.dtcData.length() == 0) {
                // Do nothing
            }

            // IF timeout, maybe we should below codes because we will have it executed before timeout
            if (mGetDTCTimeoutRunnable != null && mGetDTCTimeoutRunnable.getDTCTimeOut) {
                return;
            }

            PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(MainDashboardFragment.pfCurrentCar,
                    createdCar.getId()).commit();
            networkHelper.setMainCar(context.getCurrentUserId(), createdCar.getId(), null);
            callback.carSuccessfullyAdded(createdCar);
        }
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            Log.i(TAG, "Device disconnected");
        }
    }

    /**
     * Start MashapeAsync
     */
    public void runVinTask() {
        if (mashapeAsync == null) {
            mashapeAsync = new CallMashapeAsync();
        } else if (mashapeAsync.getStatus().equals(AsyncTask.Status.PENDING)) {
            Log.i("VIN DECODER", "Pending TASK");
        } else if (mashapeAsync.getStatus().equals(AsyncTask.Status.RUNNING)) {
            mashapeAsync.cancel(true);
            mashapeAsync = null;
            mashapeAsync = new CallMashapeAsync();
        } else if (mashapeAsync.getStatus().equals(AsyncTask.Status.FINISHED)) {
            mashapeAsync = null;
            mashapeAsync = new CallMashapeAsync();
        }
        mashapeAsync.execute();
    }

    public boolean isValidVin() {
        return isValidVin(pendingCar.getVin());
    }

    public void setDealership(Dealership dealership) {
        pendingCar.setDealership(dealership);
    }

    /**
     * Complex call in adding car.
     * First it goes to Mashape and get info based on VIN -> determines if valid
     * Second it tries to add to database -> determines if already existing in Parse
     * Third it shows the Shops to choose from and add it to Parse.
     * Fourth uploads the DTCs to server (where server code will run to link to car)
     */
    private class CallMashapeAsync extends AsyncTask<String, Void, String> {
        @Override
        protected void onPostExecute(String error) {
            Log.i(TAG, "On post execute");

            if (!Utils.isEmpty(error)) {
                callback.hideLoading(error);
            }
        }

        @Override
        protected String doInBackground(String... msg) {
            String error = "";
            try {
                StringBuilder response = new StringBuilder();
                URL url = new URL("https://vindecoder.p.mashape.com/decode_vin?vin=" + pendingCar.getVin());
                // Starts the query

                HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
                httpconn.addRequestProperty("Content-Type", "application/x-zip");
                httpconn.addRequestProperty("X-Mashape-Key", context.getString(R.string.mashape_key));
                httpconn.addRequestProperty("Accept", "application/json");
                if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader input = new BufferedReader(
                            new InputStreamReader(httpconn.getInputStream()), 8192);
                    String strLine;
                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                }
                if (new JSONObject(response.toString()).getBoolean("success")) {
                    Log.i(TAG, "Call to mashape succeed");
                    //load mashape info
                    final JSONObject jsonObject =
                            new JSONObject(response.toString()).getJSONObject("specification");

                    // Check if scannerId already exists and if Vin already exists
                    // scannerIdCheck();
                    vinCheck();
                } else {
                    error = "Failed to find by VIN, may be invalid";
                }
            } catch (Exception e) {
                error = e.getMessage();
                e.printStackTrace();
            }
            return error;
        }
    }

    /**
     * Check if Vin exists in the backend.<br>
     * 1. If it does, we show user a toast saying that the car exists. <br>
     * 2. If it doesn't, we call saveCarToServer() and passing in the carInfo, which will do either:<br>
     * <ul><li>a. If the scanner Id doesn't exists, we create the car.</li>
     * <li>b. If it does, we let the user know and stop the add car process.</li></ul>
     */
    private void vinCheck() {
        Log.i(TAG, "vinCheck()");
        //check if car already exists!
        callback.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.showLoading("Checking car vin");
            }
        });

        networkHelper.getCarsByVin(pendingCar.getVin(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null) {
                    if (response.equals("{}")) { // Vin does not exist in the backend

                        if (!AddCarActivity.isPairingUnrecognizedDevice) {
                            // If user is adding a car instead of pairing a car with unrecognized device
                            callback.hideLoading("Please pick the dealership for your car.");
                            callback.askForDealership();
                        } else {
                            callback.hideLoading(null);
                            callback.pairCarError("Oops, we connected to a new car, please turn off your Bluetooth and retry.");
                        }

                        // Attempt to save car on backend, this step will also check if the scannerId exists on backend
                        // saveCarToServer(carInfo);

                    } else { // Vin exists in the backend
                        callback.hideLoading("Car Already Exists!");
                        if (!AddCarActivity.isPairingUnrecognizedDevice){
                            callback.resetScreen();
                        }

                        // Car exists
                        Car existedCar = null;
                        try {
                            existedCar = Car.createCar(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if (existedCar != null) {
                            int carUserId = existedCar.getUserId();
                            String carScannerId = existedCar.getScannerId();
                            Log.d(TAG, "User Id for car " + existedCar.getVin() + " is: " + carUserId);

                            if (carUserId != 0) { // User id is not 0, this car has not been deleted
                                if (AddCarActivity.isPairingUnrecognizedDevice) {
                                    if (carUserId == context.getCurrentUserId()
                                            && carScannerId == null) {
                                        callback.pairCarWithDevice(existedCar,
                                                autoConnectService.getConnectedDeviceName(),
                                                autoConnectService.getCurrentDeviceId());
                                    } else {
                                        callback.pairCarError("The device we found has been paired with another car," +
                                                " please try again later.");
                                    }
                                    return;
                                }

                                String value;
                                if (carUserId == context.getCurrentUserId()) {
                                    value = MixpanelHelper.ADD_CAR_CAR_EXIST_FOR_CURRENT_USER;
                                } else {
                                    value = MixpanelHelper.ADD_CAR_CAR_EXIST_FOR_ANOTHER_USER;
                                }

                                try {
                                    JSONObject json = new JSONObject();
                                    json.put("Car", existedCar.getMake() + " " + existedCar.getModel());
                                    json.put("Button", value);
                                    json.put("View", MixpanelHelper.ADD_CAR_VIEW);
                                    mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, json);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                // This car has been deleted previously
                                // Skip the last page
                                existedCar.setBaseMileage((int) pendingCar.getBaseMileage());
                                existedCar.setScannerId(pendingCar.getScannerId());
                                pendingCar = existedCar;
                                new AnimatedDialogBuilder(callback)
                                        .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                                        .setTitle("Adding deleted car")
                                        .setMessage("This car has been deleted by a previous user, " +
                                                "do you wish to add it?")
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                saveCarToServer();
                                            }
                                        })
                                        .setNegativeButton("CANCEL", null)
                                        .show();
                            }
                        }

                    }
                } else {
                    callback.hideLoading("There was an error, please try again");
                    Log.e(TAG, "Check vin: " + requestError.getMessage());
                }
            }
        });
    }

    /**
     * <p>This method will sum up the car info and post the car to the server</p>
     * <p>This method can also checks if the scanner id exists in the backend, if that so,
     * the callback of network request will receive a requestError from the backend notifying the user that
     * this device has already been used in another car.</p>
     */
    public void saveCarToServer() {
        Log.i("shop selected:", String.valueOf(pendingCar.getShopId()));

        callback.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                callback.showLoading("Saving car details");
            }
        });

        networkHelper.createNewCar(context.getCurrentUserId(),
                pendingCar.equals("") ? 0 : (int) pendingCar.getBaseMileage(),
                pendingCar.getVin(),
                pendingCar.getScannerId() == null ? "" : pendingCar.getScannerId(),
                pendingCar.getShopId(),
                new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        callback.showLoading("Final Touches");

                        if (requestError == null) {
                            Log.i(TAG, "Create car response: " + response);
                            try {
                                createdCar = Car.createCar(response);

                                if (pendingCar.getScannerId() != null && !pendingCar.getScannerId().isEmpty()) {
                                    networkHelper.createNewScanner(createdCar.getId(), pendingCar.getScannerId(), null);
                                }

                                if (pendingCar.getScannerId() != null) {
                                    autoConnectService.saveScannerOnResultPostCar(createdCar);
                                } else {
                                    autoConnectService.saveEmptyScanner(createdCar.getId());
                                }

                                Log.i(TAG, "After successfully posting car to server, scanner saved Locally");

                                // After successfully posting car to server, attempt to get engine codes
                                // Also start timing out, if after 15 seconds it didn't finish, just skip it and jumps to MainActivity
                                if (autoConnectService.getState() == IBluetoothCommunicator.CONNECTED) {
                                    Log.i(TAG, "Now connected to device");

                                    callback.showLoading("Loading car engine codes");
                                    askForDTC = true;
                                    Log.i(TAG, "Make car --- Getting DTCs");

                                    // Check if DTCs are retrieved after 15 seconds
                                    mGetDTCTimeoutRunnable = new GetDTCTimeoutRunnable(System.currentTimeMillis());
                                    mHandler.post(mGetDTCTimeoutRunnable);

                                    autoConnectService.getDTCs();
                                    autoConnectService.getPendingDTCs();
                                } else { // If bluetooth connection state is not connected, then just ignore getting DTCs
                                    PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(MainDashboardFragment.pfCurrentCar,
                                            createdCar.getId()).commit();
                                    networkHelper.setMainCar(context.getCurrentUserId(), createdCar.getId(), null);
                                    callback.carSuccessfullyAdded(createdCar);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                callback.hideLoading("There was an error adding your car, please try again");
                            }
                        } else {
                            //Scanner id exists in backend
                            try {
                                mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_SCANNER_EXISTS_IN_BACKEND, MixpanelHelper.ADD_CAR_VIEW);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Error in posting button tracking info to mixpanel");
                            }
                            callback.hideLoading(requestError.getMessage());
                            Log.e(TAG, "Create new car: " + requestError.getMessage());
                        }
                    }
                });
    }

    /**
     * This method will cancel all runnables and messages.<br>
     * Invoked when the AddCarActivity finished.
     */
    private void cancelAllRunnables() {
        mHandler.removeCallbacksAndMessages(null);
    }

    private final Runnable vinDetectionRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "vinDetectionRunnable run, isGettingVinAndCarIsConnected: " + isGettingVinAndCarIsConnected);
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - vinRetrievalStartTime;
            int seconds = (int) (timeDiff / 1000);

            if (seconds > 10 && isGettingVinAndCarIsConnected) {
                if (linkingAttempts++ > 4) {
                    mHandler.removeCallbacks(vinDetectionRunnable);
                    linkingAttempts = 0;
                    callback.openRetryDialog();

                    if (gotValidRTC) {
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN, "Failed");
                    } else {
                        mixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_RTC, "Failed");
                    }

                } else {
                    mHandler.sendEmptyMessage(HANDLER_MSG_GET_VIN);
                    mHandler.postDelayed(vinDetectionRunnable, 2000);
                }
                return;
            }

            if (!isGettingVinAndCarIsConnected) {
                mHandler.removeCallbacks(vinDetectionRunnable);
                return;
            }

            mHandler.postDelayed(vinDetectionRunnable, 2000);
        }
    };


    private final Runnable carSearchRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - searchTime;
            int seconds = (int) (timeDiff / 1000);
            if (seconds > 15 && (isSearchingForCar) && autoConnectService.getState()
                    == IBluetoothCommunicator.DISCONNECTED) {
                mHandler.sendEmptyMessage(HANDLER_MSG_SEARCH_CAR);
                mHandler.removeCallbacks(carSearchRunnable);
            } else if (!isSearchingForCar) {
                mHandler.removeCallbacks(carSearchRunnable);
            } else {
                mHandler.post(carSearchRunnable);
            }
        }
    };

    private final class GetDTCTimeoutRunnable implements Runnable {

        /**
         * Starting time in milliseconds, initiate when construct the runnable
         */
        private final long startTime;

        /**
         * State variable
         */
        private boolean getDTCTimeOut;

        public GetDTCTimeoutRunnable(long startTime) {
            Log.d("DTC TIMEOUT RUNNABLE", "Created");
            this.startTime = startTime;
            getDTCTimeOut = false;
        }

        @Override
        public void run() {
            Log.d("DTC TIMEOUT RUNNABLE", "Running");
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - startTime;

            int seconds = (int) (timeDiff / 1000);

            // If it finished, then remove the runnable from the handler
            if (!askForDTC) {
                Log.d("DTC TIMEOUT RUNNABLE", "Got DTCs");
                mHandler.removeCallbacks(this);
                getDTCTimeOut = false;
            } else if (seconds > 15) { // If it didn't finish and the time exceeded 15 seconds, let the handler knows
                Log.d("DTC TIMEOUT RUNNABLE", "TIMEOUT");
                getDTCTimeOut = true;
                askForDTC = false;
                mHandler.sendEmptyMessage(HANDLER_MSG_GET_DTC_TIMEOUT);
                mHandler.removeCallbacks(this);
            } else {  // If it didn't finish and it didn't timeout, we wait for another 5 seconds
                Log.d("DTC TIMEOUT RUNNABLE", "Continue");
                mHandler.postDelayed(this, 5000);
            }
        }
    }

    public Car getPendingCar() {
        return pendingCar;
    }

    public interface AddCarUtilsCallback extends ObdManager.IBluetoothDataListener, LoadingActivityInterface {
        void carSuccessfullyAdded(Car car);

        void resetScreen();

        void openRetryDialog();

        BluetoothAutoConnectService getAutoConnectService();

        void postMileageInput();

        void askForDealership();

        void showSelectCarDialog(String scannerName, String scannerId);

        void pairCarWithDevice(Car existedCar, String scannerName, String scannerId);

        void pairCarError(String errorMessage);
    }

}
