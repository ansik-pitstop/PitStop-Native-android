package com.pitstop.utils;

import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.models.User;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Ben Wu on 2016-05-13.
 */
public class MixpanelHelper {

    /**
     * Event
     */
    public static final String EVENT_BUTTON_TAPPED = "Button Tapped";
    public static final String EVENT_VIEW_APPEARED = "View Appeared";
    public static final String EVENT_APP_STATUS = "App Status";
    public static final String EVENT_PERIPHERAL_CONNECTION_STATUS = "Peripheral Connection Status";
    public static final String EVENT_SCROLLED_IN_VIEW = "Scrolled in View";
    public static final String EVENT_SWIPED_TO_TAB = "Swiped through Tab";
    public static final String EVENT_TAPPED_FAB = "Floating Action Button Clicked";
    public static final String EVENT_SCAN_COMPLETE = "Scan Complete";
    public static final String EVENT_ADD_CAR_PROCESS = "Add Car Process";
    public static final String EVENT_ALERT_APPEARED = "Alert Appeared";
    public static final String EVENT_PAIR_UNRECOGNIZED_MODULE = "Pair Unrecognized Module";

    /**
     * General Button
     */
    public static final String BUTTON_BACK = "Back";

    /**
     * Application Status
     */
    public static final String APP_LAUNCHED = "Launched";
    public static final String APP_LAUNCHED_FROM_PUSH = "Launched from Push";
    public static final String APP_ENTERED_BACKGROUND = "Entered Background";
    public static final String APP_ENTERED_FOREGROUND = "Entered Foreground";
    public static final String APP_TERMINATE = "Will Terminate";

    //    public static final String DISCONNECTED = "Disconnected from Bluetooth";
    public static final String DISCONNECTED = "Disconnected";
    //    public static final String CONNECTED = "Connected to Bluetooth";
    public static final String CONNECTED = "Connected";

    /**
     * Login View and Registration View
     */
    public static final String LOGIN_FORGOT_PASSWORD = "Forgot Password";
    public static final String LOGIN_LOGIN_WITH_FACEBOOK = "Login with Facebook";
    public static final String LOGIN_REGISTER_WITH_EMAIL = "Register with Email";
    public static final String LOGIN_LOGIN_WITH_EMAIL = "Login with Email";
    public static final String LOGIN_REGISTER_WITH_FACEBOOK = "Register with Facebook"; // Added
    public static final String LOGIN_VIEW = "Login";
    public static final String ONBOARDING_VIEW_APPEARED = "Onboarding"; // Added
    public static final String REGISTER_BUTTON_TAPPED = "Register";
    public static final String REGISTER_VIEW = "Register";
    public static final String CONFIRM_INFORMATION_VIEW = "Confirm Information";
    public static final String CONFIRM_INFORMATION_CONTINUE = "Continue";
    public static final String FORGOT_PASSWORD_CANCEL = "Cancel Forgot Password";
    public static final String FORGOT_PASSWORD_CONFIRM = "Confirm Forgot Password";

    /**
     * Tutorial/Onboarding view
     */
    public static final String TUTORIAL_VIEW_APPEARED = "Tutorial Onboarding";
    public static final String TUTORIAL_GET_STARTED_TAPPED = "Get Started";

