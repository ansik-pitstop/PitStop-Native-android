package com.pitstop.application;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.stetho.Stetho;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.database.LocalAppointmentStorage;
import com.pitstop.database.LocalCarIssueStorage;
import com.pitstop.database.LocalCarStorage;
import com.pitstop.database.LocalDeviceTripStorage;
import com.pitstop.database.LocalPidStorage;
import com.pitstop.database.LocalScannerStorage;
import com.pitstop.database.LocalShopStorage;
import com.pitstop.database.LocalTripStorage;
import com.pitstop.database.LocalUserStorage;
import com.pitstop.models.Car;
import com.pitstop.models.Notification;
import com.pitstop.models.User;
import com.pitstop.utils.PreferenceKeys;
import com.pitstop.utils.SecretUtils;

import org.acra.ACRA;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;

import java.util.List;

import io.fabric.sdk.android.Fabric;
import io.smooch.core.Settings;
import io.smooch.core.Smooch;

/**
 * Created by Ansik on 12/28/15.
 */
public class GlobalApplication extends Application {

    private static String TAG = GlobalApplication.class.getSimpleName();

    public static final String PARSE_CLIENT_KEY = "android";

    private MixpanelAPI mixpanelAPI;

    private ActivityLifecycleObserver activityLifecycleObserver;

    /**
     * Database open helper
     */
    private LocalUserStorage mLocalUserStorage;
    private LocalScannerStorage mLocalScannerStorage;
    private LocalCarStorage mLocalCarStorage;
    private LocalCarIssueStorage mLocalCarIssueStorage;
    private LocalAppointmentStorage mLocalAppointmentStorage;
    private LocalTripStorage mLocalTripStorage;
    private LocalPidStorage mLocalPidStorage;
    private LocalShopStorage mLocalShopStorage;
    private LocalDeviceTripStorage mLocalDeviceTripStorage;

