package com.pitstop.utils;

import com.pitstop.BuildConfig;
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
        application.getMixpanelAPI().track("App Status",
                new JSONObject("{'Status':'" + value + "','Device':'Android'}"));
    }

    public void trackViewAppeared(String value) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        application.getMixpanelAPI().track("View Appeared",
                new JSONObject("{'View':'" + value + "','Device':'Android'}"));
    }

    public void trackConnectionStatus(String value) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        application.getMixpanelAPI().track("Peripheral Connection Status",
                new JSONObject("{'Status':'" + value + "','Device':'Android'}"));
    }

    public void trackButtonTapped(String value, String view) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        application.getMixpanelAPI().track("Button Tapped",
                new JSONObject("{'Button':'" + value + "','View':'" + view + "','Device':'Android'}"));
    }

    public void trackScrolledInView(String view) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        application.getMixpanelAPI().track("Scrolled in View",
                new JSONObject("{'View':'" + view + "','Device':'Android'}"));
    }

    public void trackCarAdded(String view, String mileage, String method) throws JSONException {
        if(BuildConfig.DEBUG) {
            return;
        }
        application.getMixpanelAPI().track("Button Tapped",
                new JSONObject("{'Button':'Add Car','View':'" + view + "','Mileage':'" + mileage
                        + "','Method of Adding Car':'" + method + "','Device':'Android'}"));
    }
}