    /**
     * Add Car View / Activity
     */
    public static final String ADD_CAR_BACK = "Back";
    public static final String ADD_CAR_CAR_EXIST_FOR_CURRENT_USER = "Car already exists for user";
    public static final String ADD_CAR_CAR_EXIST_FOR_ANOTHER_USER = "Car already exists for another user";
    public static final String ADD_CAR_BLUETOOTH_RETRY = "Try to connect bluetooth again";
    public static final String ADD_CAR_SCANNER_EXISTS_IN_BACKEND = "Scanner Exists in Backend";
    public static final String ADD_CAR_ADD_CAR_TAPPED = "Add Car";
    public static final String ADD_CAR_SEARCH_TAPPED = "Search For Vehicle";
    public static final String ADD_CAR_VIEW = "Add Car";
    /**
     * In step 1
     */
    public static final String ADD_CAR_ASK_HAS_DEVICE_VIEW = "Ask If User Has Device View";
    public static final String ADD_CAR_YES_HARDWARE = "Yes I have Pitstop Hardware";
    public static final String ADD_CAR_NO_HARDWARE = "No I do not have Pitstop Hardware";
    /**
     * In step 2
     */
    public static final String ADD_CAR_SEARCH_DEVICE_VIEW = "Search for Device View";
    public static final String ADD_CAR_VIN_ENTRY_VIEW = "Enter Vin Manually View";
    public static final String ADD_CAR_NO_HARDWARE_ADD_VEHICLE = "Add Vehicle";
    public static final String ADD_CAR_YES_HARDWARE_ADD_VEHICLE = "Add Vehicle";
    public static final String ADD_CAR_METHOD_DEVICE = "Through Device";
    public static final String ADD_CAR_METHOD_MANUAL = "Manual";
    public static final String ADD_CAR_CONFIRM_ADD_VEHICLE = "Confirm Add Vehicle";
    public static final String ADD_CAR_CANCEL_ADD_VEHICLE = "Cancel Add Vehicle";
    public static final String ADD_CAR_SCAN_VIN_BARCODE = "Scan VIN Barcode";
    public static final String ADD_CAR_BARCODE_SCANNER_VIEW_APPEARED = "Barcode Scanner";
    /**
     * In step 3
     */
    public static final String ADD_CAR_SELECT_DEALERSHIP_VIEW = "Select Dealership";
    /**
     * General add car steps
     */
    public static final String ADD_CAR_STEP = "Step";
    public static final String ADD_CAR_STEP_RESULT = "Result";
    public static final String ADD_CAR_STEP_RESULT_SUCCESS = "Success";
    public static final String ADD_CAR_STEP_RESULT_PENDING = "Pending";
    public static final String ADD_CAR_STEP_RESULT_FAILED = "Failed";
    public static final String ADD_CAR_STEP_CONNECT_TO_BLUETOOTH = "Connecting to Bluetooth";
    public static final String ADD_CAR_STEP_GET_VIN = "Getting VIN";
    public static final String ADD_CAR_STEP_GET_DTCS = "Getting DTCs";
    public static final String ADD_CAR_STEP_GET_DTCS_TIMEOUT = "Timeout when getting DTCs";

    /**
     * Timeout alert
     */
    public static final String ADD_CAR_RETRY_GET_VIN = "Try Getting VIN Again";
    public static final String ADD_CAR_SUCCESS_GET_VIN = "Get Vin Success";
    public static final String ADD_CAR_NOT_SUPPORT_VIN = "VIN Not Supported";

    /**
     * Main Activity
     */
    public static final String MAIN_ACTIVITY_OPEN_SIDE_MENU = "Open Side Menu";
    public static final String MAIN_ACTIVITY_CLOSE_SIDE_MENU = "Close Side Menu";

    /**
     * Dashboard
     */
    public static final String DASHBOARD_ALERT_ADD_VIN = "Add VIN through Alert";
    public static final String DASHBOARD_ALERT_ADD_NEW_CAR = "Add New Car through Alert";
    public static final String DASHBOARD_ALERT_CANCEL = "Cancel Adding Car through Alert";
    public static final String DASHBOARD_VIEW = "Dashboard";

    /**
     * Tools
     */
    public static final String TOOLS_VIEW = "Tools";

    /**
     * Scan View / Activity
     */
    public static final String SCAN_CAR_CONFIRM_SCAN = "Confirm Scan";
    public static final String SCAN_CAR_RETRY_SCAN = "Retry Scan (Vehicle not connected)";
    public static final String SCAN_CAR_CANCEL_SCAN = "Cancel (Vehicle not connected)";
    public static final String SCAN_CAR_ALLOW_BLUETOOTH_ON = "Settings (Bluetooth not on)";
    public static final String SCAN_CAR_DENY_BLUETOOTH_ON = "Cancel (Bluetooth not on)";
    public static final String SCAN_CAR_VIEW = "Scan";

