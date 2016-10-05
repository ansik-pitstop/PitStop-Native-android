package com.pitstop.ui;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.pitstop.R;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.models.IntentProxyObject;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.models.ObdScanner;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.adapters.MainAppSideMenuAdapter;
import com.pitstop.adapters.MainAppViewPagerAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.utils.MigrationService;
import com.pitstop.ui.mainFragments.MainDashboardFragment;
import com.pitstop.ui.mainFragments.MainToolFragment;
import com.pitstop.ui.mainFragments.MainAppViewPager;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.ServiceRequestUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import io.smooch.core.Smooch;
import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

/**
 * Created by David on 6/8/2016.
 */
public class MainActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Used in communication between MainActivity and BluetoothAutoConnectService<br>
     * Action for intents
     */
    public static final String ACTION_UNRECOGNIZED_OBD_MODULE_DISCOVERED = "Obd - discovered";
    public static final String ACTION_PAIRING_MODULE_ID_INVALID = "Obd - device id invalid";
    public static final String ACTION_PAIRING_MODULE_SUCCESS = "Obd - paired device with car";
    public static final String ACTION_PAIRING_MODULE_NETWORK_ERROR = "Network error happened";

    private GlobalApplication application;
    private BluetoothAutoConnectService autoConnectService;
    private boolean serviceIsBound;
    private Intent serviceIntent;
    protected ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "connecting: onServiceConnection");
            // cast the IBinder and get MyService instance
            serviceIsBound = true;

            autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
            autoConnectService.setCallbacks(MainActivity.this);

            // Send request to user to turn on bluetooth if disabled
            if (BluetoothAdapter.getDefaultAdapter() != null) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, LOC_PERMS[0]) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(MainActivity.this, LOC_PERMS[1]) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, LOC_PERMS, RC_LOCATION_PERM);
                } else {
                    autoConnectService.startBluetoothSearch();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            Log.i(TAG, "Disconnecting: onServiceConnection");
            serviceIsBound = false;
            autoConnectService = null;
        }
    };

    // Model
    public static List<Car> carList = new ArrayList<>();
    private List<CarIssue> carIssueList = new ArrayList<>();
    private Car dashboardCar;

    // Database accesses
    public static LocalCarAdapter carLocalStore;
    public static LocalCarIssueAdapter carIssueLocalStore;
    public static LocalShopAdapter shopLocalStore;
    public static LocalScannerAdapter scannerLocalStore;

    public static final int RC_ADD_CAR = 50;
    public static final int RC_SCAN_CAR = 51;
    public static final int RC_SETTINGS = 52;
    public static final int RC_DISPLAY_ISSUE = 53;
    public static final String FROM_NOTIF = "from_notfftfttfttf";

    public static final int RC_ENABLE_BT = 102;
    public static final int RESULT_OK = 60;

    public static final String CAR_EXTRA = "car";
    public static final String CAR_ISSUE_EXTRA = "car_issue";
    public static final String CAR_LIST_EXTRA = "car_list";
    public static final String HAS_CAR_IN_DASHBOARD = "has_car";
    public static final String REFRESH_FROM_SERVER = "_server";
    public static final String FROM_ACTIVITY = "from_activity";

    public static final int LOC_PERM_REQ = 112;
    public static final int RC_LOCATION_PERM = 101;
    public static final String[] LOC_PERMS = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    // Views
    private Toolbar toolbar;
    private CharSequence mTitle = "Pitstop";
    private CharSequence mDrawerTitle = "Your Vehicles";
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private MainAppViewPager viewPager;
    private TabLayout tabLayout;
    private ProgressDialog progressDialog;
    private boolean isLoading = false;
    private MainAppSideMenuAdapter mainAppSideMenuAdapter;

    // Utils / Helper
    private MixpanelHelper mixpanelHelper;
    private NetworkHelper networkHelper;

    private boolean createdOrAttached = false; // check if onCreate or onAttachFragment has completed

    public static MainDashboardCallback callback;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_UNRECOGNIZED_OBD_MODULE_DISCOVERED:
                    Log.d(TAG, "OBD device discovered intent received!");
                    mixpanelHelper.trackAlertAppeared(MixpanelHelper.UNRECOGNIZED_MODULE_FOUND,
                            MixpanelHelper.UNRECOGNIZED_MODULE_VIEW);
                    viewPager.setCurrentItem(MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD);
                    showPairDeviceWithCarDialog();
                    break;
                case ACTION_PAIRING_MODULE_NETWORK_ERROR:
                    Log.d(TAG, "Network error occurred");
                    mixpanelHelper.trackAlertAppeared(MixpanelHelper.UNRECOGNIZED_MODULE_NETWORK_ERROR,
                            MixpanelHelper.UNRECOGNIZED_MODULE_VIEW);
                    if (isLoading) hideLoading();
                    showSimpleMessage("Sorry, some network error occurred.", false);
                    break;
                case ACTION_PAIRING_MODULE_ID_INVALID:
                    Log.d(TAG, "Device ID invalid");
                    mixpanelHelper.trackAlertAppeared(MixpanelHelper.UNRECOGNIZED_MODULE_INVALID_ID,
                            MixpanelHelper.UNRECOGNIZED_MODULE_VIEW);
                    if (isLoading) hideLoading();
                    showSimpleMessage("This device belongs to another vehicle.", false);
                    break;
                case ACTION_PAIRING_MODULE_SUCCESS:
                    Log.d(TAG, "Successfully paried device with car");
                    mixpanelHelper.trackAlertAppeared(MixpanelHelper.UNRECOGNIZED_MODULE_PAIRING_SUCCESS,
                            MixpanelHelper.UNRECOGNIZED_MODULE_VIEW);
                    if (isLoading) hideLoading();
                    showSimpleMessage("OK, we have linked the device with your car!", true);
                    refreshFromServer();
                    break;
            }
        }
    };

    private MainDashboardFragment mDashboardFragment = new MainDashboardFragment();
    private MainToolFragment mToolFragment = new MainToolFragment();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        networkHelper = new NetworkHelper(getApplicationContext());
        super.onCreate(savedInstanceState);

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(MigrationService.notificationId);

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
                if (e == null) {
                    Log.d(TAG, "Installation saved");
                } else {
                    Log.w(TAG, "Error saving installation: " + e.getMessage());
                }
            }
        });

        serviceIntent = new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        // Local db adapters
        carLocalStore = new LocalCarAdapter(application);
        carIssueLocalStore = new LocalCarIssueAdapter(application);
        shopLocalStore = new LocalShopAdapter(application);
        scannerLocalStore = new LocalScannerAdapter(application);

        viewPager = (MainAppViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        if (createdOrAttached) {
            refreshFromServer();
        } else {
            createdOrAttached = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "onResume");

        // TODO: 16/10/5 Test
        if (autoConnectService != null) {
            autoConnectService.setCallbacks(this);
        }

        resetMenus(false);

        try {
            if (dashboardCar == null || dashboardCar.getDealership() == null) {
                switch (viewPager.getCurrentItem()) {
                    case MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD:
                        mixpanelHelper.trackViewAppeared(MixpanelHelper.DASHBOARD_VIEW);
                        break;
                    case MainAppViewPager.PAGE_NUM_MAIN_TOOL:
                        mixpanelHelper.trackViewAppeared(MixpanelHelper.TOOLS_VIEW);
                        break;
                }
            } else {
                String view = viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                        MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW;
                mixpanelHelper.trackCustom("View Appeared",
                        new JSONObject("{'View':'" + view + "','Dealership':'" + dashboardCar.getDealership().getName()
                                + "'}"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        logScannerTable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        createdOrAttached = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerBroadcastReceiver();
    }

    @Override
    protected void onStop() {
        unregisterBroadcastReceiver();
        super.onStop();
    }

    private void setupViewPager(final ViewPager viewPager) {
        MainAppViewPagerAdapter adapter = new MainAppViewPagerAdapter(getSupportFragmentManager());
//        adapter.addFragment(new MainDashboardFragment(), "DASHBOARD");
//        adapter.addFragment(new MainToolFragment(), "TOOLS");
        adapter.addFragment(mDashboardFragment, "DASHBOARD");
        adapter.addFragment(mToolFragment, "TOOLS");

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //Track user changed page
                switch (position) {
                    case MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD:
                        Log.d(TAG, "Dashboard shows up");
                        try {
                            mixpanelHelper.trackButtonTapped("Dashboard", MixpanelHelper.DASHBOARD_VIEW);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                    case MainAppViewPager.PAGE_NUM_MAIN_TOOL:
                        Log.d(TAG, "Tools shows up");
                        try {
                            mixpanelHelper.trackButtonTapped("Tools", MixpanelHelper.TOOLS_VIEW);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.bluetooth, R.string.bluetooth) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                try {
                    mixpanelHelper.trackButtonTapped(MixpanelHelper.MAIN_ACTIVITY_CLOSE_SIDE_MENU,
                            viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                                    MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                try {
                    mixpanelHelper.trackButtonTapped(MixpanelHelper.MAIN_ACTIVITY_OPEN_SIDE_MENU,
                            viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                                    MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
            // refresh must only happen after onCreate is completed and onOnAttachFragment is completed
            if (createdOrAttached) {
                refreshFromServer();
            } else {
                createdOrAttached = true;
            }
        }
    }

    // repopulate car list
    public void resetMenus(boolean refresh) {
        if (carList.size() == 0 && refresh) {
            refreshFromServer();
        }
        int id = PreferenceManager.getDefaultSharedPreferences(this).getInt(MainDashboardFragment.pfCurrentCar, 0);
        if (carList.size() > 0) {
            for (Car car : carList) {
                if (car.getId() == id) {
                    dashboardCar = car;
                    car.setCurrentCar(true);
                }
            }
            if (dashboardCar == null) {
                carList.get(0).setCurrentCar(true);
                dashboardCar = carList.get(0);
            }
            callback.setDashboardCar(MainActivity.carList);
            callback.setCarDetailsUI();
        }
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer_listview);

        if (mDrawerList != null) {
            if (mainAppSideMenuAdapter == null) {
                mainAppSideMenuAdapter = new MainAppSideMenuAdapter(this,
                        carList.toArray(new Car[carList.size()]));
                mDrawerList.setAdapter(mainAppSideMenuAdapter);
            } else {
                mainAppSideMenuAdapter.setData(carList.toArray(new Car[carList.size()]));
                mainAppSideMenuAdapter.notifyDataSetChanged();
            }
            // Set the adapter for the list view
//         Set the list's click listener
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        if (serviceIsBound) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");

        if (data != null) {
            boolean shouldRefreshFromServer = data.getBooleanExtra(REFRESH_FROM_SERVER, false);

            if (requestCode == RC_ADD_CAR && resultCode == AddCarActivity.ADD_CAR_SUCCESS) {
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
                Log.d("OnActivityResult", "CarList: " + carList.size());
                Log.d("OnActivityResult", LoginActivity.sState);
                if (carList.size() == 0 && LoginActivity.sState == LoginActivity.SIGNUP) {
                    LoginActivity.switchStateForTutorial();
                    prepareAndStartTutorialSequence();
                }
            } else if (requestCode == RC_SCAN_CAR && resultCode == RESULT_OK) {
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if (requestCode == RC_SETTINGS && resultCode == RESULT_OK) {
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if (requestCode == RC_DISPLAY_ISSUE && resultCode == RESULT_OK) {
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
            }
            callback.activityResultCallback(requestCode, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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

    public void scanClicked(View view) {
        try {
            mixpanelHelper.trackCustom("Button Tapped",
                    new JSONObject(String.format("{'Button':'Scan', 'View':'%s', 'Make':'%s', 'Model':'%s'}",
                            viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                                    MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW
                            , dashboardCar.getMake(), dashboardCar.getModel())));
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

    /**
     * Invoked when the "REFRESH" button in the drawer is clicked
     *
     * @param view
     */
    public void refreshClicked(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Refresh",
                    viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                            MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        refreshFromServer();

        if (autoConnectService.getState() == IBluetoothCommunicator.DISCONNECTED) {
            autoConnectService.startBluetoothSearch(); // refresh clicked
        }
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
    }

    /**
     * Invoked when the "ADD VEHICLE" button in the drawer is clicked.
     * Starts the AddCarActivity
     *
     * @param view
     */
    public void addClicked(View view) {
        try {
            mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_ADD_CAR_TAPPED,
                    viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                            MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        startAddCarActivity(null);
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
    }

    /**
     * Invoked when the "SETTINGS" button in the drawer is clicked
     * Starts the SettingsActivity
     *
     * @param view
     */
    public void settingsClicked(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Settings",
                    viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                            MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW);
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
                        application.setCurrentUser(com.pitstop.models.User.jsonToUserObject(response));
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
            mixpanelHelper.trackButtonTapped("History", MixpanelHelper.TOOLS_VIEW);
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

            // Track new selected car
            String button = dashboardCar.getMake() + " " + dashboardCar.getModel();
            try {
                mixpanelHelper.trackButtonTapped("Select Current Car: " + button, viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                        MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieve a list of cars associated with current user
     */
    public void refreshFromServer() {
        if (NetworkHelper.isConnected(this)) {
            Log.d(TAG, "refresh called");
            if (carLocalStore == null) {
                carLocalStore = new LocalCarAdapter(this);
            }
            if (carIssueLocalStore == null) {
                carIssueLocalStore = new LocalCarIssueAdapter(this);
            }
            carLocalStore.deleteAllCars();
            carIssueLocalStore.deleteAllCarIssues();
            carIssueList.clear();
            getCarDetails();
            if (callback != null) {
                callback.onServerRefreshed();
            }
        } else {
            View drawerLayout = findViewById(R.id.drawer_layout);
            if (drawerLayout != null) {
                Snackbar.make(mDrawerLayout, "You are not connected to internet", Snackbar.LENGTH_SHORT).show();
            }
            refreshFromLocal();
            resetMenus(false);
            if (isLoading) {
                hideLoading();
            }
        }
    }

    public void refreshFromLocal() {
        carIssueList.clear();
        getCarDetails();
        if (isLoading) {
            hideLoading();
        }
        if (callback != null) {
            callback.onLocalRefreshed();
        }
    }

    /**
     * Get list of cars associated with current user
     */
    private void getCarDetails() {
        showLoading("Retrieving car details");

        // Try local store
        List<Car> localCars = carLocalStore.getAllCars();

        if (localCars.isEmpty()) {
            loadCarDetailsFromServer();
        } else {
            Log.i(TAG, "Trying local store for cars");
            MainActivity.carList = localCars;

            if (callback != null) {
                callback.setDashboardCar(MainActivity.carList);
                callback.setCarDetailsUI();
            }
            hideLoading();
        }
    }

    /**
     * Call function to retrieve live data from parse
     *
     * @see #getCarDetails()
     */
    private void loadCarDetailsFromServer() {
        final int userId = application.getCurrentUserId();

        networkHelper.getUser(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (response != null && response.equals("{}")) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    preferences.edit()
                            .putBoolean(getString(R.string.pfTutorialShown), false)
                            .putBoolean(getString(R.string.pfFirstBookingDiscountAvailability), false)
                            .apply();
                    application.logOutUser();
                    Toast.makeText(application, "Your session has expired.  Please login again.", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (response == null || response.isEmpty() || requestError != null) { // couldn't get cars from server, show try again
                    View mainView = findViewById(R.id.main_view);
                    View noCarText = findViewById(R.id.no_car_text);
                    View noConnectText = findViewById(R.id.no_connect_text);
                    View requestServiceButton = findViewById(R.id.request_service_btn);
                    if (mainView != null) {
                        mainView.setVisibility(View.GONE);
                    }
                    if (noCarText != null) {
                        noCarText.setVisibility(View.GONE);
                    }
                    if (noConnectText != null) {
                        noConnectText.setVisibility(View.VISIBLE);
                    }
                    if (requestServiceButton != null) {
                        requestServiceButton.setVisibility(View.GONE);
                    }
                    tabLayout.setVisibility(View.GONE);
                    viewPager.setPagingEnabled(false);
                    Toast.makeText(application, "An error occurred, please try again", Toast.LENGTH_SHORT).show();
                    hideLoading();
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
                                View mainView = findViewById(R.id.main_view);
                                View noCarText = findViewById(R.id.no_car_text);
                                View noConnectText = findViewById(R.id.no_connect_text);
                                View requestServiceButton = findViewById(R.id.request_service_btn);
                                try {
                                    carList = Car.createCarsList(response);

                                    if (carList.isEmpty()) { // show add first car text
                                        if (isLoading) {
                                            hideLoading();
                                        }
                                        if (mainView != null) {
                                            mainView.setVisibility(View.GONE);
                                        }
                                        if (noCarText != null) {
                                            noCarText.setVisibility(View.VISIBLE);
                                        }
                                        if (noConnectText != null) {
                                            noConnectText.setVisibility(View.GONE);
                                        }
                                        if (requestServiceButton != null) {
                                            requestServiceButton.setVisibility(View.GONE);
                                        }
                                        viewPager.setPagingEnabled(false);
                                        tabLayout.setVisibility(View.GONE);
                                    } else {
                                        if (mainCarIdCopy != -1) {
                                            for (Car car : carList) {
                                                if (car.getId() == mainCarIdCopy) {
                                                    car.setCurrentCar(true);
                                                    dashboardCar = car;
                                                }
                                            }
                                        } else {
                                            dashboardCar = carList.get(0);
                                            carList.get(0).setCurrentCar(true);
                                        }
                                        if (mainView != null) {
                                            mainView.setVisibility(View.VISIBLE);
                                        }
                                        if (noCarText != null) {
                                            noCarText.setVisibility(View.GONE);
                                        }
                                        if (noConnectText != null) {
                                            noConnectText.setVisibility(View.GONE);
                                        }
                                        if (requestServiceButton != null) {
                                            requestServiceButton.setVisibility(View.VISIBLE);
                                        }
                                        viewPager.setPagingEnabled(true);
                                        tabLayout.setVisibility(View.VISIBLE);
                                        callback.setDashboardCar(carList);
                                        carLocalStore.deleteAllCars();
                                        carLocalStore.storeCars(carList);
                                        carIssueLocalStore.deleteAllCarIssues();
                                        carIssueLocalStore.storeCarIssues(carList);

                                        // Populate the scanner table
                                        for (Car car : carList) { // populate scanner table with scanner ids associated with the cars
                                            if (!scannerLocalStore.isCarExist(car.getId())) {
                                                scannerLocalStore.storeScanner(new ObdScanner(car.getId(), car.getScannerId()));
                                                Log.d("Storing Scanner", car.getId() + " " + car.getScannerId());
                                            }
                                        }
                                        Log.d(TAG, "Size of the scanner table: " + scannerLocalStore.getTableSize());

                                        callback.setCarDetailsUI();
                                    }

                                    mainAppSideMenuAdapter.setData(carList.toArray(new Car[carList.size()]));
                                    mainAppSideMenuAdapter.notifyDataSetChanged();
                                    hideLoading();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    Toast.makeText(MainActivity.this,
                                            "Error retrieving car details.  Please check your internet connection.", Toast.LENGTH_SHORT).show();
                                    hideLoading();
                                }
                            } else {
                                hideLoading();
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
        if (state == IBluetoothCommunicator.DISCONNECTED) {
            Log.i(TAG, "Bluetooth disconnected");
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
    }

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {
    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {
    }

    @Override
    public void getIOData(final DataPackageInfo dataPackageInfo) {
        if (dataPackageInfo.dtcData != null && !dataPackageInfo.dtcData.isEmpty()) {

            final HashSet<String> activeIssueNames = new HashSet<>();

            if (dashboardCar == null) {
                return;
            }

            for (CarIssue issues : dashboardCar.getActiveIssues()) {
                activeIssueNames.add(issues.getItem());
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean newDtcFound = false;

                    if (dataPackageInfo.dtcData != null && dataPackageInfo.dtcData.length() > 0) {
                        String[] DTCs = dataPackageInfo.dtcData.split(",");
                        for (String dtc : DTCs) {
                            String parsedDtc = ObdDataUtil.parseDTCs(dtc);
                            if (!activeIssueNames.contains(parsedDtc)) {
                                newDtcFound = true;
                            }
                        }
                    }

                    if (newDtcFound) {
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
        if (loginPackageInfo.flag.
                equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            Log.i(TAG, "Device logout");
        }
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

    /**
     * Create and show an snackbar that is used to show users some information.<br>
     * The purpose of this method is to display message that requires user's confirm to be dismissed.
     *
     * @param content snack bar message
     */
    public void showSimpleMessage(@NonNull String content, boolean isSuccess) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), content, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // DO nothing
                    }
                })
                .setActionTextColor(Color.WHITE);
        View snackBarView = snackbar.getView();
        if (isSuccess) {
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.message_success));
        } else {
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.message_failure));
        }
        TextView textView = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white_text));

        snackbar.show();

    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {
        dashboardCar = carList.get(position);
        for (Car car : carList) {
            car.setCurrentCar(false);
        }
        dashboardCar.setCurrentCar(true);
        networkHelper.setMainCar(application.getCurrentUserId(), dashboardCar.getId(), null);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(MainDashboardFragment.pfCurrentCar, dashboardCar.getId()).commit();
        // Highlight the selected item, update the title, and close the drawer
        for (int i = 0; i < carList.size(); i++) {
            mDrawerList.setItemChecked(i, false);
        }
        mDrawerList.setItemChecked(position, true);
        callback.setDashboardCar(carList);
        callback.setCarDetailsUI();
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
        if (viewPager.getCurrentItem() == 0) {
            findViewById(R.id.main_view).startAnimation(AnimationUtils.loadAnimation(this, R.anim.switch_car));
        }
    }

    public void startAddCarActivity(View view) {
        Intent intent = new Intent(MainActivity.this, AddCarActivity.class);
        startActivityForResult(intent, RC_ADD_CAR);
        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == RC_LOCATION_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
     */
    public void requestMultiService(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Request Service",
                    viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                            MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // view is null for request from tutorial
        new ServiceRequestUtil(this, dashboardCar, view == null).start();
    }

    /**
     * Invoked when the message dealer button is tapped on the MainDashboardFragment
     *
     * @param view
     */
    public void startChat(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Confirm chat with " + dashboardCar.getDealership().getName(),
                    viewPager.getCurrentItem() == MainAppViewPager.PAGE_NUM_MAIN_DASHBOARD ?
                            MixpanelHelper.DASHBOARD_VIEW : MixpanelHelper.TOOLS_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final HashMap<String, Object> customProperties = new HashMap<>();
        customProperties.put("VIN", dashboardCar.getVin());
        customProperties.put("Car Make", dashboardCar.getMake());
        customProperties.put("Car Model", dashboardCar.getModel());
        customProperties.put("Car Year", dashboardCar.getYear());
        Log.i(TAG, dashboardCar.getDealership().getEmail());
        customProperties.put("Email", dashboardCar.getDealership().getEmail());
        User.getCurrentUser().addProperties(customProperties);
        if (application.getCurrentUser() != null) {
            customProperties.put("Phone", application.getCurrentUser().getPhone());
            User.getCurrentUser().setFirstName(application.getCurrentUser().getFirstName());
            User.getCurrentUser().setEmail(application.getCurrentUser().getEmail());
        }
        ConversationActivity.show(this);
        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }


    public void navigateToDealer(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Directions to " + dashboardCar.getDealership().getName(),
                    MixpanelHelper.TOOLS_VIEW);
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
            mixpanelHelper.trackButtonTapped("Confirm call to " + dashboardCar.getDealership().getName(),
                    MixpanelHelper.TOOLS_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                dashboardCar.getDealership().getPhone()));
        startActivity(intent);
        overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
    }

    /**
     * Given the tutorial should be shown to the user, show tutorial sequence
     */
    private void presentShowcaseSequence() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);

        final String prefDiscountAvailable = getResources().getString(R.string.pfFirstBookingDiscountAvailability);
        final String prefDiscountAmount = getResources().getString(R.string.pfFirstBookingDiscountAmount);
        final String prefDiscountUnit = getResources().getString(R.string.pfFirstBookingDiscountUnit);
        final boolean firstBookingDiscountAvailable = preferences.getBoolean(prefDiscountAvailable, false);

        Log.i(TAG, "running present show case");

        final MaterialShowcaseSequence discountSequence = new MaterialShowcaseSequence(this);

        try {
            mixpanelHelper.trackViewAppeared(MixpanelHelper.TUTORIAL_VIEW_APPEARED);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        StringBuilder firstServicePromotion = new StringBuilder();
        firstServicePromotion.append(getResources().getString(R.string.first_service_booking_1));

        if (firstBookingDiscountAvailable) {

            final float discountAmount = preferences.getFloat(prefDiscountAmount, 0f);
            final String discountUnit = preferences.getString(prefDiscountUnit, null);

            if (discountAmount != 0 && discountUnit != null) {
                firstServicePromotion.append(" You can also receive a discount of ");
                if (discountUnit.contains("%")) {
                    firstServicePromotion.append(discountAmount + discountUnit + " towards your first service");
                } else {
                    firstServicePromotion.append(discountUnit + (int) discountAmount + " towards your first service.");
                }
            }

            //change the preference of the first service booking
            preferences.edit().putBoolean(prefDiscountAvailable, false);
        }

        final MaterialShowcaseView firstBookingDiscountShowcase = new MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.request_service_btn))
                .setTitleText("Request Service")
                .setContentText(firstServicePromotion.toString())
                .setDismissOnTouch(true)
                .setDismissText("Get Started")
                .withRectangleShape(true)
                .setMaskColour(ContextCompat.getColor(this, R.color.darkBlueTrans))
                .build();


        final MaterialShowcaseView tentativeDateShowcase = new MaterialShowcaseView.Builder(this)
                .withoutShape()
                .setContentText(R.string.first_service_booking_2)
                .setDismissOnTouch(true)
                .setMaskColour(ContextCompat.getColor(this, R.color.darkBlueTrans))
                .setListener(new IShowcaseListener() {
                    @Override
                    public void onShowcaseDisplayed(MaterialShowcaseView materialShowcaseView) {
                    }

                    @Override
                    public void onShowcaseDismissed(MaterialShowcaseView materialShowcaseView) {
                        requestMultiService(null);
                    }
                })
                .build();

        discountSequence.addSequenceItem(firstBookingDiscountShowcase)
                .addSequenceItem(tentativeDateShowcase);

        discountSequence.setOnItemShownListener(new MaterialShowcaseSequence.OnSequenceItemShownListener() {
            @Override
            public void onShow(MaterialShowcaseView materialShowcaseView, int i) {
                if (materialShowcaseView.equals(firstBookingDiscountShowcase)) {
                    //update the local sharedPreference
                    preferences.edit().putBoolean(getString(R.string.pfTutorialShown), true).commit();

                    try {
                        Button requestServiceButton = ((Button) viewPager.findViewById(R.id.request_service_btn));
                        requestServiceButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.service_button_tutorial));
                        requestServiceButton.setText(getResources().getString(R.string.first_service_booking_tutorial_button_text));
                    } catch (NullPointerException npe) {
                        npe.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //Log tutorial GET_STARTED tapped
                    try {
                        mixpanelHelper.trackButtonTapped(MixpanelHelper.TUTORIAL_GET_STARTED_TAPPED, MixpanelHelper.DASHBOARD_VIEW);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        });

        discountSequence.setOnItemDismissedListener(new MaterialShowcaseSequence.OnSequenceItemDismissedListener() {
            @Override
            public void onDismiss(MaterialShowcaseView materialShowcaseView, int i) {

                if (!materialShowcaseView.equals(firstBookingDiscountShowcase)) return;

                try {
                    mixpanelHelper.trackButtonTapped("Tutorial - removeTutorial", MixpanelHelper.DASHBOARD_VIEW);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                com.pitstop.models.User user = application.getCurrentUser();

                final HashMap<String, Object> customProperties = new HashMap<>();
                customProperties.put("VIN", dashboardCar.getVin());
                customProperties.put("Car Make", dashboardCar.getMake());
                customProperties.put("Car Model", dashboardCar.getModel());
                customProperties.put("Car Year", dashboardCar.getYear());
                customProperties.put("Email", dashboardCar.getDealership().getEmail());

                if (user != null) {
                    customProperties.put("Phone", user.getPhone());
                    User.getCurrentUser().setFirstName(user.getFirstName());
                    User.getCurrentUser().setEmail(user.getEmail());
                }
                User.getCurrentUser().addProperties(customProperties);

//                if (user != null && !BuildConfig.DEBUG) {
                if (user != null) {
                    Log.d("MainActivity Smooch", "Sending message");
                    Smooch.getConversation().sendMessage(
                            new io.smooch.core.Message(user.getFirstName() +
                                    (user.getLastName() == null || user.getLastName().equals("null")
                                            ? "" : (" " + user.getLastName())) + " has signed up for Pitstop!"));
                }

                Smooch.track("User Logged In");

                //Change the color and text back to the original request service button
                try {
                    Button requestServiceButton = ((Button) viewPager.findViewById(R.id.request_service_btn));
                    requestServiceButton.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.color_button_rectangle_primary));
                    requestServiceButton.setText(getResources().getString(R.string.service_request_button));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        viewPager.setCurrentItem(0);
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));

        discountSequence.start();
    }

    /**
     * <p>This method is supposed to retrieve the necessary shop settings from the api and
     * stored them locally in the SharePreferences</p>
     * Including
     * <ul>
     * <li>boolean enableDiscountTutorial</li>
     * <li>float amount</li>
     * <li>String unit</li>
     * </ul>
     */
    private void prepareAndStartTutorialSequence() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
        Log.d("FSBretrieveUserSetting", LoginActivity.sState);
        Log.d("FSBretrieveUserSetting", "Is carListEmpty: " + carList.isEmpty());
        Log.d("FSB", "Start getting user settings");
        networkHelper.getUserSettingsById(application.getCurrentUserId(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {

                if (response != null) Log.d("FSB", response);
                if (requestError != null) Log.d("FSB", requestError.toString());

                if (isLoading) hideLoading();

                if (requestError == null && response != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.has("shop")) {
                            Log.d("FSB", "Response has shop");
                            JSONObject shop = jsonObject.getJSONObject("shop");
                            JSONObject firstAppointmentDiscount = shop.getJSONObject("firstAppointmentDiscount");
                            boolean enableDiscountTutorial = shop.getBoolean("enableDiscountTutorial");
                            float amount = (float) firstAppointmentDiscount.getDouble("amount");
                            String unit = firstAppointmentDiscount.getString("unit");


                            preferences.edit()
                                    .putBoolean(getString(R.string.pfFirstBookingDiscountAvailability), enableDiscountTutorial)
                                    .putFloat(getString(R.string.pfFirstBookingDiscountAmount), amount)
                                    .putString(getString(R.string.pfFirstBookingDiscountUnit), unit)
                                    .commit();

                        }
                    } catch (JSONException je) {
                        je.printStackTrace();
                        Log.d(TAG, "Error occurred in retrieving first service booking promotion");
                    }
                } else {
                    Log.e(TAG, "Login: " + requestError.getError() + ": " + requestError.getMessage());
                }

                //Show the tutorial
                try {
                    presentShowcaseSequence();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Invoked when broadcast receiver receives ACTION_UNRECOGNIZED_OBD_MODULE_DISCOVERED intnet
     */
    private void showPairDeviceWithCarDialog() {
        try {
            mixpanelHelper.trackViewAppeared(MixpanelHelper.UNRECOGNIZED_MODULE_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Dashboard fragment handler select car message sent");
        mDashboardFragment.selectCarForUnrecognizedModule();
    }

    /**
     * Register all actions, invoked at onStart()
     */
    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UNRECOGNIZED_OBD_MODULE_DISCOVERED);
        intentFilter.addAction(ACTION_PAIRING_MODULE_ID_INVALID);
        intentFilter.addAction(ACTION_PAIRING_MODULE_SUCCESS);
        intentFilter.addAction(ACTION_PAIRING_MODULE_NETWORK_ERROR);
        this.registerReceiver(mBroadcastReceiver, intentFilter);
    }

    /**
     * unregister broadcast receiver, invoked at onStop()
     */
    private void unregisterBroadcastReceiver() {
        this.unregisterReceiver(mBroadcastReceiver);
    }

    public interface MainDashboardCallback {
        void activityResultCallback(int requestCode, int resultCode, Intent data);

        void onServerRefreshed();

        void onLocalRefreshed();

        void setDashboardCar(List<Car> carList);

        void setCarDetailsUI();

        void selectCarForUnrecognizedModule();
    }

    private void logScannerTable(){
        List<ObdScanner> scanners = scannerLocalStore.getAllScanners();
        Log.i(TAG + " Scanner", "Scanner Table");
        for (ObdScanner scanner : scanners){
            Log.i("Scanner table", "Car ID: " + scanner.getCarId());
            Log.i("Scanner table", "Device Name: " + scanner.getDeviceName());
            Log.i("Scanner table", "Device ID: " + scanner.getScannerId());
        }
    }

}
