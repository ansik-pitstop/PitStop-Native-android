package com.pitstop.ui.add_car;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothServiceConnection;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.ObdScanner;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.MainActivity;
import com.pitstop.ui.mainFragments.MainDashboardFragment;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.TimeoutTimer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by yifan on 16/11/22.
 */

public class AddCarPresenter implements AddCarContract.Presenter {

    private static final String TAG = AddCarPresenter.class.getSimpleName();

    private Car pendingCar, createdCar;

    private final AddCarContract.View mCallback;
    private final GlobalApplication mApplication;
    private final NetworkHelper mNetworkHelper;
    private final MixpanelHelper mMixpanelHelper;

    private BluetoothAutoConnectService mAutoConnectService;
    private ServiceConnection mServiceConnection;

    private LocalScannerAdapter mLocalScannerAdapter;

    private final boolean isPairingUnrecognizedDevice;

    public AddCarPresenter(AddCarContract.View callback, GlobalApplication application, boolean isPairingUnrecognizedDevice) {
        pendingCar = new Car();
        mCallback = callback;
        mApplication = application;
        mNetworkHelper = new NetworkHelper(application);
        mMixpanelHelper = new MixpanelHelper(application);
        mLocalScannerAdapter = new LocalScannerAdapter(application);
        mAutoConnectService = callback.getAutoConnectService();
        mServiceConnection = new BluetoothServiceConnection(application, callback.getActivity(), this);
        bindBluetoothService();
        this.isPairingUnrecognizedDevice = isPairingUnrecognizedDevice;
    }

    public boolean hasGotMileage = false;

