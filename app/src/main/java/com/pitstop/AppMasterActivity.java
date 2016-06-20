package com.pitstop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.DataAccessLayer.DTOs.IntentProxyObject;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarIssueAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalShopAdapter;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestError;
import com.pitstop.adapters.MainAppSideMenuAdapter;
import com.pitstop.adapters.MainAppViewPagerAdaper;
import com.pitstop.application.GlobalApplication;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.fragments.MainDashboardFragment;
import com.pitstop.utils.CarDataManager;
import com.pitstop.fragments.MainToolFragment;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by David on 6/8/2016.
 */
public class AppMasterActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener,
        EasyPermissions.PermissionCallbacks {

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

    public static final String TAG = "AppMasterActivity";
    private static final int LOC_PERM_REQ = 112;
    private static final int RC_LOCATION_PERM = 101;
    public static final String[] LOC_PERMS = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};


    private String[] mDrawerTitles;
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle="Your Vehicles";
    private CharSequence mTitle="Pitstop";
    private MixpanelHelper mixpanelHelper;
    private  Toolbar toolbar;
    private ViewPager viewPager;
    private TabLayout tabLayout;
    private Car dashboardCar;
    private GlobalApplication application;

    private NetworkHelper networkHelper;
    public static MainDashboardCallback callback;



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
            autoConnectService.setCallbacks(AppMasterActivity.this);

            // Send request to user to turn on bluetooth if disabled
            if (BluetoothAdapter.getDefaultAdapter()!=null) {
                if(EasyPermissions.hasPermissions(AppMasterActivity.this, LOC_PERMS)) {
                    autoConnectService.startBluetoothSearch();
                } else {
                    EasyPermissions.requestPermissions(AppMasterActivity.this,
                            getString(R.string.location_request_rationale), RC_LOCATION_PERM, LOC_PERMS);
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
        setContentView(R.layout.activity_main_drawer_frame);
        serviceIntent= new Intent(AppMasterActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);

        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        networkHelper = new NetworkHelper(getApplicationContext());
        CarDataManager carDataManager = new CarDataManager();
        dashboardCar = carDataManager.getDashboardCar();

        // Local db adapters
        carLocalStore = new LocalCarAdapter(this);
        carIssueLocalStore = new LocalCarIssueAdapter(this);
        shopLocalStore = new LocalShopAdapter(this);

        viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
    }

    private void setupViewPager(ViewPager viewPager) {
        MainAppViewPagerAdaper adapter = new MainAppViewPagerAdaper(getSupportFragmentManager());
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

        carList = carLocalStore.getAllCars();
        if(carList.size()==0&&check){
            refreshFromServer();
        }
        mDrawerTitles = new String[carList.size()];
        for (int i = 0; i<carList.size();i++){
            Car car = carList.get(i);
            mDrawerTitles[i]=car.getMake()+" " + car.getModel();
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer_listview);

        if(mDrawerList!=null) {
            // Set the adapter for the list view
            mDrawerList.setAdapter(new MainAppSideMenuAdapter(this,
                    mDrawerTitles));
//         Set the list's click listener
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
            if (carList.size() > 0) {
                callback.setDashboardCar(AppMasterActivity.carList);
                callback.setCarDetailsUI();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "onResume");

        try {
            mixpanelHelper.trackViewAppeared(TAG);
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
        startAddCarActivity(true);
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

        final Intent intent = new Intent(AppMasterActivity.this, SettingsActivity.class);

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
                    } else {
                        Log.e(TAG, "Get user error: " + requestError.getMessage());
                    }
                }
            });
        } else {
            startActivityForResult(intent, RC_SETTINGS);
        }
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int id = item.getItemId();

//        if (id == R.id.action_car_history) {
//            try {
//                mixpanelHelper.trackButtonTapped("History", TAG);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            Intent intent = new Intent(AppMasterActivity.this, CarHistoryActivity.class);
//            //intent.putExtra("carId",dashboardCar.getId());
//            intent.putExtra("dashboardCar", dashboardCar);
//            startActivity(intent);
//        }
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

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }


    public void refreshFromServer() {
        Log.d("random","refresh called");
        carLocalStore.deleteAllCars();
        carIssueLocalStore.deleteAllCarIssues();
        carIssueList.clear();
        getCarDetails();
        if(isLoading) {
            hideLoading();
        }
        if(callback!=null) {

            callback.onServerRefreshed();
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
            AppMasterActivity.carList = localCars;

            callback.setDashboardCar(AppMasterActivity.carList);
            callback.setCarDetailsUI();
            hideLoading();
        }
    }

    /** Call function to retrieve live data from parse
     * @see #getCarDetails()
     * */
    private void loadCarDetailsFromServer() {
        int userId = ((GlobalApplication)getApplication()).getCurrentUserId();

        networkHelper.getCarsByUserId(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(requestError == null) {
                    try {
                        AppMasterActivity.carList = Car.createCarsList(response);

                        if(AppMasterActivity.carList.isEmpty()) {
                            if (isLoading) {
                                hideLoading();
                            }
                            startAddCarActivity(false);
                        } else {
                            callback.setDashboardCar(AppMasterActivity.carList);
                            carLocalStore.deleteAllCars();
                            carLocalStore.storeCars(AppMasterActivity.carList);
                            callback.setCarDetailsUI();
                            resetMenus(false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getBaseContext(),
                                "Error retrieving car details", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Load cars error: " + requestError.getMessage());
                    Toast.makeText(getBaseContext(),
                            "Error retrieving car details", Toast.LENGTH_SHORT).show();
                }
            }
        });
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

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(MainDashboardFragment.pfCurrentCar, carList.get(position).getId()).apply();
        callback.setDashboardCar(carList);
        callback.setCarDetailsUI();
        mDrawerLayout.closeDrawer(findViewById(R.id.left_drawer));
    }


    public void startAddCarActivity(boolean hasCar) {
        Intent intent = new Intent(AppMasterActivity.this, AddCarActivity.class);
        intent.putExtra(AppMasterActivity.HAS_CAR_IN_DASHBOARD, hasCar);
        startActivity(intent);
    }


    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions,
                                            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if(autoConnectService != null) {
            autoConnectService.startBluetoothSearch();
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

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    @Override
    public void getBluetoothState(int state) {

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
    public void getIOData(DataPackageInfo dataPackageInfo) {

    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }

    public interface MainDashboardCallback{
        public void activityResultCallback(int requestCode, int resultCode, Intent data);
        void onServerRefreshed();
        void onLocalRefreshed();

        void setDashboardCar(List<Car> carList);

        void setCarDetailsUI();
    }
}
