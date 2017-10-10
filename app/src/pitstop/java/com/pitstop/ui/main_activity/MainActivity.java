package com.pitstop.ui.main_activity;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.ParseACL;
import com.parse.ParseInstallation;
import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.database.LocalCarStorage;
import com.pitstop.database.LocalScannerStorage;
import com.pitstop.database.LocalShopStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.check.CheckFirstCarAddedUseCase;
import com.pitstop.interactors.get.GetCarsByUserIdUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.set.SetFirstCarAddedUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.ObdScanner;
import com.pitstop.models.ReadyDevice;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.observer.Device215BreakingObserver;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.LoginActivity;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.add_car.PromptAddCarActivity;
import com.pitstop.ui.issue_detail.IssueDetailsActivity;
import com.pitstop.ui.my_appointments.MyAppointmentActivity;
import com.pitstop.ui.my_trips.MyTripsActivity;
import com.pitstop.ui.service_request.RequestServiceActivity;
import com.pitstop.ui.services.custom_service.CustomServiceActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.MigrationService;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smooch.core.Smooch;
import io.smooch.core.User;

/**
 * Created by David on 6/8/2016.
 */
public class MainActivity extends IBluetoothServiceActivity implements MainActivityCallback
        , Device215BreakingObserver, BluetoothConnectionObserver, TabSwitcher{

    public static final String TAG = MainActivity.class.getSimpleName();

    private GlobalApplication application;
    private boolean serviceIsBound = false;
    private boolean isFirstAppointment = false;
    private Intent serviceIntent;
    protected ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "connecting: onServiceConnection");
            // cast the IBinder and get MyService instance

            autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
            autoConnectService.subscribe(MainActivity.this);
            autoConnectService.requestDeviceSearch(false, false);
            displayDeviceState(autoConnectService.getDeviceState());

            checkPermissions();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "Disconnecting: onServiceConnection");
            autoConnectService = null;
        }
    };

    // Database accesses
    private LocalCarStorage carLocalStore;
    private LocalShopStorage shopLocalStore;
    private LocalScannerStorage scannerLocalStore;

    public static final int RC_ADD_CAR = 50;
    public static final int RC_SCAN_CAR = 51;
    public static final int RC_SETTINGS = 52;
    public static final int RC_DISPLAY_ISSUE = 53;
    public static final int RC_ADD_CUSTOM_ISSUE = 54;
    public static final int RC_REQUEST_SERVICE = 55;
    public static final String FROM_NOTIF = "from_notfftfttfttf";

    public static final int RC_ENABLE_BT = 102;
    public static final int RESULT_OK = 60;

    public static final String CAR_EXTRA = "car";
    public static final String CAR_ISSUE_EXTRA = "car_issue";
    public static final String CAR_LIST_EXTRA = "car_list";
    public static final String REFRESH_FROM_SERVER = "_server";
    public static final String FROM_ACTIVITY = "from_activity";
    public static final String REMOVE_TUTORIAL_EXTRA = "remove_tutorial";

    public static final int RC_LOCATION_PERM = 101;
    public static final String[] LOC_PERMS = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    // Views
    private View rootView;
    private Toolbar toolbar;

    private ProgressDialog progressDialog;
    private boolean isLoading = false;

    // Utils / Helper
    private MixpanelHelper mixpanelHelper;
    private NetworkHelper networkHelper;

    private boolean userSignedUp;
    private TabFragmentManager tabFragmentManager;

    private UseCaseComponent useCaseComponent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userSignedUp = getIntent().getBooleanExtra(LoginActivity.USER_SIGNED_UP,false);

        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(this))
                .build();
        networkHelper = tempNetworkComponent.networkHelper();

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getApplicationContext()))
                .build();

        //If user just signed up then store the user has not sent its initial smooch message
        if (userSignedUp){
            setGreetingsNotSent();
        }

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(MigrationService.notificationId);

        rootView = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(rootView);
        ParseACL acl = new ParseACL();
        acl.setPublicReadAccess(true);
        acl.setPublicWriteAccess(true);

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.setACL(acl);
        installation.put("userId", String.valueOf(application.getCurrentUserId()));
        installation.saveInBackground(e -> {
            if (e == null) {
                Log.d(TAG, "Installation saved");
            } else {
                Log.w(TAG, "Error saving installation: " + e.getMessage());
            }
        });

        serviceIntent = new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        serviceIsBound = true;

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        displayDeviceState(BluetoothConnectionObservable.State.DISCONNECTED);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        // Local db adapters
        carLocalStore = new LocalCarStorage(application);
        shopLocalStore = new LocalShopStorage(application);
        scannerLocalStore = new LocalScannerStorage(application);

        logAuthInfo();

        updateScannerLocalStore();

        tabFragmentManager = new TabFragmentManager(this,mixpanelHelper);
        tabFragmentManager.createTabs();

        FabMenu fabMenu = new FabMenu(application,this,useCaseComponent
                ,mixpanelHelper);
        fabMenu.createMenu();

    }

   // public void removeBluetoothFragmentCallback

    private void setGreetingsNotSent(){
        useCaseComponent.setFirstCarAddedUseCase().execute(false, new SetFirstCarAddedUseCase.Callback() {
            @Override
            public void onFirstCarAddedSet() {
                //Do nothing
            }

            @Override
            public void onError(RequestError error) {
                //Error logic here
            }
        });
    }

    public void changeTheme(boolean darkTheme) {
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(darkTheme ? Color.BLACK : ContextCompat.getColor(this,R.color.primary)));
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this , darkTheme ? R.color.black : R.color.primary_dark));
        }
    }

    private void loadDealerDesign(Car car){
        //Update tab design to the current dealerships custom design if applicable
        if (car.getDealership() != null){
            if (BuildConfig.DEBUG && (car.getDealership().getId() == 4
                    || car.getDealership().getId() == 18)){

                bindMercedesDealerUI();
            }else if (!BuildConfig.DEBUG && car.getDealership().getId() == 14) {
                bindMercedesDealerUI();
            }
            else{
                bindDefaultDealerUI();
            }
            hideLoading();
        }
    }

    private void displayDeviceState(String state){
        Log.d(TAG,"displayDeviceState(): "+state);

        if (getSupportActionBar() == null) return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (state.equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)){
                    getSupportActionBar().setSubtitle("Device connected");
                }
                else if(state.equals(BluetoothConnectionObservable.State.VERIFYING)){
                    getSupportActionBar().setSubtitle("Verifying device");
                }
                else if(state.equals(BluetoothConnectionObservable.State.SEARCHING)){
                    getSupportActionBar().setSubtitle("Searching for device");
                }
                else if(state.equals(BluetoothConnectionObservable.State.DISCONNECTED)){
                    getSupportActionBar().setSubtitle("Device not connected");
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume, serviceBound? "+serviceIsBound);

        checkPermissions();
        if (!serviceIsBound){
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            serviceIsBound = true;
        }
        if (autoConnectService != null){
            displayDeviceState(autoConnectService.getDeviceState());
            autoConnectService.subscribe(this);
            autoConnectService.requestDeviceSearch(false, false);
        }

        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                loadDealerDesign(car);
            }

            @Override
            public void onNoCarSet(){
                //startPromptAddCarActivity();
            }

            @Override
            public void onError(RequestError error) {

            }
        });
    }

    @Override
    protected void onStop() {
        hideLoading();

        if (autoConnectService != null){
            autoConnectService.unsubscribe(this);
        }
        if (serviceIsBound){
            unbindService(serviceConnection);
            serviceIsBound = false;
        }

        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");

        //Returned from car being added
        if (data != null && requestCode == RC_ADD_CAR) {

            if (resultCode == AddCarActivity.ADD_CAR_SUCCESS_HAS_DEALER || resultCode == AddCarActivity.ADD_CAR_SUCCESS_NO_DEALER) {
                Car addedCar = data.getParcelableExtra(CAR_EXTRA);

                updateSmoochUser(application.getCurrentUser(),addedCar);

                useCaseComponent
                        .checkFirstCarAddedUseCase()
                        .execute(new CheckFirstCarAddedUseCase.Callback() {
                            @Override
                            public void onFirstCarAddedChecked(boolean added) {
                                if (!added) {

                                    sendSignedUpSmoochMessage(application.getCurrentUser());
                                    prepareAndStartTutorialSequence();

                                    useCaseComponent.setFirstCarAddedUseCase()
                                            .execute(true, new SetFirstCarAddedUseCase.Callback() {
                                                @Override
                                                public void onFirstCarAddedSet() {
                                                    //Variable has been set
                                                }

                                                @Override
                                                public void onError(RequestError error) {
                                                    //Networking error logic here
                                                }
                                            });
                                }
                            }

                            @Override
                            public void onError(RequestError error) {
                                //Error logic here
                            }
                        });

            } else {
                mixpanelHelper.trackButtonTapped("Cancel in Add Car", "Add Car");
            }
        }
        else{
            super.onActivityResult(requestCode,resultCode,data);
        }
    }

    private void sendSignedUpSmoochMessage(com.pitstop.models.User user){
        Log.d("MainActivity Smooch", "Sending message");
        Smooch.getConversation().sendMessage(new io.smooch.core.Message(user.getFirstName() +
                (user.getLastName() == null || user.getLastName().equals("null") ?
                        "" : (" " + user.getLastName())) + " has signed up for Pitstop!"));
    }

    private void updateSmoochUser(com.pitstop.models.User user, Car car){
        if (car == null || user == null) return;

        final HashMap<String, Object> customProperties = new HashMap<>();
        customProperties.put("VIN", car.getVin());
        Log.d(TAG, car.getVin());
        customProperties.put("Car Make", car.getMake());
        Log.d(TAG, car.getMake());
        customProperties.put("Car Model", car.getModel());
        Log.d(TAG, car.getModel());
        customProperties.put("Car Year", car.getYear());
        Log.d(TAG, String.valueOf(car.getYear()));

        //Add custom user properties
        if (car.getDealership() != null) {
            customProperties.put("Email", car.getDealership().getEmail());
            Log.d(TAG, car.getDealership().getEmail());
        }

        if (user != null) {
            customProperties.put("Phone", user.getPhone());
            User.getCurrentUser().setFirstName(user.getFirstName());
            User.getCurrentUser().setEmail(user.getEmail());
        }

        User.getCurrentUser().addProperties(customProperties);
    }

    public BluetoothAutoConnectService getBluetoothConnectService() {
        return autoConnectService;
    }

    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");

        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                settingsClicked(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void startPromptAddCarActivity() {
        Intent intent = new Intent(MainActivity.this, PromptAddCarActivity.class);
        //Don't allow user to come back to tabs without first setting a car
        startActivityForResult(intent, RC_ADD_CAR);
    }

    private void bindMercedesDealerUI(){
        TabLayout tabLayout = (TabLayout)findViewById(R.id.main_tablayout);
        tabLayout.setBackgroundColor(Color.BLACK);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.setBackgroundColor(Color.DKGRAY);
        changeTheme(true);
    }

    private void bindDefaultDealerUI(){
        Log.d(TAG,"Binding deafult dealer UI.");
        //Change theme elements back to default
        changeTheme(false);

        //Get the themes default primary color
        TypedValue defaultColor = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, defaultColor, true);

        //Set other changed UI elements back to original color
        ((TabLayout)findViewById(R.id.main_tablayout)).setBackgroundColor(defaultColor.data);
        ((AppBarLayout) findViewById(R.id.appbar)).setBackgroundColor(defaultColor.data);
    }


    private void updateScannerLocalStore(){
        useCaseComponent.getCarsByUserIdUseCase().execute(new GetCarsByUserIdUseCase.Callback() {
            @Override
            public void onCarsRetrieved(List<Car> cars) {
                Log.d(TAG,"retrievedCars: "+cars);
                for (Car car : cars) { // populate scanner table with scanner ids associated with the cars
                    if (!scannerLocalStore.isCarExist(car.getId())) {
                        carLocalStore.deleteAllCars();
                        carLocalStore.storeCars(cars);
                        scannerLocalStore.storeScanner(new ObdScanner(car.getId(), car.getScannerId()));
                        Log.d("Storing Scanner", car.getId() + " " + car.getScannerId());
                    }
                }
            }

            @Override
            public void onError(RequestError error) {

            }
        });
    }

    private boolean ignoreMissingDeviceName = false;
    private AlertDialog alertInvalidDeviceNameDialog = null;
    private boolean idInput = false;

    //Primarily for development reasons, set inside BluetoothAutoConnectService
    public static boolean allowDeviceOverwrite = false;

    private void displayGetScannerIdDialog(){
        if (idInput) return;
        if (alertInvalidDeviceNameDialog != null && alertInvalidDeviceNameDialog.isShowing())
            return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final EditText input = new EditText(MainActivity.this);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setTitle("Device Id Invalid");
                alertDialogBuilder
                        .setView(input)
                        .setMessage("Your OBD device has lost its ID or is invalid, please input " +
                                "the ID found on the front of the device so our algorithm can fix it.")
                        .setCancelable(false)
                        .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {

                                autoConnectService.setDeviceNameAndId(input.getText()
                                        .toString().trim().toUpperCase());

                                allowDeviceOverwrite = false;
                                idInput = true;
                            }
                        })
                        .setNegativeButton("Ignore",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                                ignoreMissingDeviceName = true;
                            }
                        });
                alertInvalidDeviceNameDialog = alertDialogBuilder.create();
                alertInvalidDeviceNameDialog.show();
            }
        });
    }

    public void hideLoading() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        } else {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCanceledOnTouchOutside(false);
        }
        isLoading = false;
    }

    public void showLoading(String text) {

        isLoading = true;
        if (progressDialog == null) {
            return;
        }
        progressDialog.setMessage(text);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == RC_LOCATION_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (autoConnectService.getDeviceState().equals(BluetoothConnectionObservable.State.DISCONNECTED)) {
                    autoConnectService.requestDeviceSearch(false,false);
                }
            } else {
                Snackbar.make(findViewById(R.id.main_view), R.string.location_request_rationale, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(MainActivity.this, LOC_PERMS, RC_LOCATION_PERM);
                            }
                        })
                        .show();
            }
        }
    }

    /**
     * Request permission with custom message dialog
     *
     * @param activity
     * @param permissions
     * @param requestCode
     * @param needDescription
     * @param message
     */
    public void requestPermission(final Activity activity, final String[] permissions, final int requestCode,
                                  final boolean needDescription, @Nullable final String message) {
        if (isFinishing()) {
            return;
        }

        if (needDescription) {
            new AnimatedDialogBuilder(activity)
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setCancelable(false)
                    .setTitle("Request Permissions")
                    .setMessage(message != null ? message : getString(R.string.request_permission_message_default))
                    .setNegativeButton("", null)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(activity, permissions, requestCode);
                        }
                    }).show();
        } else {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    /**
     * Onclick method for Settings button
     *
     * @param view
     */
    public void settingsClicked(View view) {

        Intent intent = new Intent(this, com.pitstop.ui.settings.SettingsActivity.class);
        startActivity(intent);

        /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(REFRESH_FROM_SERVER, true).apply();

        final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);

        IntentProxyObject proxyObject = new IntentProxyObject();

        application = ((GlobalApplication) getApplication());
        if (application.getCurrentUser() == null) {
            networkHelper.getUser(application.getCurrentUserId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        application.setCurrentUser(com.pitstop.models.User.jsonToUserObject(response));
                        startActivityForResult(intent, RC_SETTINGS);
                    } else {
                        Log.e(TAG, "Get user error: " + requestError.getMessage());
                    }
                }
            });
        } else {
            startActivityForResult(intent, RC_SETTINGS);
        }*/
    }

    /**
     * Onclick method for requesting services
     *
     * @param view if this view is null, we consider the service booking is tentative (first time)
     */
    public void requestMultiService(View view) {

        MainActivity thisInstance = this;

        showLoading("Loading...");
        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                if (!checkDealership(car)) return;

                // view is null for request from tutorial
                final Intent intent = new Intent(thisInstance, RequestServiceActivity.class);
                intent.putExtra(RequestServiceActivity.EXTRA_CAR, car);
                intent.putExtra(RequestServiceActivity.EXTRA_FIRST_BOOKING, isFirstAppointment);
                isFirstAppointment = false;
                startActivityForResult(intent, RC_REQUEST_SERVICE);
                hideLoading();
            }

            @Override
            public void onNoCarSet() {
                hideLoading();
                Toast.makeText(thisInstance,"Please add a car",Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(RequestError error) {
                hideLoading();
                Toast.makeText(thisInstance,"Error loading car",Toast.LENGTH_LONG).show();
            }
        });

    }

    public void myAppointments(){
        mixpanelHelper.trackButtonTapped("My Appointments","Dashboard");


        final MainActivity thisInstance = this;

        showLoading("Loading...");

        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                if (!checkDealership(car)) return;
                final Intent intent = new Intent(thisInstance, MyAppointmentActivity.class);
                intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA,false);
                intent.putExtra(MainActivity.CAR_EXTRA, car);
                startActivity(intent);
                hideLoading();
            }

            @Override
            public void onNoCarSet() {
                hideLoading();
                Toast.makeText(thisInstance,"Please add a car",Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(RequestError error) {
                hideLoading();
                Toast.makeText(thisInstance,"Error loading car",Toast.LENGTH_LONG).show();
            }
        });

    }

    public void myTrips(){
        final MainActivity thisInstance = this;

        showLoading("Loading...");
        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                final Intent intent = new Intent(thisInstance, MyTripsActivity.class);
                intent.putExtra(MainActivity.CAR_EXTRA, car);
                startActivity(intent);
                hideLoading();
            }

            @Override
            public void onNoCarSet() {
                hideLoading();
                Toast.makeText(thisInstance,"Please add a car",Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(RequestError error) {
                hideLoading();
                Toast.makeText(thisInstance,"Error loading car",Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void prepareAndStartTutorialSequence() {

    }

    @Override
    public void startDisplayIssueActivity(CarIssue issue) {
        Intent intent = new Intent(this, IssueDetailsActivity.class);
        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                intent.putExtra(MainActivity.CAR_EXTRA, car);
                intent.putExtra(MainActivity.CAR_ISSUE_EXTRA, issue);
                startActivityForResult(intent, MainActivity.RC_DISPLAY_ISSUE);
            }

            @Override
            public void onNoCarSet() {

            }

            @Override
            public void onError(RequestError error) {

            }
        });

    }

    @Override
    protected void onDestroy() {
        if (serviceIsBound){
            unbindService(serviceConnection);
            serviceIsBound = false;
        }
        super.onDestroy();
    }

    private boolean checkDealership(Car car) {
        if (car == null) {
            return false;
        }

        if (car.getDealership() == null) {
            Snackbar.make(rootView, "Please select your dealership first!", Snackbar.LENGTH_LONG)
                    .setAction("Select", view -> selectDealershipForDashboardCar(car))
                    .show();
            return false;
        }
        return true;
    }

    private void selectDealershipForDashboardCar(Car car) {
        final List<Dealership> dealerships = shopLocalStore.getAllDealerships();
        final List<String> shops = new ArrayList<>();
        final List<String> shopIds = new ArrayList<>();

        showLoading("Getting shop information..");
        networkHelper.getShops((response, requestError) -> {
            hideLoading();
            if (requestError == null) {
                try {
                    List<Dealership> dealers = Dealership.createDealershipList(response);
                    shopLocalStore.deleteAllDealerships();
                    shopLocalStore.storeDealerships(dealers);
                    for (Dealership dealership : dealers) {
                        shops.add(dealership.getName());
                        shopIds.add(String.valueOf(dealership.getId()));
                    }
                    showSelectDealershipDialog(car, shops.toArray(new String[shops.size()]),
                            shopIds.toArray(new String[shopIds.size()]));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "An error occurred, please try again", Toast.LENGTH_SHORT)
                            .show();
                }
            } else {
                Log.e(TAG, "Get shops: " + requestError.getMessage());
                Toast.makeText(MainActivity.this, "An error occurred, please try again", Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void showSelectDealershipDialog(Car car, final String[] shops, final String[] shopIds) {
        final int[] pickedPosition = {-1};

        final AlertDialog dialog = new AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setSingleChoiceItems(shops, -1, (dialogInterface, which) -> pickedPosition[0] = which)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("CONFIRM", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (pickedPosition[0] == -1) {
                Toast.makeText(MainActivity.this, "Please select a dealership", Toast.LENGTH_SHORT).show();
                return;
            }

            final int shopId = Integer.parseInt(shopIds[pickedPosition[0]]);

            try {
                mixpanelHelper.trackCustom("Button Tapped",
                        new JSONObject(String.format("{'Button':'Select Dealership', 'View':'%s', 'Make':'%s', 'Model':'%s'}",
                                MixpanelHelper.SETTINGS_VIEW, car.getMake(), car.getModel())));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            networkHelper.updateCarShop(car.getId(), shopId, (response, requestError) -> {
                dialog.dismiss();
                if (requestError == null) {
                    Log.i(TAG, "Dealership updated - carId: " + car.getId() + ", dealerId: " + shopId);
                    // Update car in local database
                    car.setShopId(shopId);
                    car.setDealership(shopLocalStore.getDealership(shopId));
                    carLocalStore.updateCar(car);

                    final Map<String, Object> properties = User.getCurrentUser().getProperties();
                    properties.put("Email", shopLocalStore.getDealership(shopId).getEmail());
                    User.getCurrentUser().addProperties(properties);

                    Toast.makeText(MainActivity.this, "Car dealership updated", Toast.LENGTH_SHORT).show();

                } else {
                    Log.e(TAG, "Dealership updateCarIssue error: " + requestError.getError());
                    Toast.makeText(MainActivity.this, "There was an error, please try again", Toast.LENGTH_SHORT).show();
                }
            });
        }));

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void logAuthInfo(){
        LogUtils.LOGD(TAG,"RefreshToken: "+application.getRefreshToken());
        LogUtils.LOGD(TAG,"AccessToken: "+application.getAccessToken());
    }

    @Override
    public void onDeviceNeedsOverwrite() {

        LogUtils.LOGD(TAG,"onDeviceNeedsOverwrite(), BuildConfig.DEBUG?" + BuildConfig.DEBUG
                + " ignoreMissingDeviceName?"+ignoreMissingDeviceName);

        /*Check for device name being broken and create pop-up to set the id on DEBUG only(for now)
        **For 215 device only*/

        if ((BuildConfig.DEBUG || BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_BETA))
                && !ignoreMissingDeviceName && allowDeviceOverwrite){

            displayGetScannerIdDialog();
        }
    }

    @Override
    public void onSearchingForDevice() {
        Log.d(TAG,"onSearchingForDevice()");
        displayDeviceState(BluetoothConnectionObservable.State.SEARCHING);
    }

    public void onDeviceReady(ReadyDevice device) {
        displayDeviceState(BluetoothConnectionObservable.State.CONNECTED_VERIFIED);
    }

    @Override
    public void onDeviceDisconnected() {
        Log.d(TAG,"onDeviceDisconnected()");
        displayDeviceState(BluetoothConnectionObservable.State.DISCONNECTED);
    }

    @Override
    public void onDeviceVerifying() {
        Log.d(TAG,"onDeviceVerifying()");
        displayDeviceState(BluetoothConnectionObservable.State.VERIFYING);
    }

    @Override
    public void onDeviceSyncing() {

    }

    @Override
    public void openCurrentServices() {
        tabFragmentManager.openServices();
    }

    @Override
    public void openAppointments() {
        myAppointments();
    }

    @Override
    public void openScanTab() {
        tabFragmentManager.openScanTab();
    }
}