    /**
     * Issue details
     */
    public static final String ISSUE_DETAIL_VIEW = "Issue Detail";

    /**
     * CarService History
     */
    public static final String SERVICE_HISTORY_VIEW = "Service History";

    /**
     * Settings Activity
     */
    public static final String SETTINGS_VIEW = "Settings";
    public static final String DELETE_CAR = "Delete Car";
    public static final String DELETE_CAR_CONFIRM = "Confirm Delete Car";
    public static final String DELETE_CAR_CANCEL = "Cancel Delete Car";
    public static final String DELETE_CAR_ERROR = "Delete Car Error";

    /**
     * Detect unrecognized module
     */
    public static final String UNRECOGNIZED_MODULE_FOUND = "Found Unrecognized Module";
    public static final String UNRECOGNIZED_MODULE_NETWORK_ERROR = "Network Error";
    public static final String UNRECOGNIZED_MODULE_INVALID_ID = "Invalid ID";
    public static final String UNRECOGNIZED_MODULE_PAIRING_SUCCESS = "Paring Success";
    public static final String UNRECOGNIZED_MODULE_VIEW = "Unrecognized Module Detected";
    public static final String UNRECOGNIZED_MODULE_STATUS = "Status";

    /**
     * CarService request
     */
    public static final String SERVICE_REQUEST_VIEW = "Service Request";

    /**
     * Add preset issues
     */
    public static final String ADD_PRESET_ISSUE_BUTTON = "Add Preset Issue";
    public static final String ADD_PRESET_ISSUE_CONFIRM = "Confirm Add Preset Issues";
    public static final String ADD_PRESET_ISSUE_CANCEL = "Cancel Add Preset Issues";

    /**
     * Time event
     */
    public static final String TIME_EVENT_BLUETOOTH_CONNECTED = "Bluetooth Connected Time";
    public static final String TIME_EVENT_ADD_CAR = "Add Car Time";
    public static final String TIME_EVENT_SCAN_CAR = "Scan Car Time";
    public static final String TIME_EVENT_APP_OPEN = "App Open Time";

    /**
     * Notifications
     * */
    public static final String NOTIFICATION_DISPLAYED = "Notification(s) Displayed";
    public static final String NOTIFICATION_FETCH_ERROR = "Error in fetching Notification(s) / Network Error";
    public static final String NO_NOTIFICATION_DISPLAYED = "Empty Notification Message Displated";

    /**
     * Bluetooth Events
     */
    public static final String EVENT_BLUETOOTH = "Bluetooth Event";

    public static final String BT_TRIP_START_RT_SUCCESS = "Real-Time Trip Start Processed Successfully";
    public static final String BT_TRIP_START_FAILED = "Trip Start Failed To Process";
    public static final String BT_TRIP_START_HT_SUCCESS = "Historical Trip Start Processed Successfully";
    public static final String BT_TRIP_END_RT_SUCCESS = "Real-Time Trip Start Processed Successfully";
    public static final String BT_TRIP_END_FAILED = "Trip Start Failed To Process";
    public static final String BT_TRIP_END_HT_SUCCESS = "Historical Trip Start Processed Successfully";
    public static final String BT_TRIP_END_RECEIVED = "Trip End Received";
    public static final String BT_TRIP_START_RECEIVED = "Trip Start Received";
    public static final String BT_TRIP_NOT_PROCESSED = "Trip Not Processed";
    public static final String BT_TRIP_END = "Trip End Processing";
    public static final String BT_DTC_REQUESTED = "Requested DTC";
    public static final String BT_VIN_GOT = "Received VIN";
    public static final String BT_DTC_GOT = "Received DTC";
    public static final String BT_RTC_GOT = "Received RTC";
    public static final String BT_SYNCING = "Syncing RTC";
    public static final String BT_CONNECTED = "Connected to Verified Device";
    public static final String BT_VERIFYING = "Verifying Device";
    public static final String BT_SEARCHING = "Searching for Device";
    public static final String BT_DISCONNECTED = "Disconnected from Device";
    public static final String BT_SCAN_URGENT = "Started Urgent Scan";
    public static final String BT_SCAN_NOT_URGENT = "Started Non-urgent Scan";
    public static final String BT_DEVICE_BROKEN = "Device Recognized as Broken";

