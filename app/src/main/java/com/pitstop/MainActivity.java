package com.pitstop;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.ObdDataUtil;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.SaveCallback;
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.DTOs.IntentProxyObject;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarIssueAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalShopAdapter;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestError;
import com.pitstop.adapters.MainAppSideMenuAdapter;
import com.pitstop.adapters.MainAppViewPagerAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.background.MigrationService;
import com.pitstop.fragments.MainDashboardFragment;
import com.pitstop.fragments.MainToolFragment;
import com.pitstop.utils.MainAppViewPager;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;

/**
 * Created by David on 6/8/2016.
 */
public class MainActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener {

    public static List<Car> carList = new ArrayList<>();
    private List<CarIssue> carIssueList = new ArrayList<>();

    private ProgressDialog progressDialog;


    public static LocalCarAdapter carLocalStore;
    public static LocalCarIssueAdapter carIssueLocalStore;
    public static LocalShopAdapter shopLocalStore;

    public static final int RC_ADD_CAR = 50;
    public static final int RC_SCAN_CAR = 51;
    public static final int RC_SETTINGS = 52;
    public static final int RC_DISPLAY_ISSUE = 53;
    public static final String FROM_NOTIF = "from_notfftfttfttf";


    public static final int RC_ENABLE_BT= 102;
    public static final int RESULT_OK = 60;

    private static final String SHOWCASE_ID = "main_activity_sequence_01";

    public static final String CAR_EXTRA = "car";
    public static final String CAR_ISSUE_EXTRA = "car_issue";
    public static final String CAR_LIST_EXTRA = "car_list";
    public static final String HAS_CAR_IN_DASHBOARD = "has_car";
    public static final String REFRESH_FROM_SERVER = "_server";
    public static final String FROM_ACTIVITY = "from_activity";

    public static final String TAG = "MainActivity";
    public static final int LOC_PERM_REQ = 112;
    public static final int RC_LOCATION_PERM = 101;
    public static final String[] LOC_PERMS = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle="Your Vehicles";
    private CharSequence mTitle="Pitstop";
    private MixpanelHelper mixpanelHelper;
    private  Toolbar toolbar;
    private MainAppViewPager viewPager;
    private TabLayout tabLayout;
    private Car dashboardCar;
    private GlobalApplication application;

    private NetworkHelper networkHelper;
    public static MainDashboardCallback callback;
    private MainAppSideMenuAdapter mainAppSideMenuAdapter;



