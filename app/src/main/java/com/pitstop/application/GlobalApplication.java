package com.pitstop.application;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pitstop.BuildConfig;
import com.pitstop.DataAccessLayer.DTOs.User;
import com.pitstop.DataAccessLayer.DataAdapters.UserAdapter;
import com.pitstop.R;

import java.io.File;
import java.io.IOException;

import io.smooch.core.Settings;
import io.smooch.core.Smooch;

/**
 * Created by Ansik on 12/28/15.
 */
public class GlobalApplication extends Application {

    private static String TAG = GlobalApplication.class.getSimpleName();

    public final static String pfName = "com.pitstop.credentials";
    public final static String pfUserName = "com.pitstop.user_name";
    public final static String pfPassword = "com.pitstop.password";
    public final static String pfUserId = "com.pitstop.user_id";
    public final static String pfAccessToken = "com.pitstop.access";
    public final static String pfRefreshToken = "com.pitstop.refresh";
    public final static String pfLoggedIn = "com.pitstop.logged_in";

    private static MixpanelAPI mixpanelAPI;

    private UserAdapter userAdapter;

    // Build a RemoteInput for receiving voice input in a Car Notification
    public static RemoteInput remoteInput = null;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        MultiDex.install(this);

        userAdapter = new UserAdapter(this);

        // Smooch
        Settings settings = new Settings(getString(R.string.smooch_token));
        settings.setGoogleCloudMessagingAutoRegistrationEnabled(false);
        Smooch.init(this, settings);

        // Parse
        Parse.enableLocalDatastore(this);
        FacebookSdk.sdkInitialize(this);
        if(BuildConfig.DEBUG) {
            Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);
        } else {
            Parse.setLogLevel(Parse.LOG_LEVEL_NONE);
        }

        Parse.initialize(getApplicationContext(), BuildConfig.BUILD_TYPE.equals(BuildConfig.RELEASE_TYPE) ?
                        getString(R.string.parse_appID_prod) : getString(R.string.parse_appID_dev),

                BuildConfig.BUILD_TYPE.equals(BuildConfig.RELEASE_TYPE) ?
                        getString(R.string.parse_clientID_prod) : getString(R.string.parse_clientID_dev));

        ParseInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null) {
                    Log.d(TAG, "Installation saved");
                } else {
                    Log.w(TAG, "Error saving installation: " + e.getMessage());
                }
            }
        });

        // Mixpanel
        mixpanelAPI = MixpanelAPI.getInstance(this, BuildConfig.DEBUG ? "butt" : getString(R.string.prod_mixpanel_api_token));
    }

    public void setUpMixPanel(){
        User user = userAdapter.getUser();
        if(user != null) {
            Log.d(TAG, "Setting up mixpanel");
            mixpanelAPI.identify(String.valueOf(user.getId()));
            mixpanelAPI.getPeople().identify(String.valueOf(user.getId()));
            mixpanelAPI.getPeople().set("$phone", user.getPhone());
            mixpanelAPI.getPeople().set("$name", user.getFirstName());
            mixpanelAPI.getPeople().set("$email", user.getEmail());
        } else {
            Log.d(TAG, "Can't set up mixpanel; current user is null");
        }
    }

    public MixpanelAPI getMixpanelAPI() {
        if(mixpanelAPI == null) {
            mixpanelAPI = MixpanelAPI.getInstance(this, BuildConfig.DEBUG ? "grpogrjer" : getString(R.string.prod_mixpanel_api_token));
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

    public void logInUser(String accessToken, String refreshToken, User currentUser) {
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(pfAccessToken, accessToken);
        editor.putString(pfRefreshToken, refreshToken);
        editor.putBoolean(pfLoggedIn, true);
        editor.apply();

        ParseUser.logOut();

        setCurrentUser(currentUser);
    }

    public int getCurrentUserId() {
        SharedPreferences settings =
                getSharedPreferences(GlobalApplication.pfName, MODE_PRIVATE);

        return settings.getInt(GlobalApplication.pfUserId, -1);
    }

    public User getCurrentUser() {
        return userAdapter.getUser();
    }

    public boolean isLoggedIn() {
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        return settings.getBoolean(pfLoggedIn, false);
    }

    public void setCurrentUser(User user) {
        Log.i(TAG, "UserId:"+user.getId());
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(pfUserId, user.getId());
        editor.apply();

        userAdapter.storeUserData(user);
    }

    public void setTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor prefEditor = getSharedPreferences(pfName, MODE_PRIVATE).edit();

        prefEditor.putString(pfAccessToken, accessToken);
        prefEditor.putString(pfRefreshToken, refreshToken);

        prefEditor.apply();
    }

    public String getAccessToken() {
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        return settings.getString(pfAccessToken, "");
    }

    public String getRefreshToken() {
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        return settings.getString(pfRefreshToken, "");
    }

    public void logOutUser() {
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(pfUserName, null);
        editor.putString(pfPassword, null);
        editor.putInt(pfUserId, -1);
        editor.putString(pfAccessToken, null);
        editor.putString(pfRefreshToken, null);
        editor.putBoolean(pfLoggedIn, false);
        editor.apply();

        ParseUser.logOut();

        AccessToken.setCurrentAccessToken(null);

        userAdapter.deleteAllUsers();
    }

}
