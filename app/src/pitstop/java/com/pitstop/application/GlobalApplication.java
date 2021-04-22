package com.pitstop.application;

import android.app.Application;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.RemoteInput;
import androidx.multidex.MultiDex;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.stetho.Stetho;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.bluetooth.BluetoothService;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalUserStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.other.SendPendingUpdatesUseCase;
import com.pitstop.models.Notification;
import com.pitstop.models.PendingUpdate;
import com.pitstop.models.User;
import com.pitstop.network.RequestError;
import com.pitstop.ui.login.LoginActivity;
import com.pitstop.ui.trip.TripsService;
import com.pitstop.utils.Logger;
import com.pitstop.utils.LoginManager;
import com.pitstop.utils.NotificationsHelper;
import com.pitstop.utils.PreferenceKeys;
import com.pitstop.utils.SecretUtils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Emitter;
import io.reactivex.Observable;
import io.smooch.core.Settings;
import io.smooch.core.Smooch;

/**
 * Created by Ansik on 12/28/15.
 */
public class GlobalApplication extends Application implements LoginManager {

    private static String TAG = GlobalApplication.class.getSimpleName();

    public static final String PARSE_CLIENT_KEY = "android";

    private MixpanelAPI mixpanelAPI;

    private ActivityLifecycleObserver activityLifecycleObserver;

    private UseCaseComponent useCaseComponent;
    private Observable<Service> serviceObservable;
    private BluetoothService autoConnectService;
    private TripsService tripsService;
    private ServiceConnection serviceConnection;

    // Build a RemoteInput for receiving voice input in a Car Notification
    public static RemoteInput remoteInput = null;
    private boolean isBluetoothServiceRunning = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        MultiDex.install(this);

    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        FacebookSdk.sdkInitialize(this);
        Stetho.initializeWithDefaults(this);

//        Crashlytics crashlyticsKit = new Crashlytics.Builder()
//                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
//                .build();
//
//        Fabric.with(this, crashlyticsKit);

        if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)){
            Log.d(TAG,"Release build.");
//            crashlyticsKit.setString(BuildConfig.VERSION_NAME,"Release");
        }
        else if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA)){
            Log.d(TAG,"Beta build.");
//            crashlyticsKit.setString(BuildConfig.VERSION_NAME,"Beta");
        }

        Logger.initLogger(this);

        NotificationsHelper.createNotificationChannels(this);

        LocalUserStorage localUserStorage = new LocalUserStorage(LocalDatabaseHelper.getInstance(this));

        // Smooch
        Log.d(TAG,"Smooch app id: "+SecretUtils.getSmoochToken(this));
        Settings settings = new Settings(SecretUtils.getSmoochToken(this)); //ID must be upper case
        settings.setFirebaseCloudMessagingAutoRegistrationEnabled(true);

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(this)).build();
        Smooch.init(this, settings, response -> {
            Log.d(TAG,"Smooch: init response: "+response.getError());
        });

        // Parse
        ParseObject.registerSubclass(Notification.class);
        Parse.enableLocalDatastore(this);
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

        ParseInstallation.getCurrentInstallation().saveInBackground(e -> {
            if(e == null) {
                Log.d(TAG, "Installation saved");
            } else {
                Log.w(TAG, "Error saving installation: " + e.getMessage());
            }
        });