    // Build a RemoteInput for receiving voice input in a Car Notification
    public static RemoteInput remoteInput = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        if (BuildConfig.DEBUG) {
            try {
                final ACRAConfiguration config = new ConfigurationBuilder(this)
                        .setAlsoReportToAndroidFramework(true)
                        .setMailTo("developers@getpitstop.io")
                        .build();
                ACRA.init(this, config);
            } catch (ACRAConfigurationException e) {
                e.printStackTrace();
                ACRA.init(this);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        Stetho.initializeWithDefaults(this);

        //Begin Crashlytics
        Fabric.with(this, new Crashlytics());

        if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)){
            Log.d(TAG,"Release build.");
            Crashlytics.setString(BuildConfig.VERSION_NAME,"Release");
        }
        else if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA)){
            Log.d(TAG,"Beta build.");
            Crashlytics.setString(BuildConfig.VERSION_NAME,"Beta");
        }

        MultiDex.install(this);

        initiateDatabase();

        // Smooch
        Settings settings = new Settings(SecretUtils.getSmoochToken(this));

        settings.setFirebaseCloudMessagingAutoRegistrationEnabled(true);
        Smooch.init(this, settings, null);

        // Parse
        ParseObject.registerSubclass(Notification.class);
        Parse.enableLocalDatastore(this);
        FacebookSdk.sdkInitialize(this);
        if(BuildConfig.DEBUG) {
            Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);
        } else {
            Parse.setLogLevel(Parse.LOG_LEVEL_NONE);
        }

        Parse.initialize(new Parse.Configuration.Builder(getApplicationContext())
                .applicationId(SecretUtils.getParseAppId(this))
                .clientKey(PARSE_CLIENT_KEY)
                .enableLocalDataStore()
                .server(getString(R.string.parse_server_url))
                .build()
        );

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

        // MixPanel
        mixpanelAPI = getMixpanelAPI();

        activityLifecycleObserver = new ActivityLifecycleObserver(this);
        registerActivityLifecycleCallbacks(activityLifecycleObserver);

    }

    public void setUpMixPanel(){
        User user = mLocalUserStorage.getUser();
        if(user != null) {
            Log.d(TAG, "Setting up mixpanel");
            mixpanelAPI.identify(String.valueOf(user.getId()));
            mixpanelAPI.getPeople().identify(String.valueOf(user.getId()));
            mixpanelAPI.getPeople().set("$phone", user.getPhone());
            mixpanelAPI.getPeople().set("$name", user.getFirstName() + (user.getLastName() == null ? "" : " " + user.getLastName()));
            mixpanelAPI.getPeople().set("$email", user.getEmail());
        } else {
            Log.d(TAG, "Can't set up mixpanel; current user is null");
        }
    }

    public MixpanelAPI getMixpanelAPI() {
        if(mixpanelAPI == null) {
            mixpanelAPI = MixpanelAPI.getInstance(this, SecretUtils.getMixpanelToken(this));
        }
        return mixpanelAPI;
    }

    public enum AppStart {
        FIRST_TIME, FIRST_TIME_VERSION, NORMAL
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

        Log.d(TAG,"logInUser()");

        SharedPreferences settings = getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(PreferenceKeys.KEY_ACCESS_TOKEN, accessToken);
        editor.putString(PreferenceKeys.KEY_REFRESH_TOKEN, refreshToken);
        editor.putBoolean(PreferenceKeys.KEY_LOGGED_IN, true);
        editor.apply();

        ParseUser.logOut();

        //Login to smooch with userId
        int userId = currentUser.getId();
        if (userId != -1){
            Smooch.login(String.valueOf(userId), "12345", response
                    -> Log.d(TAG,"Smooch.login() result err: "+response.getError()));
        }

        setCurrentUser(currentUser);
    }

    public int getCurrentUserId() {
        SharedPreferences settings =
                getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);

        return settings.getInt(PreferenceKeys.KEY_USER_ID, -1);
    }

    public User getCurrentUser() {
        return mLocalUserStorage.getUser();
    }

    public Car getCurrentCar(){

        //Get most recent version of car list
        List<Car> carList = mLocalCarStorage.getAllCars();

        //Set car list to what it was initially
        if (carList.size() == 0)
            return null;

        for (Car c: carList){
            if (c.isCurrentCar())
                return c;
        }

        return carList.get(0);
    }

    public boolean isLoggedIn() {
        SharedPreferences settings = getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);
        return settings.getBoolean(PreferenceKeys.KEY_LOGGED_IN, false);
    }

    public void setCurrentUser(User user) {
        Log.i(TAG, "UserId:"+user.getId());
        SharedPreferences settings = getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PreferenceKeys.KEY_USER_ID, user.getId());
        editor.apply();

        mLocalUserStorage.storeUserData(user);
    }

    public void setTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor prefEditor = getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE).edit();

        prefEditor.putString(PreferenceKeys.KEY_ACCESS_TOKEN, accessToken);
        prefEditor.putString(PreferenceKeys.KEY_REFRESH_TOKEN, refreshToken);

        prefEditor.apply();
    }

    public String getAccessToken() {
        SharedPreferences settings = getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);
        return settings.getString(PreferenceKeys.KEY_ACCESS_TOKEN, "");
    }

    public String getRefreshToken() {
        SharedPreferences settings = getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);
        return settings.getString(PreferenceKeys.KEY_REFRESH_TOKEN, "");
    }

    public void logOutUser() {
        Log.i(TAG, "Logging user out");
        SharedPreferences settings = getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt(PreferenceKeys.KEY_USER_ID, -1);
        editor.putString(PreferenceKeys.KEY_ACCESS_TOKEN, null);
        editor.putString(PreferenceKeys.KEY_REFRESH_TOKEN, null);
        editor.putBoolean(PreferenceKeys.KEY_LOGGED_IN, false);
        editor.apply();

        ParseUser.logOut();

        AccessToken.setCurrentAccessToken(null);

        // Logout from Smooch for the next login
        Smooch.logout(response -> Log.d(TAG,"smooch logout err:  "+response.getError()));

        cleanUpDatabase();
    }

    public void modifyMixpanelSettings(String field, Object value){
        getMixpanelAPI().getPeople().set(field, value);
    }

    /**
     * Initiate database open helper when the app start
     */
    private void initiateDatabase(){
        mLocalUserStorage = new LocalUserStorage(this);
        mLocalScannerStorage = new LocalScannerStorage(this);
        mLocalCarStorage = new LocalCarStorage(this);
        mLocalAppointmentStorage = new LocalAppointmentStorage(this);
        mLocalTripStorage = new LocalTripStorage(this);
        mLocalCarIssueStorage = new LocalCarIssueStorage(this);
        mLocalPidStorage = new LocalPidStorage(this);
        mLocalShopStorage = new LocalShopStorage(this);
        mLocalDeviceTripStorage = new LocalDeviceTripStorage(this);
    }

    /**
     * Delete all rows in database
     */
    private void cleanUpDatabase(){
        mLocalUserStorage.deleteAllUsers();
        mLocalScannerStorage.deleteAllRows();
        mLocalPidStorage.deleteAllRows();
        mLocalCarStorage.deleteAllRows();
        mLocalAppointmentStorage.deleteAllRows();
        mLocalTripStorage.deleteAllRows();
        mLocalCarIssueStorage.deleteAllRows();
        mLocalShopStorage.deleteAllRows();
        mLocalDeviceTripStorage.deleteAllRows();
    }

}
