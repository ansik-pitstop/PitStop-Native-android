package com.pitstop.utils;

import com.pitstop.BuildConfig;
import com.pitstop.model.User;
import com.pitstop.application.GlobalApplication;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Ben Wu on 2016-05-13.
 */
public class MixpanelHelper {

    private GlobalApplication application;

    public static final String APP_LAUNCHED = "Launched";
    public static final String APP_LAUNCHED_FROM_PUSH = "Launched from Push";
    public static final String APP_ENTERED_BACKGROUND = "Entered Background";
    public static final String APP_ENTERED_FOREGROUND = "Entered Foreground";
    public static final String APP_TERMINATE = "Will Terminate";

    public static final String DISCONNECTED = "Disconnected from Bluetooth";
    public static final String CONNECTED = "Connected to Bluetooth";

    public static final String ADDED_MANUALLY = "Car Added Manually";
    public static final String ADDED_WITH_DEVICE = "Car Added Through Device";

    public MixpanelHelper(GlobalApplication context) {
        application = context;
    }

    public void trackAppStatus(String value) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        JSONObject json = new JSONObject();
        json.put("Status", value);
        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track("App Status", json);
    }

    public void trackViewAppeared(String value) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        JSONObject json = new JSONObject("{'View':'" + value + "','Device':'Android'}");
        json.put("View", value);
        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track("View Appeared", json);
    }

    public void trackConnectionStatus(String value) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        JSONObject json = new JSONObject("{'Status':'" + value + "','Device':'Android'}");
        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track("Peripheral Connection Status", json);
    }

    public void trackButtonTapped(String value, String view) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        JSONObject json = new JSONObject("{'Button':'" + value + "','View':'" + view + "','Device':'Android'}");
        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track("Button Tapped", json);
    }

    public void trackScrolledInView(String view) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        JSONObject json = new JSONObject("{'View':'" + view + "','Device':'Android'}");
        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track("Scrolled in View", json);
    }

    public void trackCarAdded(String view, String mileage, String method) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        JSONObject json = new JSONObject("{'Button':'Add Car','View':'" + view + "','Mileage':'" + mileage
                + "','Method of Adding Car':'" + method + "','Device':'Android'}");
        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track("Button Tapped", json);
    }

    public void trackMigrationProgress(String status, int userId) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        JSONObject json = new JSONObject("{'Status':'" + status + "','UserId':'" + userId + "','Device':'Android'}");
        json.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            json.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track("Migration status", json);
    }

    public void trackCustom(String event, JSONObject properties) throws JSONException{
        if(BuildConfig.DEBUG) {
            return;
        }
        properties.put("Device", "Android");
        User user = application.getCurrentUser();
        if(application.getCurrentUser() != null) {
            properties.put("Username", user.getEmail());
        }
        application.getMixpanelAPI().track(event, properties);
    }
}
