package com.pitstop.utils;

import android.app.Activity;

import com.pitstop.BuildConfig;
import com.pitstop.models.User;
import com.pitstop.application.GlobalApplication;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Ben Wu on 2016-05-13.
 */
public class MixpanelHelper {

    private GlobalApplication application;

//    Event
    public static final String EVENT_BUTTON_TAPPED = "Button Tapped";
    public static final String EVENT_VIEW_APPEARED = "View Appeared";
    public static final String EVENT_APP_STATUS = "App Status";
    public static final String EVENT_PERIPHERAL_CONNECTION_STATUS = "Peripheral Connection Status";
    public static final String EVENT_SCROLLED_IN_VIEW = "Scrolled in View";
    public static final String EVENT_SCAN_COMPLETED = "Scan Complete";

//    General Button
    public static final String BUTTON_BACK = "Back";

//    Application Status
    public static final String APP_LAUNCHED = "Launched";
    public static final String APP_LAUNCHED_FROM_PUSH = "Launched from Push";
    public static final String APP_ENTERED_BACKGROUND = "Entered Background";
    public static final String APP_ENTERED_FOREGROUND = "Entered Foreground";
    public static final String APP_TERMINATE = "Will Terminate";

    public static final String DISCONNECTED = "Disconnected from Bluetooth";
    public static final String CONNECTED = "Connected to Bluetooth";

    public static final String ADDED_MANUALLY = "Car Added Manually";
    public static final String ADDED_WITH_DEVICE = "Car Added Through Device";

//    Login View and Registration View
    public static final String LOGIN_FORGOT_PASSWORD = "Forgot Password";
    public static final String LOGIN_LOGIN_WITH_FACEBOOK = "Login with Facebook";
    public static final String LOGIN_REGISTER_WITH_EMAIL = "Register with Email";
    public static final String LOGIN_LOGIN_WITH_EMAIL = "Login with Email";
    public static final String LOGIN_RETURN = "Return (on keyboard)";
    public static final String LOGIN_TAPPED_TO_HIDE_KEYBOARD = "Tapped to Hide Keyboard";
    public static final String LOGIN_REGISTER_WITH_FACEBOOK = "Register with Facebook"; // Added
    public static final String LOGIN_VIEW_APPEARED = "Login";
    public static final String ONBOARDING_VIEW_APPEARED = "Onboarding"; // Added
    public static final String REGISTER_BUTTON_TAPPED = "Register";
    public static final String REGISTER_VIEW_APPEARED = "Register";
    public static final String CONFIRM_INFORMATION_VIEW_APPEARED = "Confirm Information";
    public static final String CONFIRM_INFORMATION_CONTINUE = "Continue";
    public static final String FORGOT_PASSWORD_CANCEL = "Cancel Forgot Password";
    public static final String FORGOT_PASSWORD_CONFIRM = "Confirm Forgot Password";

//    Tutorial/Onboarding view
    public static final String TUTORIAL_VIEW_APPEARED = "Tutorial Onboarding";
    public static final String TUTORIAL_GET_STARTED_TAPPED = "Get Started";

//    Add Car View / Activity
    public static final String ADD_CAR_SCAN_VIN_BARCODE = "Scan VIN Barcode";
    public static final String ADD_CAR_BACK = "Back";
    public static final String ADD_CAR_YES_HARDWARE = "Yes I have Pitstop Hardware";
    public static final String ADD_CAR_NO_HARDWARE = "No I do not have Pitstop Hardware";
    public static final String ADD_CAR_ADD_CAR_TAPPED = "Add Car";
    public static final String ADD_CAR_VIEW_APPEARED = "Add Car";
    public static final String ADD_CAR_BARCODE_SCANNER_VIEW_APPEARED = "Barcode Scanner";
    public static final String ADD_CAR_SELECT_DEALERSHIP_VIEW_APPEARED = "Select Dealership";
    public static final String ADD_CAR_CAR_EXIST_FOR_CURRENT_USER = "Car already exists for user";
    public static final String ADD_CAR_CAR_EXIST_FOR_ANOTHER_USER = "Car already exists for another user";
    public static final String ADD_CAR_BLUETOOTH_RETRY = "Try to connect bluetooth again";
    public static final String ADD_CAR_TRY_GET_VIN_AGAIN = "Try to get VIN again";
    public static final String ADD_CAR_GET_SET_RTC_AGAIN = "Try to get and set RTC";
    public static final String ADD_CAR_SCANNER_EXISTS_IN_BACKEND = "Scanner Exists in Backend";

//    Dashboard
    public static final String DASHBOARD_ALERT_ADD_VIN = "Add VIN through Alert";
    public static final String DASHBOARD_ALERT_ADD_NEW_CAR = "Add New Car through Alert";
    public static final String DASHBOARD_ALERT_CANCEL = "Cancel Adding Car through Alert";

//    Scan View / Activity
    public static final String SCAN_CAR_CONFIRM_SCAN = "Confirm Scan";
    public static final String SCAN_CAR_RETRY_SCAN = "Retry Scan (Vehicle not connected)";
    public static final String SCAN_CAR_CANCEL_SCAN = "Cancel (Vehicle not connected)";
    public static final String SCAN_CAR_ALLOW_BLUETOOTH_ON = "Settings (Bluetooth not on)";
    public static final String SCAN_CAR_DENY_BLUETOOTH_ON = "Cancel (Bluetooth not on)";