    private boolean isLoading = false;
    private BluetoothAutoConnectService autoConnectService;
    private boolean serviceIsBound;
    private Intent serviceIntent;
    protected ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG,"connecting: onServiceConnection");
            // cast the IBinder and get MyService instance
            serviceIsBound = true;

            autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
            autoConnectService.setCallbacks(MainActivity.this);

            // Send request to user to turn on bluetooth if disabled
            if (BluetoothAdapter.getDefaultAdapter()!=null) {

                if(ContextCompat.checkSelfPermission(MainActivity.this, LOC_PERMS[0]) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(MainActivity.this, LOC_PERMS[1]) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, LOC_PERMS, RC_LOCATION_PERM);
                } else {
                    autoConnectService.startBluetoothSearch();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            Log.i(TAG,"Disconnecting: onServiceConnection");
            serviceIsBound = false;
            autoConnectService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(MigrationService.notificationId);

        application = (GlobalApplication) getApplicationContext();
        setContentView(R.layout.activity_main_drawer_frame);

        ParseACL acl = new ParseACL();
        acl.setPublicReadAccess(true);
        acl.setPublicWriteAccess(true);

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.setACL(acl);
        installation.put("userId", String.valueOf(application.getCurrentUserId()));
        installation.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if(e == null) {
                    Log.d(TAG, "Installation saved");
                } else {
                    Log.w(TAG, "Error saving installation: " + e.getMessage());
                }
            }
        });

        serviceIntent = new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);

        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        networkHelper = new NetworkHelper(getApplicationContext());

        // Local db adapters
        carLocalStore = new LocalCarAdapter(this);
        carIssueLocalStore = new LocalCarIssueAdapter(this);
        shopLocalStore = new LocalShopAdapter(this);

        viewPager = (MainAppViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        MainAppViewPagerAdapter adapter = new MainAppViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new MainDashboardFragment(), "DASHBOARD");
        adapter.addFragment(new MainToolFragment(), "TOOLS");

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.bluetooth, R.string.bluetooth) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        viewPager.setAdapter(adapter);
        mDrawerToggle.syncState();
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof MainDashboardFragment) {
            // Always refresh from the server if entering from log in activity
            if (getIntent().getBooleanExtra(SplashScreen.LOGIN_REFRESH, false)) {
                Log.i(TAG, "refresh from login");
                refreshFromServer();
            } else if (SelectDealershipActivity.ACTIVITY_NAME.equals(getIntent().getStringExtra(FROM_ACTIVITY))) {
                // In the event the user pressed back button while in the select dealership activity
                // then load required data from local db.
                refreshFromLocal();
            } else if (PitstopPushBroadcastReceiver.ACTIVITY_NAME.equals(getIntent().getStringExtra(FROM_ACTIVITY))) {
                // On opening a push notification, load required data from server
                refreshFromServer();
            } else if (getIntent().getBooleanExtra(FROM_NOTIF, false)) {
                refreshFromServer();
            }
            resetMenus(true);
        }
    }

    public void resetMenus(boolean check){
        if(carList.size()==0&&check){
            refreshFromServer();
        }
        int id = PreferenceManager.getDefaultSharedPreferences(this).getInt(MainDashboardFragment.pfCurrentCar, 0);
        if (carList.size() > 0) {
            for(Car car : carList) {
                if(car.getId()==id) {
                    dashboardCar = car;
                    car.setCurrentCar(true);
                }
            }
            if(dashboardCar==null){
                carList.get(0).setCurrentCar(true);
                dashboardCar = carList.get(0);
            }
            callback.setDashboardCar(MainActivity.carList);
            callback.setCarDetailsUI();
        }
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer_listview);

        if(mDrawerList!=null) {
            if(mainAppSideMenuAdapter==null){
                mainAppSideMenuAdapter = new MainAppSideMenuAdapter(this,
                        carList.toArray(new Car[carList.size()]));
                mDrawerList.setAdapter(mainAppSideMenuAdapter);
            }else{
                mainAppSideMenuAdapter.setData(carList.toArray(new Car[carList.size()]));
                mainAppSideMenuAdapter.notifyDataSetChanged();
            }
            // Set the adapter for the list view
//         Set the list's click listener
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "onResume");

        resetMenus(false);

        try {
            if(dashboardCar == null || dashboardCar.getDealership() == null) {
                mixpanelHelper.trackViewAppeared(TAG);
            } else {
                mixpanelHelper.trackCustom("View Appeared",
                        new JSONObject("{'View':'" + TAG + "','Dealership':'" + dashboardCar.getDealership().getName()
                                + "','Device':'Android'}"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        if(serviceIsBound) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivity");

        if(data != null) {
            boolean shouldRefreshFromServer = data.getBooleanExtra(REFRESH_FROM_SERVER,false);

            if(requestCode == RC_ADD_CAR && resultCode==AddCarActivity.ADD_CAR_SUCCESS) {
                if(shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if(requestCode == RC_SCAN_CAR && resultCode == RESULT_OK) {
                if(shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if(requestCode == RC_SETTINGS && resultCode == RESULT_OK) {
                if(shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if(requestCode == RC_DISPLAY_ISSUE && resultCode == RESULT_OK) {
                if(shouldRefreshFromServer) {
                    refreshFromServer();
                }
            }
            callback.activityResultCallback(requestCode,resultCode,data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public BluetoothAutoConnectService getBluetoothConnectService(){
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

    public void scanClicked(View view){

        try {
            mixpanelHelper.trackCustom("Button Tapped",
                    new JSONObject(String.format("{'Button':'Scan', 'View':'%s', 'Make':'%s', 'carModel':'%s', 'Device':'Android'}",
                            TAG, dashboardCar.getMake(), dashboardCar.getModel())));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(MainActivity.REFRESH_FROM_SERVER, true).apply();

        Intent intent = new Intent(this, CarScanActivity.class);
        intent.putExtra(MainActivity.CAR_EXTRA, dashboardCar);
        startActivityForResult(intent, MainActivity.RC_SCAN_CAR);
        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }
    public void refreshClicked(View view){
        try {
            mixpanelHelper.trackButtonTapped("Refresh", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        refreshFromServer();

        if (autoConnectService.getState() == IBluetoothCommunicator.DISCONNECTED) {
            autoConnectService.startBluetoothSearch();
        }
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
    }
    public void addClicked(View view){

        try {
            mixpanelHelper.trackButtonTapped("Add Car", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        startAddCarActivity(null);
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
    }
    public void settingsClicked(View view){
        try {
            mixpanelHelper.trackButtonTapped("Settings", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(REFRESH_FROM_SERVER, true).apply();

        final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);

        IntentProxyObject proxyObject = new IntentProxyObject();

        proxyObject.setCarList(carList);
        intent.putExtra(CAR_LIST_EXTRA, proxyObject);

        application = ((GlobalApplication) getApplication());
        if (application.getCurrentUser() == null) {
            networkHelper.getUser(application.getCurrentUserId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        application.setCurrentUser(com.pitstop.DataAccessLayer.DTOs.User.jsonToUserObject(response));
                        startActivityForResult(intent, RC_SETTINGS);
                        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
                    } else {
                        Log.e(TAG, "Get user error: " + requestError.getMessage());
                    }
                }
            });
        } else {
            startActivityForResult(intent, RC_SETTINGS);
            overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
        }
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    public List<CarIssue> getCarIssueList() {
        return carIssueList;
    }

    public void clickServiceHistory(View view) {
            try {
                mixpanelHelper.trackButtonTapped("History", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(MainActivity.this, CarHistoryActivity.class);
            //intent.putExtra("carId",dashboardCar.getId());
            intent.putExtra(MainActivity.CAR_EXTRA, dashboardCar);
            startActivity(intent);
            overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
            resetMenus(false);
        }
    }

    public void refreshFromServer() {
        if(NetworkHelper.isConnected(this)) {
            Log.d("random", "refresh called");
            if(carLocalStore == null) {
                carLocalStore = new LocalCarAdapter(this);
            }
            carLocalStore.deleteAllCars();
            carIssueLocalStore.deleteAllCarIssues();
            carIssueList.clear();
            getCarDetails();
            if (isLoading) {
                hideLoading();
            }
            if (callback != null) {

                callback.onServerRefreshed();
            }
        }else{
            Snackbar.make(findViewById(R.id.drawer_layout),"You are not connected to internet",Snackbar.LENGTH_SHORT).show();
            refreshFromLocal();
            resetMenus(false);
        }
    }

    public void refreshFromLocal() {
        carIssueList.clear();
        getCarDetails();
        if(isLoading) {
            hideLoading();
        }
        if(callback!=null) {
            callback.onLocalRefreshed();
        }
    }


    /**
     * Get list of cars associated with current user
     * */
    private void getCarDetails() {


        showLoading("Retrieving car details");

        // Try local store
        List<Car> localCars = carLocalStore.getAllCars();

        if(localCars.isEmpty()) {
            loadCarDetailsFromServer();
        } else {
            Log.i(TAG,"Trying local store for cars");
            MainActivity.carList = localCars;

            callback.setDashboardCar(MainActivity.carList);
            callback.setCarDetailsUI();
            hideLoading();
        }
    }

    /** Call function to retrieve live data from parse
     * @see #getCarDetails()
     * */
    private void loadCarDetailsFromServer() {
        final int userId = application.getCurrentUserId();

        networkHelper.getUser(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(response == null || response.isEmpty() || response.equals("{}")) {
                    application.logOutUser();
                    Toast.makeText(application, "Your session has expired.  Please login again.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    int mainCarId = -1;
                    try {
                        mainCarId = new JSONObject(response).getJSONObject("settings").getInt("mainCar");
                    } catch (JSONException e) {
                    }
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    sharedPreferences.edit().putInt(MainDashboardFragment.pfCurrentCar, mainCarId).commit();

                    final int mainCarIdCopy = mainCarId;

                    networkHelper.getCarsByUserId(userId, new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            if (requestError == null) {
                                try {
                                    carList = Car.createCarsList(response);

                                    if (carList.isEmpty()) {
                                        if (isLoading) {
                                            hideLoading();
                                        }
                                        findViewById(R.id.main_view).setVisibility(View.GONE);
                                        findViewById(R.id.no_car_text).setVisibility(View.VISIBLE);
                                    } else {
                                        if(mainCarIdCopy != -1) {
                                            for (Car car : carList) {
                                                if (car.getId() == mainCarIdCopy) {
                                                    car.setCurrentCar(true);
                                                }
                                            }
                                        } else {
                                            carList.get(0).setCurrentCar(true);
                                        }

                                        findViewById(R.id.no_car_text).setVisibility(View.GONE);
                                        callback.setDashboardCar(carList);
                                        carLocalStore.deleteAllCars();
                                        carLocalStore.storeCars(carList);
                                        callback.setCarDetailsUI();
                                    }

                                    mainAppSideMenuAdapter.setData(carList.toArray(new Car[carList.size()]));
                                    mainAppSideMenuAdapter.notifyDataSetChanged();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this,
                                            "Error retrieving car details.  Please check your internet connection.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.e(TAG, "Load cars error: " + requestError.getMessage());
                                Toast.makeText(MainActivity.this,
                                        "Error retrieving car details.  Please check your internet connection.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void getBluetoothState(int state) {
        if(state== BluetoothManage.DISCONNECTED) {
            Log.i(TAG,"Bluetooth disconnected");
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {}

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {}

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {   }

    @Override
    public void getIOData(final DataPackageInfo dataPackageInfo) {
        if(dataPackageInfo.dtcData != null && !dataPackageInfo.dtcData.isEmpty()) {

            final HashSet<String> activeIssueNames = new HashSet<>();

            for(CarIssue issues : dashboardCar.getActiveIssues()) {
                activeIssueNames.add(issues.getIssueDetail().getItem());
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean newDtcFound = false;

                    if(dataPackageInfo.dtcData!=null&&dataPackageInfo.dtcData.length()>0){
                        String[] DTCs = dataPackageInfo.dtcData.split(",");
                        for(String dtc : DTCs) {
                            String parsedDtc = ObdDataUtil.parseDTCs(dtc);
                            if(!activeIssueNames.contains(parsedDtc)) {
                                newDtcFound = true;
                            }
                        }
                    }

                    if(newDtcFound) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                refreshFromServer();
                            }
                        }, 1111);
                    }
                }
            });
        }
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if(loginPackageInfo.flag.
                equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            Log.i(TAG, "Device logout");
        }
    }


    public void hideLoading() {
        progressDialog.dismiss();
        isLoading = false;


    }

    public void showLoading(String text) {

        isLoading = true;

        progressDialog.setMessage(text);
        if(!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position){
        dashboardCar = carList.get(position);
        for(Car car : carList) {
            car.setCurrentCar(false);
        }
        dashboardCar.setCurrentCar(true);
        networkHelper.setMainCar(application.getCurrentUserId(), dashboardCar.getId(), null);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(MainDashboardFragment.pfCurrentCar, dashboardCar.getId()).commit();
        // Highlight the selected item, update the title, and close the drawer
        for(int i = 0 ; i < carList.size() ; i++) {
            mDrawerList.setItemChecked(i, false);
        }
        mDrawerList.setItemChecked(position, true);
        callback.setDashboardCar(carList);
        callback.setCarDetailsUI();
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
        if(viewPager.getCurrentItem() == 0) {
            findViewById(R.id.viewpager).startAnimation(AnimationUtils.loadAnimation(this, R.anim.switch_car));
        }
    }


    public void startAddCarActivity(View view) {
        Intent intent = new Intent(MainActivity.this, com.pitstop.AddCarProcesses.AddCarActivity.class);
        startActivityForResult(intent, RC_ADD_CAR);
        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }


    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions,
                                            int[] grantResults) {
        if(requestCode == RC_LOCATION_PERM) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //autoConnectService.startBluetoothSearch();
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
     * Request service for all issues currently displayed or custom request
     * */
    public void requestMultiService(View view) {

        try {
            mixpanelHelper.trackButtonTapped("Request Service", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Enter additional comment");

        final String[] additionalComment = {""};
        final EditText userInput = new EditText(this);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT);
        alertDialog.setView(userInput);

        alertDialog.setPositiveButton("SEND", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    application.getMixpanelAPI().track("Button Tapped",
                            new JSONObject("{'Button':'Confirm Service Request','View':'" + TAG
                                    + "','Device':'Android','Number of Services Requested':"
                                    + dashboardCar.getActiveIssues().size() + "}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                additionalComment[0] = userInput.getText().toString();
                sendRequest(additionalComment[0]);
            }
        });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mixpanelHelper.trackButtonTapped("Cancel Request Service", TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    public void startChat(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Chat with " + dashboardCar.getDealership().getName(), TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final HashMap<String, Object> customProperties = new HashMap<>();
        customProperties.put("VIN", dashboardCar.getVin());
        customProperties.put("Car Make",  dashboardCar.getMake());
        customProperties.put("Car Model", dashboardCar.getModel());
        customProperties.put("Car Year", dashboardCar.getYear());
        Log.i(TAG, dashboardCar.getDealership().getEmail());
        customProperties.put("Email",dashboardCar.getDealership().getEmail());
        User.getCurrentUser().addProperties(customProperties);
        if(application.getCurrentUser() != null) {
            customProperties.put("Phone", application.getCurrentUser().getPhone());
            User.getCurrentUser().setFirstName(application.getCurrentUser().getFirstName());
            User.getCurrentUser().setEmail(application.getCurrentUser().getEmail());
        }
        ConversationActivity.show(this);
        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }

    public void navigateToDealer(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Directions to " + dashboardCar.getDealership().getName(), TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String uri = String.format(Locale.ENGLISH,
                "http://maps.google.com/maps?daddr=%s",
                dashboardCar.getDealership().getAddress());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }

    public void callDealer(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Confirm call to " + dashboardCar.getDealership().getName(), TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                dashboardCar.getDealership().getPhone()));
        startActivity(intent);
        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }

    /**
     * Request service for all issues currently displayed or custom request
     * @see #requestMultiService(View)
     * */
    private void sendRequest(String additionalComment) {

        networkHelper.requestService(application.getCurrentUserId(), dashboardCar.getId(),
                dashboardCar.getShopId(), additionalComment, new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null) {
                            Toast.makeText(getApplicationContext(), "Service request sent", Toast.LENGTH_SHORT).show();
                            for(CarIssue issue : dashboardCar.getActiveIssues()) {
                                networkHelper.servicePending(dashboardCar.getId(), issue.getId(), null);
                            }
                        } else {
                            Log.e(TAG, "service request: " + requestError.getMessage());
                            Toast.makeText(getApplicationContext(), "There was an error, please try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    public interface MainDashboardCallback{
        void activityResultCallback(int requestCode, int resultCode, Intent data);
        void onServerRefreshed();
        void onLocalRefreshed();

        void setDashboardCar(List<Car> carList);

        void setCarDetailsUI();
    }
}