//         MixPanel
        mixpanelAPI = getMixpanelAPI();
        mixpanelAPI.getPeople().initPushHandling(SecretUtils.getGoogleSenderId());
        User user = localUserStorage.getUser();
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
        Log.d(TAG,"google sender id: "+SecretUtils.getGoogleSenderId());

        activityLifecycleObserver = new ActivityLifecycleObserver(this);
        registerActivityLifecycleCallbacks(activityLifecycleObserver);

        tripsService = null;
        autoConnectService = null;
        List<Emitter<Service>> emitterList = new ArrayList<>();
        serviceObservable = Observable.create(emitter -> {
            Log.d(TAG,"serviceObservable.subscribe() autoconnectService null? "
                    +(autoConnectService == null) +", tripsService null? "+(tripsService == null));
            emitterList.add(emitter);
            if (autoConnectService != null){
                emitter.onNext(autoConnectService);
            }
            if (tripsService != null){
                emitter.onNext(tripsService);
            }

            if (serviceConnection == null){
                serviceConnection = new ServiceConnection() {

                    @Override
                    public void onServiceConnected(ComponentName className, IBinder service) {
                        Log.i(TAG, String.format("connecting: onServiceConnection, className: %s, trips class: %s"
                                ,className.getClassName(),TripsService.class.getCanonicalName()));
                        if (className.getClassName().equals(BluetoothService.class.getCanonicalName())){
                            autoConnectService = ((BluetoothService.BluetoothBinder)service).getService();
                            isBluetoothServiceRunning = true;
                            for (Emitter e: emitterList){
                                e.onNext(autoConnectService);
                            }
                            Log.d(TAG,"bluetooth service set");
                        }
                        else if (className.getClassName().equals(TripsService.class.getName())) {
                            Log.d(TAG, "trips service set");
                            try {
                                tripsService = ((TripsService.TripsBinder) service).getService();
                                for (Emitter e: emitterList){
                                    e.onNext(tripsService);
                                }
                                Log.d(TAG,"passed trips service to emitter");
                            } catch (ClassCastException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName arg0) {
                        Log.i(TAG, "Disconnecting: onServiceConnection componentName.className: "
                                +arg0.getClassName());
                        if (arg0.getClassName().equals(BluetoothService.class.getCanonicalName())){
                            autoConnectService = null;
                        }else if (arg0.getClassName().equals(TripsService.class.getName())){
                            tripsService = null;
                        }
                    }
                };

                //Begin sending pending updates when connectivity intent is received
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
                BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context1, Intent intent) {
                        try {
                            Log.d(TAG,"Received broadcast!");
                            ConnectivityManager conn = (ConnectivityManager)
                                    context1.getSystemService(Context.CONNECTIVITY_SERVICE);
                            if (conn != null && conn.getActiveNetworkInfo() != null) {
                                useCaseComponent.sendPendingUpdatesUseCase().execute(new SendPendingUpdatesUseCase.Callback() {
                                    @Override
                                    public void updatesSent(@NotNull List<PendingUpdate> pendingUpdates) {
                                        Log.d(TAG,"updatesSent() pendingUpdates: "+pendingUpdates);
                                    }

                                    @Override
                                    public void errorSending(@NotNull RequestError err) {
                                        Log.d(TAG,"errorSending() err: "+err);
                                    }
                                });
                            }
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                };
                registerReceiver(broadcastReceiver,intentFilter);

                startBluetoothService();

                Intent tripsServiceIntent = new Intent(GlobalApplication.this
                        , TripsService.class);
                startService(tripsServiceIntent);
                bindService(tripsServiceIntent, serviceConnection, BIND_AUTO_CREATE);
            }
        });
    }

    public boolean isBluetoothServiceRunning(){
        return isBluetoothServiceRunning;
    }

    public void startBluetoothService(){
        Log.d(TAG,"startBluetoothService()");
        if (!isBluetoothServiceRunning && serviceConnection != null){
            Intent serviceIntent = new Intent(GlobalApplication.this
                    , BluetoothService.class);
            startService(serviceIntent);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            isBluetoothServiceRunning = true;

        }
    }

    public void stopBluetoothService(){
        Log.d(TAG,"stopBluetoothService()");
        if (autoConnectService != null){
            isBluetoothServiceRunning = false;
            autoConnectService.stopSelf();
        }
    }

    public Observable<Service> getServices(){
        return serviceObservable;
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

    @Override
    public void loginUser(String accessToken, String refreshToken, User currentUser) {

        Log.d(TAG, "logInUser() user: " + currentUser);

        SharedPreferences settings = getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(PreferenceKeys.KEY_ACCESS_TOKEN, accessToken);
        editor.putString(PreferenceKeys.KEY_REFRESH_TOKEN, refreshToken);
        editor.putBoolean(PreferenceKeys.KEY_LOGGED_IN, true);
        editor.putInt(PreferenceKeys.KEY_USER_ID, currentUser.getId());
        editor.apply();

        ParseUser.logOut();

        //Login to smooch with userId
        Settings smoochSettings = new Settings(SecretUtils.getSmoochToken(this)); //ID must be upper case
        smoochSettings.setFirebaseCloudMessagingAutoRegistrationEnabled(true);
        Smooch.init(this, smoochSettings, response -> {
            Log.d(TAG,"Smooch: init response: "+response.getError());
        });
    }

    public int getCurrentUserId() {
        SharedPreferences settings =
                getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);

        return settings.getInt(PreferenceKeys.KEY_USER_ID, -1);
    }

    @Override
    public boolean isLoggedIn() {
        SharedPreferences settings = getSharedPreferences(PreferenceKeys.NAME_CREDENTIALS, MODE_PRIVATE);
        return settings.getBoolean(PreferenceKeys.KEY_LOGGED_IN, false);
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

        if (AccessToken.getCurrentAccessToken() != null){
            com.facebook.login.LoginManager.getInstance().logOut();
            AccessToken.setCurrentAccessToken(null);
        }


        // Logout from Smooch for the next login
        Smooch.logout(response -> {
            Log.d(TAG,"smooch logout response: "+response.getError());
        });

        LocalDatabaseHelper.getInstance(this).deleteAllData();
        Intent intent = new Intent(this,LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void modifyMixpanelSettings(String field, Object value){
        getMixpanelAPI().getPeople().set(field, value);
    }
}