    private GlobalApplication application;

    public MixpanelHelper(GlobalApplication context) {
        application = context;
    }

    public void trackAppStatus(String value)  {
        JSONObject json = new JSONObject();
        try {
            json.put("Status", value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        insertUsername(json);
        insertCar(json);
        application.getMixpanelAPI().track(EVENT_APP_STATUS, json);
    }

    private void insertUsername(JSONObject json)  {
        User user = application.getCurrentUser();
        if (application.getCurrentUser() != null) {
            try {
                json.put("Username", user.getEmail());
            } catch (JSONException e) {
                e.
                        printStackTrace();
            }
        }
    }

    //Insert car data into json object if available
    private void insertCar(JSONObject json)  {
        Car car = application.getCurrentCar();

        if (application.getCurrentCar() != null) {
            try {
                json.put("make", car.getMake());
                json.put("model", car.getModel());
                json.put("year", car.getYear());
                json.put("carId", car.getId());
                json.put("mileage",car.getTotalMileage());
                json.put("services",car.getNumberOfServices());
                json.put("recalls",car.getNumberOfRecalls());
                json.put("engineCodes",car.getEngine());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void trackViewAppeared(String value){
        JSONObject json = new JSONObject();
        try {
            json.put("View", value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        insertUsername(json);
        insertCar(json);
        application.getMixpanelAPI().track(EVENT_VIEW_APPEARED, json);
    }

    public void trackConnectionStatus(String value) {
        JSONObject json = new JSONObject();
        try {
            json.put("Status", value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        insertUsername(json);
        insertCar(json);
        application.getMixpanelAPI().track(EVENT_PERIPHERAL_CONNECTION_STATUS, json);
    }

    public void trackButtonTapped(String value, String view) {
        JSONObject json = new JSONObject();
        try {
            json.put("Button", value);
            json.put("View", view);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        insertUsername(json);
        insertCar(json);
        application.getMixpanelAPI().track(EVENT_BUTTON_TAPPED, json);
    }

    public void trackScrolledInView(String view) {
        JSONObject json = new JSONObject();
        try {
            json.put("View", view);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        insertUsername(json);
        insertCar(json);
        application.getMixpanelAPI().track(EVENT_SCROLLED_IN_VIEW, json);
    }

    public void trackSwitchedToTab(String tab){
        JSONObject json = new JSONObject();
        try {
            json.put("Tab", tab);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        insertUsername(json);
        insertCar(json);
        application.getMixpanelAPI().track(EVENT_SWIPED_TO_TAB, json);
    }

    public void trackFabClicked(String fab){
        JSONObject json = new JSONObject();
        try {
            json.put("Fab", fab);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        insertUsername(json);
        insertCar(json);
        application.getMixpanelAPI().track(EVENT_TAPPED_FAB, json);
    }

    public void trackCarAdded(String view, String mileage, String method) {
        JSONObject json = new JSONObject();
        try {
            json.put("Button", "Add Car");
            json.put("View", view);
            json.put("Mileage", mileage);
            json.put("Method of Adding Car", method);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        insertUsername(json);
        insertCar(json);
        application.getMixpanelAPI().track(EVENT_BUTTON_TAPPED, json);
    }

    public void trackMigrationProgress(String status, int userId){
        JSONObject json = new JSONObject();
        try {
            json.put("Status", status);
            json.put("UserId", userId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        insertUsername(json);
        insertCar(json);
        application.getMixpanelAPI().track("Migration Status", json);
    }

    /**
     * @param event
     * @param properties
     * @throws JSONException
     */
    public void trackCustom(String event, JSONObject properties) {
        insertUsername(properties);
        insertCar(properties);
        application.getMixpanelAPI().track(event, properties);
    }

    /**
     * <p>Track add car steps</p>
     * Error handling predefined (print stack trace). If you want to handle the error yourself, see {@link #trackCustom(String, JSONObject)}
     *
     * @param step
     * @param result
     */
    public void trackAddCarProcess(String step, String result) {
        try {
            JSONObject properties = new JSONObject();
            properties.put(ADD_CAR_STEP, step)
                    .put(ADD_CAR_STEP_RESULT, result);
            trackCustom(EVENT_ADD_CAR_PROCESS, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Track steps of pairing car with unrecognized module.<br>
     * Error handling predefined (print stack trace). If you want to handle the error yourself, see {@link #trackCustom(String, JSONObject)}
     *
     * @param status status of paring
     */
    public void trackDetectUnrecognizedModule(String status) {
        try {
            JSONObject properties = new JSONObject();
            properties.put(UNRECOGNIZED_MODULE_STATUS, status);
            trackCustom(EVENT_PAIR_UNRECOGNIZED_MODULE, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackBluetoothEvent(String status, boolean deviceVerified, String deviceConnState,
            long terminalRtcTime){
        try {
            JSONObject properties = new JSONObject();
            properties.put(EVENT_BLUETOOTH, status);
            properties.put("DeviceVerified",deviceVerified);
            properties.put("ConnectionState",deviceConnState);
            properties.put("TerminalRtcTime",terminalRtcTime);
            trackCustom(EVENT_BLUETOOTH, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackBluetoothEvent(String status, String scannerId, boolean deviceVerified
            , String deviceConnState, long terminalRtcTime){
        try {
            if (scannerId == null) scannerId = "";
            JSONObject properties = new JSONObject();
            properties.put(EVENT_BLUETOOTH, status);
            properties.put("ScannerId",scannerId);
            properties.put("DeviceVerified",deviceVerified);
            properties.put("ConnectionState",deviceConnState);
            properties.put("TerminalRtcTime",terminalRtcTime);
            trackCustom(EVENT_BLUETOOTH, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackBluetoothEvent(String status, String scannerId, String vin,boolean deviceVerified
            , String deviceConnState, long terminalRtcTime){
        try {
            if (scannerId == null) scannerId = "";
            JSONObject properties = new JSONObject();
            properties.put(EVENT_BLUETOOTH, status);
            properties.put("ScannerId",scannerId);
            properties.put("Vin",vin);
            properties.put("DeviceVerified",deviceVerified);
            properties.put("ConnectionState",deviceConnState);
            properties.put("TerminalRtcTime",terminalRtcTime);
            trackCustom(EVENT_BLUETOOTH, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*add event called ?Alert Appeared? which would show up everytime an alert or toast message pops up.
    parameters are ?Alert Name? which is the name/message of the alert/toast and ?View? which is the view the alert/toast appeared in*/

    /**
     * Track event called ?Alert Appeared? which would show up every time an alert or toast message pops up. <br>
     *
     * @param alertName
     * @param view
     */
    public void trackAlertAppeared(String alertName, String view) {
        try {
            JSONObject properties = new JSONObject();
            properties.put("Alert Name", alertName)
                    .put("View", view);
            trackCustom(EVENT_ALERT_APPEARED, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void trackTimeEventStart(String timeEvent) {
        application.getMixpanelAPI().timeEvent(timeEvent);
    }

    public void trackTimeEventEnd(String timeEvent) {
        application.getMixpanelAPI().track(timeEvent);
    }


}
