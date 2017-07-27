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

import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.LoginPackageInfo;
import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothServiceConnection;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.ReadyDevice;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.BasePresenter;
import com.pitstop.ui.BaseView;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.mainFragments.MainDashboardFragment;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.TimeoutTimer;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Created by yifan on 16/11/22.
 */

public class AddCarPresenter implements AddCarContract.Presenter {

    private static final String TAG = AddCarPresenter.class.getSimpleName();

    private Car pendingCar, createdCar;

    private AddCarContract.View mCallback;
    private final GlobalApplication mApplication;
    private final NetworkHelper mNetworkHelper;
    private final MixpanelHelper mMixpanelHelper;

    private BluetoothAutoConnectService mAutoConnectService;
    private ServiceConnection mServiceConnection;

    private LocalCarAdapter mLocalCarAdapter;

    public static final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_ADD_CAR);

    public AddCarPresenter(IBluetoothServiceActivity activity, GlobalApplication application) {

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(activity))
                .build();

        pendingCar = new Car();
        mApplication = application;
        mNetworkHelper = tempNetworkComponent.networkHelper();
        mMixpanelHelper = new MixpanelHelper(application);
        mLocalCarAdapter = new LocalCarAdapter(application);
        mAutoConnectService = activity.autoConnectService;
        mServiceConnection = new BluetoothServiceConnection(application, activity, this);
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
            mCallback.onMileageInputCancelled();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void searchAndGetVin() {

        checkBluetoothService();

        if (mAutoConnectService != null && mAutoConnectService.getDeviceState()
                .equals(BluetoothConnectionObservable.State.CONNECTED)){
            ReadyDevice readyDevice = mAutoConnectService.getReadyDevice();
            pendingCar.setVin(readyDevice.getVin());
            pendingCar.setScannerId(readyDevice.getScannerId());
        }


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
            if (!mAutoConnectService.getDeviceState().equals(BluetoothConnectionObservable.State.CONNECTED)) {
                // Need to search for module
                Log.i(TAG, "Searching for car but device not connected");
                mCallback.showLoading("Searching for Car");
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
                mCallback.startPendingAddCarActivity(pendingCar);
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
                                        mMixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_SCANNER_EXISTS_IN_BACKEND, MixpanelHelper.ADD_CAR_VIEW);
                                        mCallback.onPostCarFailed(requestError.getMessage());
                                        Log.e(TAG, "Create new car: " + requestError.getMessage());
                                        return;
                                    }

                                    Log.d(TAG, "Create car response: " + response);
                                    try {
                                        createdCar = Car.createCar(response);

                                        //Ask user if they want to connect for sure here

                                        mLocalCarAdapter.storeCarData(createdCar);// not correct,but I need the car to exist locally after its made
                                        List<Car> localCarList = mLocalCarAdapter.getAllCars();

                                        Log.d(TAG, "Current car list size: " + localCarList.size());
                                        Log.d(TAG, "Created car id: " + createdCar.getId());
                                        if (localCarList.size() == 0) {
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
                                            mCallback.askForDealership(createdCar);
                                        } else { // has default shop selected in the backend
                                            mCallback.hideLoading("Great! We have added this car to your account.");
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

                        //Ask user if they want to connect for sure here

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (existedCar != null) {
                        mLocalCarAdapter.updateCar(existedCar);
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
                        mAutoConnectService.connectedDeviceInvalid();
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
                                List<Car> localCarList = mLocalCarAdapter.getAllCars();
                                Log.d(TAG, "Current car list size: " + localCarList.size());
                                Log.d(TAG, "Created car id: " + createdCar.getId());
                                if (localCarList.size() == 0) {
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
                                    mCallback.askForDealership(createdCar);
                                } else { // has shop selected in the backend
                                    onCarSuccessfullyPosted();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                mCallback.hideLoading("There was an error adding your car, please try again");
                            }


                        } else {
                            //Scanner id exists in backend
                            mMixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_SCANNER_EXISTS_IN_BACKEND, MixpanelHelper.ADD_CAR_VIEW);
                            mCallback.hideLoading(requestError.getMessage());
                            Log.e(TAG, "Create new car: " + requestError.getMessage());
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
        if (mAutoConnectService.getDeviceState().equals(BluetoothConnectionObservable.State.CONNECTED)) {
            Log.i(TAG, "Now connected to device");
            Log.i(TAG, "Asking for RTC and Mileage, if connected to 215");
            mAutoConnectService.get215RtcAndMileage();
            mCallback.showLoading("Loading car engine codes");
            getDtcWithTimeout();
        } else {
            // If bluetooth connection state is not connected, then just ignore getting DTCs
            PreferenceManager.getDefaultSharedPreferences(mApplication).edit()
                    .putInt(MainDashboardFragment.pfCurrentCar, createdCar.getId()).apply();
            mNetworkHelper.setMainCar(mApplication.getCurrentUserId(), createdCar.getId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null){

                        EventType type = new EventTypeImpl(EventType.EVENT_CAR_ID);
                        EventBus.getDefault().post(new CarDataChangedEvent(type,EVENT_SOURCE));
                        //mCallback.onPostCarSucceeded(createdCar);
                    }
                }
            });
            //mCallback.onPostCarSucceeded(createdCar);
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
    public List<Car> getAllLocalCars() {
        return mLocalCarAdapter.getAllCars();
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
    }

    @Override
    public void unbindBluetoothService() {
        mCallback.getActivity().unbindService(mServiceConnection);
        pendingCar = null;
    }

    @Override
    public void onServiceUnbind() {
    }

    @Override
    public void checkBluetoothService() {
        if (mAutoConnectService == null) {
            mAutoConnectService = mCallback.getAutoConnectService();
            mAutoConnectService.subscribe(this);
        }
    }

    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGIN_FLAG))) {
            pendingCar.setScannerId(loginPackageInfo.deviceId);
        } else if (loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            pendingCar.setScannerId(null);
        }
    }

    private void searchCarWithTimeout() {
        if (mAutoConnectService.getDeviceState().equals(BluetoothConnectionObservable.State.CONNECTED))
            hasGotValidRtc = false;
        isSearchingForCar = true;
        mSearchCarTimer.cancel();
        mSearchCarTimer.start();
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
    private final TimeoutTimer mSearchCarTimer = new TimeoutTimer(15, 6) {
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
    private final TimeoutTimer mGetVinTimer = new TimeoutTimer(10, 6) {
        @Override
        public void onRetry() {
            if (!isAskingForVin && !isAskingForRtc) {
                this.cancel();
                return;
            }
            if (!mAutoConnectService.getDeviceState().equals(BluetoothConnectionObservable.State.SEARCHING)) {
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
            mNetworkHelper.setMainCar(mApplication.getCurrentUserId(), createdCar.getId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null){

                        EventType type = new EventTypeImpl(EventType.EVENT_CAR_ID);
                        EventBus.getDefault().post(
                                new CarDataChangedEvent(type,EVENT_SOURCE));
                        mCallback.onPostCarSucceeded(createdCar);
                    }
                }
            });
        }
    };

    @Override
    public void bind(BaseView<? extends BasePresenter> view) {
        if (mAutoConnectService != null){
            mAutoConnectService.subscribe(this);
        }

        this.mCallback = (AddCarContract.View)view;
    }

    @Override
    public void unbind() {
        if (mAutoConnectService != null){
            mAutoConnectService.unsubscribe(this);
        }

        this.mCallback = null;
    }

    @Override
    public void onDeviceNeedsOverwrite() {

    }

    @Override
    public void onSearchingForDevice() {
        if (isSearchingForCar) mCallback.showLoading("Connecting to device");
    }

    @Override
    public void onDeviceReady(String vin, String scannerId, String scannerName) {
        Log.d(TAG,"onDeviceReady() vin: "+vin+", scannerId: "+scannerId+", scannerName: "+scannerName);

        mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_CONNECT_TO_BLUETOOTH, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

        if (isSearchingForCar) { // it was searching for device previously
            isSearchingForCar = false;
            mSearchCarTimer.cancel();

            mCallback.onDeviceConnected();

        }

        pendingCar.setScannerId(scannerId);
        String retrievedVin = vin;
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

            pendingCar.setVin(retrievedVin);

            startAddingNewCar();
            mMixpanelHelper.trackAddCarProcess(MixpanelHelper.ADD_CAR_STEP_GET_VIN, MixpanelHelper.ADD_CAR_STEP_RESULT_SUCCESS);

        } else if (!needToSetTime && getVinAttempts > 8) { /* || -> && */
            isAskingForVin = false;
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

    @Override
    public void onDeviceDisconnected() {
        if (isSearchingForCar) mCallback.hideLoading("Disconnected from device, add failed.");
    }

    @Override
    public void onDeviceVerifying() {

    }

    @Override
    public void onDeviceSyncing() {

    }

    @Override
    public void onGotDtc(DtcPackage dtcPackage) {

    }
}
