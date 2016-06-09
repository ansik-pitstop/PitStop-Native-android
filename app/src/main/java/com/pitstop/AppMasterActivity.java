package com.pitstop;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.IntentProxyObject;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarIssueAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalShopAdapter;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestError;
import com.pitstop.application.GlobalApplication;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.utils.CarDataManager;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by David on 6/8/2016.
 */
public class AppMasterActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener,
        EasyPermissions.PermissionCallbacks {

    public static List<Car> carList = new ArrayList<>();


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
    private CharSequence mDrawerTitle="Swag";
    private CharSequence mTitle="swaggier";
    private MixpanelHelper mixpanelHelper;
    private Car dashboardCar;

    private NetworkHelper networkHelper;
    public static MainDashboardCallback callback;



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

        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        networkHelper = new NetworkHelper(getApplicationContext());
        CarDataManager carDataManager = new CarDataManager();
        dashboardCar = carDataManager.getDashboardCar();

        // Local db adapters
        carLocalStore = new LocalCarAdapter(this);
        carIssueLocalStore = new LocalCarIssueAdapter(this);
        shopLocalStore = new LocalShopAdapter(this);

        List<Car> cars = carLocalStore.getAllCars();
        mDrawerTitles = new String[cars.size()];
        for (int i = 0; i<cars.size();i++){
            Car car = cars.get(i);
            mDrawerTitles[i]=car.getMake()+" " + car.getModel();
        }

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);


        // Set the adapter for the list view
//        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
//                R.layout.drawer_list_item, mPlanetTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
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

        // Always refresh from the server if entering from log in activity
        if(getIntent().getBooleanExtra(SplashScreen.LOGIN_REFRESH, false)) {
            Log.i(TAG, "refresh from login");
            refreshFromServer();
        } else if(SelectDealershipActivity.ACTIVITY_NAME.equals(getIntent().getStringExtra(FROM_ACTIVITY))) {
            // In the event the user pressed back button while in the select dealership activity
            // then load required data from local db.
            refreshFromLocal();
        } else if(PitstopPushBroadcastReceiver.ACTIVITY_NAME.equals(getIntent().getStringExtra(FROM_ACTIVITY))) {
            // On opening a push notification, load required data from server
            refreshFromServer();
        } else if(getIntent().getBooleanExtra(FROM_NOTIF, false)) {
            refreshFromServer();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int id = item.getItemId();

        if(id == R.id.refresh_main) {
            try {
                mixpanelHelper.trackButtonTapped("Refresh", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            refreshFromServer();

            if(autoConnectService.getState() == IBluetoothCommunicator.DISCONNECTED) {
                autoConnectService.startBluetoothSearch();
            }
        } else if(id == R.id.add) {
            try {
                mixpanelHelper.trackButtonTapped("Add Car", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            startAddCarActivity();
        } else if(id == R.id.action_settings) {
            try {
                mixpanelHelper.trackButtonTapped("Settings", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            sharedPreferences.edit().putBoolean(REFRESH_FROM_SERVER, true).apply();

            final Intent intent = new Intent(AppMasterActivity.this, SettingsActivity.class);

            IntentProxyObject proxyObject = new IntentProxyObject();

            proxyObject.setCarList(carList);
            intent.putExtra(CAR_LIST_EXTRA,proxyObject);

            final GlobalApplication application  = ((GlobalApplication)getApplication());
            if(application.getCurrentUser() == null) {
                networkHelper.getUser(application.getCurrentUserId(), new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null) {
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

        } else if(id == R.id.action_car_history) {
            try {
                mixpanelHelper.trackButtonTapped("History", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(AppMasterActivity.this, CarHistoryActivity.class);
            //intent.putExtra("carId",dashboardCar.getId());
            intent.putExtra("dashboardCar", dashboardCar);
            startActivity(intent);
        }
        return true;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }


    public void refreshFromServer() {
        carLocalStore.deleteAllCars();
        carIssueLocalStore.deleteAllCarIssues();
        if(callback!=null) {
            callback.onServerRefreshed();
        }
    }

    public void refreshFromLocal() {
        if(callback!=null) {
            callback.onLocalRefreshed();
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        // Create a new fragment and specify the planet to show based on position
        Fragment fragment = new MainDashboardFragment();
        Bundle args = new Bundle();
//        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mDrawerTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }


    public void startAddCarActivity() {
        Intent intent = new Intent(AppMasterActivity.this, AddCarActivity.class);
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
    }
}
