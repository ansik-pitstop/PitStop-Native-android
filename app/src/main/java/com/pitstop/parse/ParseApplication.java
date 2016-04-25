package com.pitstop.parse;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.provider.SyncStateContract;
import android.util.Log;

import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.pitstop.MainActivity;
import com.pitstop.R;

import io.smooch.core.Smooch;

/**
 * Created by Ansik on 12/28/15.
 */
public class ParseApplication extends Application {

    private static String TAG = "ParseApplication";

    private static MixpanelAPI mixpanelAPI;
    @Override
    public void onCreate() {
        super.onCreate();

        // ParseCrashReporting.enable(this);
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.parse_appID),
                getString(R.string.parse_clientID));
        ParseInstallation.getCurrentInstallation().saveInBackground();
        Smooch.init(this, getString(R.string.smooch_token));
        mixpanelAPI = MixpanelAPI.getInstance(this, getString(R.string.prod_mixpanel_api_token));
    }

    public static void setUpMixPanel(){
        if(ParseUser.getCurrentUser()!=null) {
            mixpanelAPI.identify(ParseUser.getCurrentUser().getObjectId());
            mixpanelAPI.getPeople().identify(ParseUser.getCurrentUser().getObjectId());
            mixpanelAPI.getPeople().set("$phone", ParseUser.getCurrentUser().get("phoneNumber"));
            mixpanelAPI.getPeople().set("$name", ParseUser.getCurrentUser().get("name"));
            mixpanelAPI.getPeople().set("$email", ParseUser.getCurrentUser().getEmail());
        }
    }

    public MixpanelAPI getMixpanelAPI() {
        if(mixpanelAPI == null) {
            mixpanelAPI = MixpanelAPI.getInstance(this, getString(R.string.prod_mixpanel_api_token));
        }
        return mixpanelAPI;
    }

    public enum AppStart {
        FIRST_TIME, FIRST_TIME_VERSION, NORMAL;
    }

    /**
     * The app version code (not the version name!) that was used on the last
     * start of the app.
     */
    private static final String LAST_APP_VERSION = "com.pitstop.last_app_version";

    public AppStart checkAppStart() {
        PackageInfo pInfo;
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        AppStart appStart = AppStart.NORMAL;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int lastVersionCode = sharedPreferences
                    .getInt(LAST_APP_VERSION, -1);
            int currentVersionCode = pInfo.versionCode;
            appStart = checkAppStart(currentVersionCode, lastVersionCode);
            // Update version in preferences
            sharedPreferences.edit()
                    .putInt(LAST_APP_VERSION, currentVersionCode).apply();
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG,
                    "Unable to determine current app version from package manager." +
                            " Defensively assuming normal app start.");
        }
        return appStart;
    }


    public AppStart checkAppStart(int currentVersionCode, int lastVersionCode) {
        if (lastVersionCode == -1) {
            return AppStart.FIRST_TIME;
        } else if (lastVersionCode < currentVersionCode) {
            return AppStart.FIRST_TIME_VERSION;
        } else if (lastVersionCode > currentVersionCode) {
            Log.i(TAG, "Current version code (" + currentVersionCode
                    + ") is less then the one recognized on last startup ("
                    + lastVersionCode
                    + "). Defensively assuming normal app start.");
            return AppStart.NORMAL;
        } else {
            return AppStart.NORMAL;
        }
    }

}