    public MixpanelHelper(GlobalApplication context) {
        application = context;
    }

    public void trackAppStatus(String value) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        JSONObject json = new JSONObject();
        json.put("Status", value);
//        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
//        application.getMixpanelAPI().track("App Status", json);
        application.getMixpanelAPI().track(EVENT_APP_STATUS, json);
    }

    public void trackViewAppeared(String value) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
//        JSONObject json = new JSONObject("{'View':'" + value + "','Device':'Android'}");
        JSONObject json = new JSONObject();
        json.put("View", value);
//        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
//        application.getMixpanelAPI().track("View Appeared", json);
        application.getMixpanelAPI().track(EVENT_VIEW_APPEARED, json);
    }

    public void trackConnectionStatus(String value) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
//        JSONObject json = new JSONObject("{'Status':'" + value + "','Device':'Android'}");
        JSONObject json = new JSONObject();
        json.put("Status", value);
//        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
//        application.getMixpanelAPI().track("Peripheral Connection Status", json);
        application.getMixpanelAPI().track(EVENT_PERIPHERAL_CONNECTION_STATUS, json);
    }

    public void trackButtonTapped(String value, String view) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
//        JSONObject json = new JSONObject("{'Button':'" + value + "','View':'" + view + "','Device':'Android'}");
        JSONObject json = new JSONObject();
        json.put("Button", value);
        json.put("View", view);
//        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
//        application.getMixpanelAPI().track("Button Tapped", json);
        application.getMixpanelAPI().track(EVENT_BUTTON_TAPPED, json);
    }

    public void trackScrolledInView(String view) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
//        JSONObject json = new JSONObject("{'View':'" + view + "','Device':'Android'}");
        JSONObject json = new JSONObject();
        json.put("View", view);
//        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
//        application.getMixpanelAPI().track("Scrolled in View", json);
        application.getMixpanelAPI().track(EVENT_SCROLLED_IN_VIEW, json);
    }

    public void trackCarAdded(String view, String mileage, String method) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
//        JSONObject json = new JSONObject("{'Button':'Add Car','View':'" + view + "','Mileage':'" + mileage
//                + "','Method of Adding Car':'" + method + "','Device':'Android'}");
        JSONObject json = new JSONObject();
        json.put("Button", "Add Car");
        json.put("View", view);
        json.put("Mileage", mileage);
        json.put("Method of Adding Car", method);
//        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
//        application.getMixpanelAPI().track("Button Tapped", json);
        application.getMixpanelAPI().track(EVENT_BUTTON_TAPPED, json);
    }

    public void trackMigrationProgress(String status, int userId) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
//        JSONObject json = new JSONObject("{'Status':'" + status + "','UserId':'" + userId + "','Device':'Android'}");
        JSONObject json = new JSONObject();
        json.put("Status", status);
        json.put("UserId", userId);
//        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track("Migration Status", json);
    }

    public void trackCustom(String event, JSONObject properties) throws JSONException{
        if(BuildConfig.DEBUG) {
            return;
        }
//        properties.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            properties.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track(event, properties);
    }

}