    @Override
    public void updatePendingCarMileage(int mileage) {
        // The user confirms adding this vehicle after entering the mileage
        try {
            JSONObject properties = new JSONObject();
            properties.put("Button", MixpanelHelper.ADD_CAR_CONFIRM_ADD_VEHICLE);
            properties.put("View", MixpanelHelper.ADD_CAR_VIEW);
            properties.put("Mileage", mileage);
            properties.put("Method of Adding Car", AddCarActivity.addingCarWithDevice ?
                    MixpanelHelper.ADD_CAR_METHOD_DEVICE : MixpanelHelper.ADD_CAR_METHOD_MANUAL);
            mMixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        pendingCar.setBaseMileage(mileage);
        hasGotMileage = true;

        mCallback.onMileageEntered();
        searchAndGetVin();
    }

    @Override
    public void cancelUpdateMileage() {
        // The user cancels adding this vehicle (cancel mileage input)
        try {
            JSONObject properties = new JSONObject()
                    .put("Button", MixpanelHelper.ADD_CAR_CANCEL_ADD_VEHICLE)
                    .put("View", MixpanelHelper.ADD_CAR_VIEW)
                    .put("Method of Adding Car", AddCarActivity.addingCarWithDevice ? MixpanelHelper.ADD_CAR_METHOD_DEVICE : MixpanelHelper.ADD_CAR_METHOD_MANUAL);
            mMixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void searchAndGetVin() {
        checkBluetoothService();

        if (isValidVin(pendingCar.getVin())) { // if Vin has been entered by the user
            Log.d(TAG, "VIN is valid, start creating car");
            startAddingNewCar();
            return;
        }

        if (ContextCompat.checkSelfPermission(mApplication, MainActivity.LOC_PERMS[0]) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(mApplication, MainActivity.LOC_PERMS[1]) != PackageManager.PERMISSION_GRANTED) {
            mCallback.hideLoading("Location permissions are required");
        } else if (BluetoothAdapter.getDefaultAdapter() == null) { // Device doesn't support bluetooth
            mCallback.hideLoading("Your device does not support bluetooth");
        } else {
            if (mAutoConnectService.getState() == BluetoothCommunicator.CONNECTED) {
                // Already connected to module
                Log.i(TAG, "Getting car vin with device connected");
                mCallback.showLoading("Linking with Device, give it a few seconds");
                getVinWithTimeout();
            } else {
                // Need to search for module
                Log.i(TAG, "Searching for car but device not connected");
                mCallback.showLoading("Searching for Car");
                searchCarWithTimeout();
            }
        }
    }

    @Override
    public void searchForUnrecognizedDevice() {
        checkBluetoothService();

        mCallback.showLoading("Searching for scanner, please make sure your car engine is on and OBD device is plugged in.");

        if (ContextCompat.checkSelfPermission(mApplication, MainActivity.LOC_PERMS[0]) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(mApplication, MainActivity.LOC_PERMS[1]) != PackageManager.PERMISSION_GRANTED) {
            mCallback.hideLoading("Location permissions are required");
        } else if (BluetoothAdapter.getDefaultAdapter() == null) { // Device doesn't support bluetooth
            mCallback.hideLoading("Your device does not support bluetooth");
        } else {
            if (mAutoConnectService.getState() == BluetoothCommunicator.CONNECTED) {
                // Already connected to module
                Log.i(TAG, "OBD device is connected, reading VIN");
                mCallback.showLoading("We have found the device, verifying...");
                getVinWithTimeout();
            } else {
                // Need to search for module
                Log.i(TAG, "Searching for car but device not connected");
                mCallback.showLoading("Searching for scanner, please make sure your car engine is on and OBD device is plugged in.");
                searchCarWithTimeout();
            }
        }
    }

    @Override
    public void setPendingCarVin(final String vin) {
        pendingCar.setVin(vin);
    }

    @Override
    public synchronized void startAddingNewCar() {
        checkBluetoothService();

        if (!mCallback.checkNetworkConnection(null)) {
            Log.d(TAG, "Start Pending Add Car");
            if (!isPairingUnrecognizedDevice) {
                mCallback.startPendingAddCarActivity(pendingCar);
            }
            return;
        }

        mCallback.showLoading("Checking VIN information...");
        mNetworkHelper.getCarsByVin(pendingCar.getVin(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    mCallback.hideLoading("There was a network error, please try again");
                    return;
                }

                if (!mCallback.checkNetworkConnection(null)) return;

                if (response.equals("{}")) { // vin does not exist in the backend
                    mCallback.onPostCarStarted();
                    mNetworkHelper.createNewCarWithoutShopId(mApplication.getCurrentUserId(),
                            (int) pendingCar.getBaseMileage(),
                            pendingCar.getVin(),
                            pendingCar.getScannerId() == null ? "" : pendingCar.getScannerId(),
                            new RequestCallback() {
                                @Override
                                public void done(String response, RequestError requestError) {
                                    if (requestError != null) {
                                        //Scanner id exists in backend
                                        try {
                                            mMixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_SCANNER_EXISTS_IN_BACKEND, MixpanelHelper.ADD_CAR_VIEW);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            Log.e(TAG, "Error in posting button tracking info to mixpanel");
                                        }
                                        mCallback.onPostCarFailed(requestError.getMessage());
                                        Log.e(TAG, "Create new car: " + requestError.getMessage());
                                        return;
                                    }

                                    Log.d(TAG, "Create car response: " + response);
                                    try {
                                        createdCar = Car.createCar(response);

                                        Log.d(TAG, "Current car list size: " + MainActivity.carList.size());
                                        Log.d(TAG, "Created car id: " + createdCar.getId());
                                        if (MainActivity.carList.size() == 0) {
                                            Set<String> carsAwaitingTutorial = PreferenceManager.getDefaultSharedPreferences(mApplication)
                                                    .getStringSet(mApplication.getString(R.string.pfAwaitTutorial), new HashSet<String>());
                                            Log.d(TAG, "Old set size: " + carsAwaitingTutorial.size());
                                            Set<String> newSet = new HashSet<>(); // The set returned by preference is immutable
                                            newSet.addAll(carsAwaitingTutorial);
                                            newSet.add(String.valueOf(createdCar.getId()));
                                            Log.d(TAG, "New set size: " + newSet.size());
                                            PreferenceManager.getDefaultSharedPreferences(mApplication).edit()
                                                    .putStringSet(mApplication.getString(R.string.pfAwaitTutorial), newSet)
                                                    .apply();
                                        }

                                        if (pendingCar.getScannerId() != null && !pendingCar.getScannerId().isEmpty()) {
                                            mAutoConnectService.saveScannerOnResultPostCar(createdCar);
                                        } else { // if scannerId is null or empty
                                            mAutoConnectService.saveEmptyScanner(createdCar.getId());
                                        }

                                        if (createdCar.getShopId() == 0) { // no default shop
                                            mCallback.hideLoading("Great! We have added this car to your account, now please pick the dealership for your car.");
                                            mCallback.askForDealership();
                                        } else { // has default shop selected in the backend
                                            onCarSuccessfullyPosted();
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        mCallback.hideLoading("We had problem creating the car, please try again.");
                                    }
                                }
                            });
                } else { // vin exists in the backend
                    Car existedCar = null;
                    try {
                        existedCar = Car.createCar(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (existedCar != null) {
                        int carUserId = existedCar.getUserId();
                        Log.d(TAG, "User Id for car " + existedCar.getVin() + " is: " + carUserId);
                        if (carUserId != 0) { // User id is not 0, this car is still in use
                            mCallback.askForManualVinInput();
                            String eventName;
                            if (carUserId == mApplication.getCurrentUserId()) {
                                eventName = MixpanelHelper.ADD_CAR_CAR_EXIST_FOR_CURRENT_USER;
                            } else {
                                eventName = MixpanelHelper.ADD_CAR_CAR_EXIST_FOR_ANOTHER_USER;
                            }

                            try {
                                JSONObject property = new JSONObject()
                                        .put("Car", existedCar.getMake() + " " + existedCar.getModel())
                                        .put("Button", eventName)
                                        .put("View", MixpanelHelper.ADD_CAR_VIEW);
                                mMixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, property);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            // This car has been deleted previously
                            // Skip the last page
                            existedCar.setBaseMileage((int) pendingCar.getBaseMileage());
                            existedCar.setScannerId(pendingCar.getScannerId());
                            pendingCar = existedCar;
                            mCallback.onConfirmAddingDeletedCar(pendingCar, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    saveCarToServer(pendingCar);
                                }
                            });
                        }
                    } else { // in case error happened when parsing the car response
                        mCallback.askForManualVinInput();
                    }
                }
            }
        });
    }

    private synchronized void saveCarToServer(Car car) {
        Log.i(TAG, "Save car to server: " + car.toString());

        if (!mCallback.checkNetworkConnection(null)) return;

        mCallback.showLoading("Creating car profile...");

        mNetworkHelper.createNewCarWithoutShopId(
                mApplication.getCurrentUserId(),
                (int) pendingCar.getBaseMileage(),
                pendingCar.getVin(),
                pendingCar.getScannerId() == null ? "" : pendingCar.getScannerId(),
                new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        mCallback.showLoading("Final Touches");

                        if (requestError == null) {
                            try {
                                createdCar = Car.createCar(response);

                                Log.d(TAG, "Current car list size: " + MainActivity.carList.size());
                                Log.d(TAG, "Created car id: " + createdCar.getId());
                                if (MainActivity.carList.size() == 0) {
                                    Set<String> carsAwaitingTutorial = PreferenceManager.getDefaultSharedPreferences(mApplication)
                                            .getStringSet(mApplication.getString(R.string.pfAwaitTutorial), new HashSet<String>());
                                    Log.d(TAG, "Old set size: " + carsAwaitingTutorial.size());
                                    Set<String> newSet = new HashSet<>(); // The set returned by preference is immutable
                                    newSet.addAll(carsAwaitingTutorial);
                                    newSet.add(String.valueOf(createdCar.getId()));
                                    Log.d(TAG, "New set size: " + newSet.size());
                                    PreferenceManager.getDefaultSharedPreferences(mApplication).edit()
                                            .putStringSet(mApplication.getString(R.string.pfAwaitTutorial), newSet)
                                            .apply();
                                }

                                if (pendingCar.getScannerId() != null && !pendingCar.getScannerId().isEmpty()) {
                                    mAutoConnectService.saveScannerOnResultPostCar(createdCar);
                                } else { // if scannerId is null or empty
                                    mAutoConnectService.saveEmptyScanner(createdCar.getId());
                                }

                                if (createdCar.getShopId() == 0) { // no default shop or previous shop
                                    mCallback.hideLoading("Great! We have added this car to your account, now please pick the dealership for your car.");
                                    mCallback.askForDealership();
                                } else { // has shop selected in the backend
                                    onCarSuccessfullyPosted();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                mCallback.hideLoading("There was an error adding your car, please try again");
                            }


                        } else {
                            //Scanner id exists in backend
                            try {
                                mMixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_SCANNER_EXISTS_IN_BACKEND, MixpanelHelper.ADD_CAR_VIEW);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e(TAG, "Error in posting button tracking info to mixpanel");
                            }
                            mCallback.hideLoading(requestError.getMessage());
                            Log.e(TAG, "Create new car: " + requestError.getMessage());
                        }
                    }
                });
    }


    @Override
    public void startPairingUnrecognizedDevice() {

        checkBluetoothService();

        if (!mCallback.checkNetworkConnection("Network unavailable. Please turn on your Wi-Fi or Cellular network and try again.")) {
            return;
        }

        mCallback.showLoading("Checking car vin");
        mNetworkHelper.getCarsByVin(pendingCar.getVin(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    mCallback.hideLoading("There was an error, please try again");
                    Log.e(TAG, "Check vin: " + requestError.getMessage());
                    return;
                }

                if (response.equals("{}")) {
                    mCallback.hideLoading(null);
                    mCallback.pairCarError("Oops, we didn't find the information for the car we are currently connected to, please turn off your Bluetooth and retry.");
                } else { // VIN exists in the backend
                    mCallback.hideLoading("Car information found!");

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

                        if (carUserId != mApplication.getCurrentUserId()) {
                            mCallback.pairCarError("Sorry, the car we have connected to (" +
                                    existedCar.getYear() + " " + existedCar.getMake() + " " + existedCar.getModel() +
                                    ") belong to another user, please turn off your bluetooth and try again later.");
                        } else if (carScannerId != null) {
                            mCallback.pairCarError("Sorry, the car we have connected to already has a scanner connected to it," +
                                    " please turn off your bluetooth and try again later.");
                        } else {
                            mCallback.confirmPairCarWithDevice(existedCar,
                                    mAutoConnectService.getConnectedDeviceName(),
                                    mAutoConnectService.getCurrentDeviceId());
                        }
                    } else {
                        mCallback.pairCarError("Sorry, unknown error happened when we are validating the VIN number, please try again.");
                    }
                }
            }
        });
    }

    @Override
    public boolean selectedValidCar(Car car) {
        return !mLocalScannerAdapter.carHasDevice(car.getId());
    }

    /**
     * 1. Check with the backend and see if scanner is valid <br>
     * 2. If so, create an association with the car selected and the scanner <br>
     * 3. On success, store scanner information locally, and finish.
     *
     * @param car         selected car or the car retrieved from the backend by its VIN
     * @param scannerId   The current scannerId
     * @param scannerName The current scannerName
     */
    @Override
    public void validateAndPostScanner(final Car car, final String scannerId, final String scannerName) {

        if (!mCallback.checkNetworkConnection(null)) return;

        mCallback.onPairingDeviceWithCar();

        mNetworkHelper.validateScannerId(scannerId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    mCallback.pairCarError("Network error, please try again later");
                    mMixpanelHelper.trackDetectUnrecognizedModule(MixpanelHelper.UNRECOGNIZED_MODULE_NETWORK_ERROR);
                } else {
                    try {
                        JSONObject result = new JSONObject(response);
                        if (result.has("id")) { //invalid
                            Log.d(TAG, "DeviceID is not valid");
                            mCallback.pairCarError("This device has been paired with another car.");
                            mMixpanelHelper.trackDetectUnrecognizedModule(MixpanelHelper.UNRECOGNIZED_MODULE_INVALID_ID);
                        } else {
                            mNetworkHelper.createNewScanner(car.getId(), scannerId, new RequestCallback() {
                                @Override
                                public void done(String response, RequestError requestError) {
                                    if (requestError != null) {
                                        // Error occurred during creating new scanner
                                        Log.d(TAG, "Create new scanner failed!");
                                        mCallback.pairCarError("Network errors, please try again later");
                                        mMixpanelHelper.trackDetectUnrecognizedModule(MixpanelHelper.UNRECOGNIZED_MODULE_NETWORK_ERROR);
                                    } else {
                                        // Save locally
                                        ObdScanner scanner = new ObdScanner(
                                                car.getId(), scannerName, scannerId);
                                        mLocalScannerAdapter.updateScannerByCarId(scanner);
                                        mMixpanelHelper.trackDetectUnrecognizedModule(MixpanelHelper.UNRECOGNIZED_MODULE_PAIRING_SUCCESS);
                                        mCallback.onDeviceSuccessfullyPaired();
                                    }
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        mCallback.pairCarError("Unknown error, please try again later");
                    }
                }
            }
        });
    }


    @Override
    public void updateCreatedCarDealership(final Dealership dealership) {
        Log.i(TAG, "Shop selected: " + dealership.getName());

        if (!mCallback.checkNetworkConnection(null)) return;

        mCallback.showLoading("Saving shop info...");
        pendingCar.setDealership(dealership);

        mNetworkHelper.updateCarShop(createdCar.getId(), pendingCar.getShopId(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError == null) {
                    createdCar.setDealership(dealership);
                    onCarSuccessfullyPosted();
                } else {
                    Log.e(TAG, "Update car shop error:");
                    Log.e(TAG, requestError.getMessage());
                    Log.e(TAG, requestError.getError());
                    mCallback.showRetryDialog("Network error",
                            "Sorry but it seems that we had some problem updating dealership info for your vehicle, " +
                                    "you can either retry OR cancel and continue without shop. You can always select dealership for your vehicle.",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    updateCreatedCarDealership(dealership);
                                }
                            },
                            null);
                }
            }
        });
    }


    /**
     * Asks for DTCs if connected to bluetooth, otherwise finishes Add Car
     */
    private void onCarSuccessfullyPosted() {
        // After successfully posting car to server, attempt to get engine codes
        // Also start timing out, if after 15 seconds it didn't finish, just skip it and jumps to MainActivity
        cancelAllTimeouts();
        if (mAutoConnectService.getState() == BluetoothCommunicator.CONNECTED) {
            Log.i(TAG, "Now connected to device");
            mCallback.showLoading("Loading car engine codes");
            getDtcWithTimeout();
        } else {
            // If bluetooth connection state is not connected, then just ignore getting DTCs
            PreferenceManager.getDefaultSharedPreferences(mApplication).edit()
                    .putInt(MainDashboardFragment.pfCurrentCar, createdCar.getId()).apply();
            mNetworkHelper.setMainCar(mApplication.getCurrentUserId(), createdCar.getId(), null);
            mCallback.onPostCarSucceeded(createdCar);
        }
    }

    public static boolean isValidVin(final String vin) {
        return vin != null && (vin.length() == 17);
    }

    @Override
    public Car getPendingCar() {
        return pendingCar;
    }

    @Override
    public Car getCreatedCar() {
        return createdCar;
    }

    @Override
    public boolean hasGotMileage() {
        return hasGotMileage;
    }

    @Override
    public void bindBluetoothService() {
        mCallback.getActivity().bindService(
                new Intent(mApplication, BluetoothAutoConnectService.class),
                mServiceConnection,
                Context.BIND_AUTO_CREATE
        );
    }

    @Override
    public void onServiceBound(BluetoothAutoConnectService service) {
        mAutoConnectService = service;
        mAutoConnectService.setCallbacks(this);
    }

    @Override
    public void unbindBluetoothService() {
        mCallback.getActivity().unbindService(mServiceConnection);
        pendingCar = null;
    }

    @Override
    public void onServiceUnbind() {
        mAutoConnectService = null;
    }

    @Override
    public void checkBluetoothService() {
        if (mAutoConnectService == null) {
            mAutoConnectService = mCallback.getAutoConnectService();
            mAutoConnectService.setCallbacks(this);
        }
    }

    @Override
    public void getBluetoothState(int state) {
        Log.i(TAG, "Bluetooth state update");
        switch (state) {
            case BluetoothCommunicator.CONNECTED:
                // Successfully connected to OBD device
                mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

                if (isSearchingForCar) { // it was searching for device previously
                    isSearchingForCar = false;
                    mSearchCarTimer.cancel();

                    mCallback.onDeviceConnected();

                    hasGotValidRtc = false; // reset flag variable
                    getVinWithTimeout(); // get RTC then get VIN
                }
                break;

            case BluetoothCommunicator.CONNECTING:
                if (isSearchingForCar) mCallback.showLoading("Connecting to device");
                break;
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {
        Log.i(TAG, "Set Parameter Response");

        if ((responsePackageInfo.type + responsePackageInfo.value).equals(ObdManager.RTC_TAG)) {
            // Once device time is reset, the obd device disconnects from mobile device
            Log.i(TAG, "Set parameter() device time is set-- starting bluetooth search");
            mCallback.onRTCReset();
            hasGotValidRtc = true;
            needToSetTime = false;
            mSetRtcTimer.cancel();
            searchAndGetVin();
        }
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            pendingCar.setScannerId(loginPackageInfo.deviceId);
        } else if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            pendingCar.setScannerId(null);
        }
    }

    @Override
    public void tripData(TripInfoPackage tripInfoPackage) {

    }

    @Override
    public void parameterData(ParameterPackage parameterPackage) {
        Log.i(TAG, "Get parameter data");

        ParameterPackage.ParamType type = parameterPackage.paramType;

        switch (type) {
            case RTC_TIME:
                if (!isAskingForRtc) return;
                pendingCar.setScannerId(parameterPackage.deviceId);

                // If RTC is off by more than a year, a lot of stuff get fucked up
                // So if that is the case, we reset the RTC using current time
                Log.i(TAG, "Returned RTC: " + parameterPackage.value);
                long moreThanOneYear = 32000000;
                long deviceTime = Long.valueOf(parameterPackage.value);
                long currentTime = System.currentTimeMillis() / 1000;
                long diff = currentTime - deviceTime;

                // Now we got RTC from the device
                try {
                    JSONObject properties = new JSONObject()
                            .put(MixpanelHelper.ADD_CAR_STEP, MixpanelHelper.ADD_CAR_STEP_GET_RTC)
                            .put("View", MixpanelHelper.ADD_CAR_VIEW)
                            .put("RTC Time", String.valueOf(deviceTime))
                            .put(MixpanelHelper.ADD_CAR_STEP_RESULT, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);
                    mMixpanelHelper.trackCustom(MixpanelHelper.EVENT_ADD_CAR_PROCESS, properties);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mCallback.onRTCRetrieved(diff > moreThanOneYear);

                if (diff > moreThanOneYear) {
                    Log.i(TAG, "Device RTC time is off by more than one year");
                    setRtcWithTimeout();
                    try {
                        JSONObject properties = new JSONObject()
                                .put(MixpanelHelper.ADD_CAR_STEP, MixpanelHelper.ADD_CAR_STEP_SET_RTC)
                                .put("RTC Time", String.valueOf(deviceTime))
                                .put(MixpanelHelper.ADD_CAR_STEP_RESULT, MixpanelHelper.ADD_CAR_STEP_RESULT_PENDING);
                        mMixpanelHelper.trackCustom(MixpanelHelper.EVENT_ADD_CAR_PROCESS, properties);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.i(TAG, "Device RTC time is within the desired range, start getting VIN");
                    hasGotValidRtc = true;
                    mAutoConnectService.saveSyncedDevice(parameterPackage.deviceId);
                    mAutoConnectService.getVinFromCar(); // get parameter VIN
                }
                break;
            case VIN:
                if (!isAskingForVin) return;
                pendingCar.setScannerId(parameterPackage.deviceId);
                String retrievedVin = parameterPackage.value;
                Log.d(TAG, "Retrieved VIN: " + retrievedVin);

                mCallback.showLoading("Getting car VIN");

                try {
                    JSONObject properties = new JSONObject()
                            .put("VIN", retrievedVin)
                            .put("View", MixpanelHelper.ADD_CAR_VIEW);
                    mMixpanelHelper.trackCustom("Retrieved VIN from device", properties);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (isValidVin(retrievedVin)) {
                    Log.i(TAG, "Retrieved VIN is valid");
                    mCallback.onVINRetrieved(retrievedVin, true);

                    isAskingForVin = false;
                    getVinAttempts = 0;
                    mGetVinTimer.cancel();

                    mAutoConnectService.setFixedUpload();
                    pendingCar.setVin(retrievedVin);

                    if (isPairingUnrecognizedDevice) { // is adding a scanner to a car
                        startPairingUnrecognizedDevice();
                    } else { // is adding a new car (with a scanner)
                        startAddingNewCar();
                        mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);
                    }
                } else if (!needToSetTime && getVinAttempts > 8) { /* || -> && */
                    isAskingForVin = false;
                    if (isPairingUnrecognizedDevice) {
                        mCallback.showSelectCarDialog(mAutoConnectService.getConnectedDeviceName(),
                                mAutoConnectService.getCurrentDeviceId());
                        return;
                    }

                    mCallback.onVINRetrieved(null, false);

                    Log.i(TAG, "Vin returned was not valid");
                    getVinAttempts = 0;
                    pendingCar.setVin("");
                    mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN, "Not Support");
                } else {
                    Log.i(TAG, "VIN returned is not valid, attempts: " + getVinAttempts);
                    getVinAttempts++;
                    isAskingForVin = true;
                }
        }
    }

    @Override
    public void pidData(PidPackage pidPackage) {

    }

    @Override
    public void dtcData(DtcPackage dtcPackage) {
        Log.d(TAG, "dtcData() - Num of dtc:" + dtcPackage.dtcs.length);
        if (isAskingForDtc) {
            isAskingForDtc = false;

            mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_DTCS, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

            PreferenceManager.getDefaultSharedPreferences(mApplication).edit().putInt(MainDashboardFragment.pfCurrentCar,
                    createdCar.getId()).apply();
            mNetworkHelper.setMainCar(mApplication.getCurrentUserId(), createdCar.getId(), null);
            mCallback.onPostCarSucceeded(createdCar);
        }
    }

    @Override
    public void ffData(FreezeFramePackage ffPackage) {

    }

    private void searchCarWithTimeout() {
        if (mAutoConnectService.getState() != BluetoothCommunicator.CONNECTED) hasGotValidRtc = false;
        mAutoConnectService.startBluetoothSearch(2);  // search for car
        isSearchingForCar = true;
        mSearchCarTimer.cancel();
        mSearchCarTimer.start();
    }

    private void getVinWithTimeout() {
        mAutoConnectService.getCarVIN();
        isAskingForRtc = true;
        isAskingForVin = true;
        mGetVinTimer.cancel();
        mGetVinTimer.start();
    }

    private void setRtcWithTimeout() {
        needToSetTime = true;
        mSetRtcTimer.start();
        hasGotValidRtc = false;
        mAutoConnectService.syncObdDevice();
    }

    /**
     * Check if DTCs are retrieved after 15 seconds
     */
    private void getDtcWithTimeout() {
        isAskingForDtc = true;
        mGetDtcTimer.cancel();
        mGetDtcTimer.start();
        mAutoConnectService.getDTCs();
//        mAutoConnectService.getPendingDTCs();
    }

    @Override
    public void cancelAllTimeouts() {
        isSearchingForCar = false;
        mSearchCarTimer.cancel();
        needToSetTime = false;
        mSetRtcTimer.cancel();
        isAskingForVin = false;
        mGetVinTimer.cancel();
        isAskingForDtc = false;
        mGetDtcTimer.cancel();
    }

    @Override
    public void finish() {
        cancelAllTimeouts();
        hasGotMileage = false;
    }

    private boolean isSearchingForCar = false; // flag variable
    private final TimeoutTimer mSearchCarTimer = new TimeoutTimer(20, 3) {
        @Override
        public void onRetry() {
            if (!isSearchingForCar) {
                this.cancel();
                return;
            }
            isSearchingForCar = true;
            searchAndGetVin();
        }

        @Override
        public void onTimeout() {
            if (!isSearchingForCar) return;
            isSearchingForCar = false;
            mMixpanelHelper.trackAlertAppeared(MixpanelHelper.ADD_CAR_ALERT_CONNECT, MixpanelHelper.ADD_CAR_VIEW);
            mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH, "Failed");
            mCallback.onTimeoutRetry("Searching for car", MixpanelHelper.ADD_CAR_RETRY_CONNECT);
        }
    };

    private boolean needToSetTime = false; // flag variable
    private final TimeoutTimer mSetRtcTimer = new TimeoutTimer(40, 0) {
        @Override
        public void onRetry() {
        }

        @Override
        public void onTimeout() {
            if (!needToSetTime) return;
            needToSetTime = false;
            mMixpanelHelper.trackAlertAppeared(MixpanelHelper.ADD_CAR_ALERT_SET_RTC, MixpanelHelper.ADD_CAR_VIEW);
            mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_SET_RTC, "Failed");
            mCallback.onTimeoutRetry("Syncing OBD device", MixpanelHelper.ADD_CAR_RETRY_SET_RTC);
        }
    };

    private boolean hasGotValidRtc = false;
    private boolean isAskingForRtc = false;
    private boolean isAskingForVin = false;
    private int getVinAttempts = 0;
    private final TimeoutTimer mGetVinTimer = new TimeoutTimer(15, 4) {
        @Override
        public void onRetry() {
            if (!isAskingForVin && !isAskingForRtc) {
                this.cancel();
                return;
            }
            if (mAutoConnectService.getState() == BluetoothCommunicator.DISCONNECTED) {
                mAutoConnectService.startBluetoothSearch(1);  // when getting vin and disconnected
            } else {
                getVinAttempts++;
                mAutoConnectService.getCarVIN();
            }
        }

        @Override
        public void onTimeout() {
            if (!isAskingForVin && isAskingForRtc) return;
            pendingCar.setVin("");
            isAskingForRtc = false;
            isAskingForVin = false;
            if (hasGotValidRtc) {
                mMixpanelHelper.trackAlertAppeared(MixpanelHelper.ADD_CAR_ALERT_GET_VIN, MixpanelHelper.ADD_CAR_VIEW);
                mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN, "Failed");
                mCallback.onTimeoutRetry("Read VIN from scanner", MixpanelHelper.ADD_CAR_RETRY_GET_VIN);
            } else {
                mMixpanelHelper.trackAlertAppeared(MixpanelHelper.ADD_CAR_ALERT_GET_RTC, MixpanelHelper.ADD_CAR_VIEW);
                mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_RTC, "Failed");
                mCallback.onTimeoutRetry("Read RTC from scanner", MixpanelHelper.ADD_CAR_RETRY_GET_RTC);
            }
        }
    };

    private boolean isAskingForDtc = false;
    private final TimeoutTimer mGetDtcTimer = new TimeoutTimer(15, 0) {
        @Override
        public void onRetry() {
        }

        @Override
        public void onTimeout() {
            if (!isAskingForDtc) return;
            isAskingForDtc = false;
            mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_DTCS_TIMEOUT,
                    MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);
            // If getting DTCs timeout, for the sake of keeping good UX, we skip it
            PreferenceManager.getDefaultSharedPreferences(mApplication).edit()
                    .putInt(MainDashboardFragment.pfCurrentCar, createdCar.getId()).apply();
            mNetworkHelper.setMainCar(mApplication.getCurrentUserId(), createdCar.getId(), null);
            mCallback.onPostCarSucceeded(createdCar);
        }
    };

    @Override
    public void start() {

    }
}
