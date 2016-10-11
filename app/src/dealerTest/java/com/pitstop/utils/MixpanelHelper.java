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

    public static final String DISCONNECTED = "Disconnected from Bluetooth";
    public static final String CONNECTED = "Connected to Bluetooth";

    public MixpanelHelper(GlobalApplication context) {

    }

    public void trackConnectionStatus(String value) throws JSONException {
    }

    public void trackButtonTapped(String value, String view) throws JSONException {
    }

    public void trackCustom(String event, JSONObject properties) throws JSONException{
    }

}
