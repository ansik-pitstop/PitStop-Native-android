package com.pitstop.ui;

import android.app.Activity;
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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.SaveCallback;
import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.adapters.TabViewPagerAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.models.Dealership;
import com.pitstop.models.IntentProxyObject;
import com.pitstop.models.ObdScanner;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.add_car.PromptAddCarActivity;
import com.pitstop.ui.mainFragments.MainDashboardCallback;
import com.pitstop.ui.mainFragments.MainDashboardFragment;
import com.pitstop.ui.mainFragments.MainFragmentCallback;
import com.pitstop.ui.my_appointments.MyAppointmentActivity;
import com.pitstop.ui.scan_car.ScanCarFragment;
import com.pitstop.ui.service_request.ServiceRequestActivity;
import com.pitstop.ui.services.MainServicesFragment;
import com.pitstop.ui.upcoming_timeline.TimelineActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MigrationService;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.smooch.core.Smooch;
import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

;

/**
 * Created by David on 6/8/2016.
 */
public class MainActivity extends IBluetoothServiceActivity implements ObdManager.IBluetoothDataListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    //Bind floating action buttons
    @BindView(R.id.fab_main)
    FloatingActionButton fabMain;

    @BindView(R.id.fab_call)
    FloatingActionButton fabCall;

    @BindView(R.id.fab_find_directions)
    FloatingActionButton fabDirections;

    @BindView(R.id.fab_menu_request_service)
    FloatingActionButton fabRequestService;

    @BindView(R.id.fab_menu_message)
    FloatingActionButton fabMessage;

    private GlobalApplication application;
    private boolean serviceIsBound;
    private boolean isFirstAppointment = false;
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
                final String[] locationPermissions = getResources().getStringArray(R.array.permissions_location);
                for (String permission : locationPermissions) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                        requestPermission(MainActivity.this, locationPermissions, RC_LOCATION_PERM,
                                true, getString(R.string.request_permission_location_message));
                        return;
                    }
                }
                if (autoConnectService.getState() == IBluetoothCommunicator.DISCONNECTED) {
                    autoConnectService.startBluetoothSearch(); // refresh clicked
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
    //private List<CarIssue> carIssueList = new ArrayList<>();
    private Car dashboardCar;

    // Database accesses
    private LocalCarAdapter carLocalStore;
    //private LocalCarIssueAdapter carIssueLocalStore;
    private LocalShopAdapter shopLocalStore;
    private LocalScannerAdapter scannerLocalStore;

    //tabs

    public static final String[] TAB_NAMES = {"Dashboard","Services","Scan","Notifications"};

    public static final int TAB_DASHBOARD = 0;
    public static final int TAB_SERVICES = 1;
    public static final int TAB_SCAN = 2;
    public static final int TAB_NOTIF = 3;

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

    private final int FAB_DELAY = 50;

    public static final int RC_LOCATION_PERM = 101;
    public static final String[] LOC_PERMS = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    // Views
    private View rootView;
    private Toolbar toolbar;

    private ViewPager viewPager;
    private TabViewPagerAdapter mTabViewPagerAdapter;

    private ProgressDialog progressDialog;
    private boolean isLoading = false;

    // Utils / Helper
    private MixpanelHelper mixpanelHelper;
    private NetworkHelper networkHelper;

    private boolean isRefreshingFromServer = false;
    private boolean isFabOpen = false;

    public static MainDashboardCallback mainDashboardCallback;
    public static MainFragmentCallback servicesCallback;
    public static MainFragmentCallback scanCallback;

    private MaterialShowcaseSequence tutorialSequence;

    private int attachedFragmentCounter = 0;
    private final int TOTAL_WORKING_FRAGMENT_NUM = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        networkHelper = new NetworkHelper(getApplicationContext());

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(MigrationService.notificationId);

        rootView = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(rootView);
        ButterKnife.bind(this);
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
        toggleConnectionStatusActionBar(false);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        // Local db adapters
        carLocalStore = new LocalCarAdapter(application);
        shopLocalStore = new LocalShopAdapter(application);
        scannerLocalStore = new LocalScannerAdapter(application);

        refreshFromServer();

        logAuthInfo();
        getSupportFragmentManager().beginTransaction().add(R.id.main_container, new MainDashboardFragment()).commit();

        setTabUI();
        setFabUI();
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

    //Set up fab, and fab menu click listener and the animations that will be taking place
    private void setFabUI(){

        final ArrayList<Animation> open_anims = new ArrayList<>();
        final ArrayList<Animation> close_anims = new ArrayList<>();

        //Add delay between animations of each FAB to avoid performance decrease
        for (int i=0;i<4;i++){
            Animation fab_open = AnimationUtils.loadAnimation(getApplication(), R.anim.fab_open);
            fab_open.setStartOffset((4-i)*FAB_DELAY);
            open_anims.add(fab_open);

            Animation fab_close = AnimationUtils.loadAnimation(getApplication(), R.anim.fab_close);
            fab_close.setStartOffset(i*FAB_DELAY);
            close_anims.add(fab_close);
        }

        final Animation rotate_forward = AnimationUtils.loadAnimation(getApplication(),R.anim.rotate_forward);
        final Animation rotate_backward = AnimationUtils.loadAnimation(getApplication(),R.anim.rotate_backward);

        fabMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mixpanelHelper.trackFabClicked("Main");
                if(isFabOpen){

                    fabMain.startAnimation(rotate_backward);
                    //Begin closing animation
                    fabDirections.startAnimation(close_anims.get(0));
                    fabCall.startAnimation(close_anims.get(1));
                    fabMessage.startAnimation(close_anims.get(2));
                    fabRequestService.startAnimation(close_anims.get(3));

                    //Don't let the user click
                    fabCall.setClickable(false);
                    fabRequestService.setClickable(false);
                    fabDirections.setClickable(false);
                    fabMessage.setClickable(false);

                    isFabOpen = false;

                } else {

                    //Begin opening animation
                    fabMain.startAnimation(rotate_forward);
                    fabDirections.startAnimation(open_anims.get(0));
                    fabCall.startAnimation(open_anims.get(1));
                    fabMessage.startAnimation(open_anims.get(2));
                    fabRequestService.startAnimation(open_anims.get(3));

                    //Let the user click fab
                    fabCall.setClickable(true);
                    fabRequestService.setClickable(true);
                    fabDirections.setClickable(true);
                    fabMessage.setClickable(true);

                    isFabOpen = true;

                }
            }
        });

        final Activity thisActivity = this;
        //Begin message activity
        fabMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mixpanelHelper.trackFabClicked("Message");
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
                ConversationActivity.show(thisActivity);
            }
        });

        //Begin request service activity
        fabRequestService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mixpanelHelper.trackFabClicked("Request Service");
                final Intent intent = new Intent(getBaseContext(), ServiceRequestActivity.class);
                intent.putExtra(ServiceRequestActivity.EXTRA_CAR, dashboardCar);
                intent.putExtra(ServiceRequestActivity.EXTRA_FIRST_BOOKING, false);
                startActivityForResult(intent, RC_REQUEST_SERVICE);
            }
        });

        //Begin call activity
        fabCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mixpanelHelper.trackFabClicked("Call");
                mixpanelHelper.trackButtonTapped("Confirm call to " + dashboardCar.getDealership().getName(),
                        MixpanelHelper.TOOLS_VIEW);
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                        dashboardCar.getDealership().getPhone()));
                startActivity(intent);
            }
        });

        //Begin directions activity
        fabDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mixpanelHelper.trackFabClicked("Directions");
                mixpanelHelper.trackButtonTapped("Directions to " + dashboardCar.getDealership().getName(),
                        MixpanelHelper.TOOLS_VIEW);

                String uri = String.format(Locale.ENGLISH,
                        "http://maps.google.com/maps?daddr=%s",
                        dashboardCar.getDealership().getAddress());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        });
    }

    private void setTabUI(){

        //Initialize tab navigation
        //View pager adapter that returns the corresponding fragment for each page
        mTabViewPagerAdapter = new TabViewPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.main_container);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch(position){
                    case TAB_DASHBOARD:
                        mixpanelHelper.trackSwitchedToTab("Dashboard");
                        break;
                    case TAB_SERVICES:
                        mixpanelHelper.trackSwitchedToTab("Services");
                        break;
                    case TAB_SCAN:
                        mixpanelHelper.trackSwitchedToTab("Scan");
                        break;
                    case TAB_NOTIF:
                        mixpanelHelper.trackSwitchedToTab("Notifications");
                        break;
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        //Set up actionbar
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //Change actionbar title
                getSupportActionBar().setTitle(TAB_NAMES[position]);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        viewPager.setAdapter(mTabViewPagerAdapter);

        //Populate tabs with icons
        TabLayout tabLayout = (TabLayout) findViewById(R.id.main_tablayout);
        tabLayout.setupWithViewPager(viewPager);

        int[] tabIcons = {R.drawable.ic_dashboard,R.drawable.history
                ,R.drawable.scan_icon,R.drawable.ic_notifications_white_24dp};

        for (int i=0;i<tabIcons.length;i++){
            try{
                tabLayout.getTabAt(i).setIcon(tabIcons[i]);
            }catch(java.lang.NullPointerException e){

            }
        }

        //Switch to selected fragment when tab is clicked
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){

                    case TAB_DASHBOARD:
                        //Go to dashboard fragment
                        viewPager.setCurrentItem(TAB_DASHBOARD);
                        break;

                    case TAB_SERVICES:
                        //Go to services fragment
                        viewPager.setCurrentItem(TAB_SERVICES);
                        break;

                    case TAB_SCAN:
                        //Go to scan fragment
                        viewPager.setCurrentItem(TAB_SCAN);
                        break;

                    case TAB_NOTIF:
                        //Go to notifications fragment
                        viewPager.setCurrentItem(TAB_NOTIF);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "onResume");

        if (autoConnectService != null) autoConnectService.setCallbacks(this);

        resetMenus(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    // repopulate car list
    public void resetMenus(boolean refresh) {
        Log.d(TAG, "Car List Size: " + carList.size());
        Log.d(TAG, "Refresh: " + refresh);
        if (carList.size() == 0 && refresh) {
            Log.d(TAG, "Refresh from server");
            refreshFromServer();
        }

        int id = PreferenceManager.getDefaultSharedPreferences(this).getInt(MainDashboardFragment.pfCurrentCar, 0);
        Log.d(TAG, "Current car id: " + id);
        dashboardCar = null;
        if (carList.size() > 0) {
            for (Car car : carList) {
                if (car.getId() == id) {
                    Log.d(TAG, "Set to id: " + id);
                    dashboardCar = car;
                    car.setCurrentCar(true);
                }
            }
            if (dashboardCar == null) {
                Log.d(TAG, "Set to first Car");
                carList.get(0).setCurrentCar(true);
                dashboardCar = carList.get(0);
            }

            broadCastCarDataToFragments();
            mainDashboardCallback.setCarDetailsUI(); //Keep this here for now, needs to be moved later
            loadDealershipCustomDesign();

        }
    }

    private void broadCastCarDataToFragments(){
        MainDashboardFragment.setDashboardCar(getCurrentCar());

        //Check whether fragment has been instantiated, if not then it'll grab dashboard car from onCreateView()
        if (mainDashboardCallback != null){
            mainDashboardCallback.onDashboardCarUpdated();
        }

        MainServicesFragment.setDashboardCar(getCurrentCar());
        //Check whether fragment has been instantiated, if not then it'll grab dashboard car from onCreateView()
        if (servicesCallback != null){
            servicesCallback.onDashboardCarUpdated();
        }

        ScanCarFragment.setDashboardCar(getCurrentCar());
        //Check whether fragment has been instantiated, if not then it'll grab dashboard car from onCreateView()
        if (scanCallback != null){
            scanCallback.onDashboardCarUpdated();
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

    private Car getCurrentCar(){
        if (carList == null){
            return null;
        }
        if (carList.size() == 0){
            return null;
        }

        for (Car c: carList){
            if (c.isCurrentCar()){
                return c;
            }
        }
        return carList.get(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");

        if (data != null) {
            boolean shouldRefreshFromServer = data.getBooleanExtra(REFRESH_FROM_SERVER, false);

            if (requestCode == RC_ADD_CAR) {
                if (resultCode == AddCarActivity.ADD_CAR_SUCCESS || resultCode == AddCarActivity.ADD_CAR_NO_DEALER_SUCCESS) {
                    Car addedCar = data.getParcelableExtra(CAR_EXTRA);
                    Log.d("OnActivityResult", "CarList: " + carList.size());
                    if (carList.size() == 0) { // first car
                        dashboardCar = addedCar;
                        carList.add(dashboardCar);
                        dashboardCar.setCurrentCar(true);
                        broadCastCarDataToFragments();

                        PreferenceManager.getDefaultSharedPreferences(this).edit()
                                .putInt(MainDashboardFragment.pfCurrentCar, dashboardCar.getId()).apply();

                        com.pitstop.models.User user = application.getCurrentUser();

                        final HashMap<String, Object> customProperties = new HashMap<>();
                        customProperties.put("VIN", addedCar.getVin());
                        Log.d(TAG, addedCar.getVin());
                        customProperties.put("Car Make", addedCar.getMake());
                        Log.d(TAG, addedCar.getMake());
                        customProperties.put("Car Model", addedCar.getModel());
                        Log.d(TAG, addedCar.getModel());
                        customProperties.put("Car Year", addedCar.getYear());
                        Log.d(TAG, String.valueOf(addedCar.getYear()));

                        if (resultCode == AddCarActivity.ADD_CAR_SUCCESS) {
                            customProperties.put("Email", addedCar.getDealership().getEmail());
                            Log.d(TAG, addedCar.getDealership().getEmail());
                        }

                        if (user != null) {
                            customProperties.put("Phone", user.getPhone());
                            User.getCurrentUser().setFirstName(user.getFirstName());
                            User.getCurrentUser().setEmail(user.getEmail());
                        }

                        User.getCurrentUser().addProperties(customProperties);

                        if (user != null) {
                            Log.d("MainActivity Smooch", "Sending message");
                            Smooch.getConversation().sendMessage(new io.smooch.core.Message(user.getFirstName() +
                                    (user.getLastName() == null || user.getLastName().equals("null") ?
                                            "" : (" " + user.getLastName())) + " has signed up for Pitstop!"));
                        }

                        Smooch.track("User Logged In");

                        if (resultCode == AddCarActivity.ADD_CAR_SUCCESS) {
                            prepareAndStartTutorialSequence();
                        }
                    }
                    if (shouldRefreshFromServer) {
                        refreshFromServer();
                    }
                } else {
                    mixpanelHelper.trackButtonTapped("Cancel in Add Car", "Add Car");
                }
            } else if (requestCode == RC_SCAN_CAR && resultCode == RESULT_OK) {
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if (requestCode == RC_SETTINGS && resultCode == RESULT_OK) {
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
                mainDashboardCallback.setCarDetailsUI();
                loadDealershipCustomDesign();

            } else if (requestCode == RC_DISPLAY_ISSUE && resultCode == RESULT_OK) {
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if (requestCode == RC_ADD_CUSTOM_ISSUE && resultCode == RESULT_OK) {
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if (resultCode == AddCarActivity.PAIR_CAR_SUCCESS) {
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if (requestCode == RC_REQUEST_SERVICE){
                if (shouldRefreshFromServer) {
                    refreshFromServer();
                }
                boolean shouldRemoveTutorial = data.getBooleanExtra(REMOVE_TUTORIAL_EXTRA, false);
                if (shouldRemoveTutorial) {
                    removeTutorial();
                }
            }
            mainDashboardCallback.activityResultCallback(requestCode, resultCode, data);
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
        if (tutorialSequence != null && tutorialSequence.hasStarted()) {
            new AnimatedDialogBuilder(this)
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(getString(R.string.first_service_booking_cancel_title))
                    .setMessage(getString(R.string.first_service_booking_cancel_message))
                    .setNegativeButton("Continue booking", null) // Do nothing on continue
                    .setPositiveButton("Quit booking", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                JSONObject properties = new JSONObject();
                                properties.put("Button", "Cancel Service Request");
                                properties.put("State", "Tentative");
                                properties.put("View", MixpanelHelper.DASHBOARD_VIEW);
                                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            tutorialSequence.dismissAllItems();
                            try {
                                JSONObject properties = new JSONObject();
                                properties.put("Button", "Confirm Service Request");
                                properties.put("State", "Tentative");
                                properties.put("View", MixpanelHelper.DASHBOARD_VIEW);
                                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    })
                    .show();
            return;
        }

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
            case R.id.action_refresh:
                refreshClicked(null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    /**
     * Onclick method for service history button
     *
     * @param view
     */
    public void clickServiceHistory(View view) {
        mixpanelHelper.trackButtonTapped("History", MixpanelHelper.TOOLS_VIEW);
        Intent intent = new Intent(MainActivity.this, CarHistoryActivity.class);
        intent.putExtra(MainActivity.CAR_EXTRA, dashboardCar);
        startActivity(intent);
    }

    public void upcomingServiceClicked(View view){
        mixpanelHelper.trackButtonTapped("Upcoming", MixpanelHelper.TOOLS_VIEW);
        Intent carTimelineIntent = new Intent(this, TimelineActivity.class);
        carTimelineIntent.putExtra(TimelineActivity.CAR_BUNDLE_KEY, dashboardCar);
        startActivity(carTimelineIntent);
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
            carLocalStore.deleteAllCars();

            getCarDetails();
        } else {
            View linearLayout = findViewById(R.id.linear_layout);
            if (linearLayout != null) {
                Snackbar.make(linearLayout, "You are not connected to internet", Snackbar.LENGTH_SHORT).show();
            }
            refreshFromLocal();
            resetMenus(false);
            if (isLoading) {
                hideLoading();
            }
        }
    }

    public void refreshFromLocal() {
        //carIssueList.clear();
        getCarDetails();
        if (isLoading) {
            hideLoading();
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
            broadCastCarDataToFragments();

            hideLoading();
        }

        //After loading car details

    }

    private void startPromptAddCarActivity() {
        Intent intent = new Intent(MainActivity.this, PromptAddCarActivity.class);
        //Don't allow user to come back to tabs without first setting a car
        startActivityForResult(intent, RC_ADD_CAR);
    }

    private void loadDealershipCustomDesign(){
        Car myCar = getCurrentCar();
        //Update tab design to the current dealerships custom design if applicable
        if (myCar.getDealership() != null){
            if (BuildConfig.DEBUG && (myCar.getDealership().getId() == 4
                    || myCar.getDealership().getId() == 18)){

                bindMercedesDealerUI();
            }else if (!BuildConfig.DEBUG && myCar.getDealership().getId() == 14) {
                bindMercedesDealerUI();
            }
            else{
                bindDefaultDealerUI();
            }
        }
    }

    private void bindMercedesDealerUI(){
        TabLayout tabLayout = (TabLayout)findViewById(R.id.main_tablayout);
        tabLayout.setBackgroundColor(Color.BLACK);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        appBarLayout.setBackgroundColor(Color.DKGRAY);
    }

    private void bindDefaultDealerUI(){
        //Change theme elements back to default
        changeTheme(false);

        //Get the themes default primary color
        TypedValue defaultColor = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, defaultColor, true);

        //Set other changed UI elements back to original color
        ((TabLayout)findViewById(R.id.main_tablayout)).setBackgroundColor(defaultColor.data);
        ((AppBarLayout) findViewById(R.id.appbar)).setBackgroundColor(defaultColor.data);
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
                    application.logOutUser();
                    Toast.makeText(application, "Your session has expired.  Please login again.", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (response == null || response.isEmpty() || requestError != null) { // couldn't get cars from server, show try again

                    application.logOutUser();
                    Toast.makeText(application, "Networking error, please check internet connection.", Toast.LENGTH_LONG).show();
                    finish();

                } else {
                    int mainCarId = -1;
                    try {
                        mainCarId = new JSONObject(response).getJSONObject("settings").getInt("mainCar");
                    } catch (JSONException e) {

                    }

                    final int mainCarIdCopy = mainCarId;

                    networkHelper.getCarsByUserId(userId, new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            if (requestError == null) {
                                try {
                                    carList = Car.createCarsList(response);

                                    if (carList.isEmpty()) { // show add first car text
                                        startPromptAddCarActivity();

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

                                        broadCastCarDataToFragments();
                                        mainDashboardCallback.setCarDetailsUI();
                                        loadDealershipCustomDesign();

                                        carLocalStore.deleteAllCars();
                                        carLocalStore.storeCars(carList);

                                        // Populate the scanner table
                                        for (Car car : carList) { // populate scanner table with scanner ids associated with the cars
                                            if (!scannerLocalStore.isCarExist(car.getId())) {
                                                scannerLocalStore.storeScanner(new ObdScanner(car.getId(), car.getScannerId()));
                                                Log.d("Storing Scanner", car.getId() + " " + car.getScannerId());
                                            }
                                        }
                                        Log.d(TAG, "Size of the scanner table: " + scannerLocalStore.getTableSize());

                                    }

                                    resetMenus(false);
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
    public void tripData(TripInfoPackage tripInfoPackage) {
        if (mainDashboardCallback != null)
            mainDashboardCallback.tripData(tripInfoPackage);
    }

    @Override
    public void parameterData(ParameterPackage parameterPackage) {

    }

    @Override
    public void pidData(PidPackage pidPackage) {

    }

    @Override
    public void dtcData(final DtcPackage dtcPackage) {
        Log.i(TAG, "DTC data received: " + dtcPackage.dtcNumber);
        if(dtcPackage.dtcs != null) {
            final HashSet<String> activeIssueNames = new HashSet<>();

            if(dashboardCar == null) {
                return;
            }

            for(CarIssue issues : dashboardCar.getActiveIssues()) {
                activeIssueNames.add(issues.getItem());
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean newDtcFound = false;

                    if(dtcPackage.dtcs.length > 0){
                        for(String dtc : dtcPackage.dtcs) {
                            if(!activeIssueNames.contains(dtc)) {
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
    public void ffData(FreezeFramePackage ffPackage) {

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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == RC_LOCATION_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (autoConnectService.getState() == IBluetoothCommunicator.DISCONNECTED) {
                    autoConnectService.startBluetoothSearch();
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
     * Onclick method for Scan the Vehicle button
     *
     * @param view
     */
    public void scanClicked(View view) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putBoolean(MainActivity.REFRESH_FROM_SERVER, true).apply();

        Intent intent = new Intent(this, ScanCarFragment.class);
        intent.putExtra(MainActivity.CAR_EXTRA, dashboardCar);
        startActivityForResult(intent, MainActivity.RC_SCAN_CAR);
    }

    /**
     * Onclick method for Refresh button
     *
     * @param view
     */
    public void refreshClicked(View view) {
        refreshFromServer();

        if (autoConnectService.getState() == IBluetoothCommunicator.DISCONNECTED) {
            autoConnectService.startBluetoothSearch(); // refresh clicked
        }
    }

    /**
     * Onclick method for Settings button
     *
     * @param view
     */
    public void settingsClicked(View view) {

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
                    } else {
                        Log.e(TAG, "Get user error: " + requestError.getMessage());
                    }
                }
            });
        } else {
            startActivityForResult(intent, RC_SETTINGS);
        }
    }

    /**
     * Onclick method for requesting services
     *
     * @param view if this view is null, we consider the service booking is tentative (first time)
     */
    public void requestMultiService(View view) {
        if (!checkDealership()) return;

        // view is null for request from tutorial
        final Intent intent = new Intent(this, ServiceRequestActivity.class);
        intent.putExtra(ServiceRequestActivity.EXTRA_CAR, dashboardCar);
        //intent.putExtra(ServiceRequestActivity.EXTRA_FIRST_BOOKING, view == null);
        intent.putExtra(ServiceRequestActivity.EXTRA_FIRST_BOOKING, isFirstAppointment);
        isFirstAppointment = false;
        startActivityForResult(intent, RC_REQUEST_SERVICE);
    }

    public void myAppointments(){
        if (!checkDealership()) return;
        mixpanelHelper.trackButtonTapped("My Appointments","Dashboard");
        final Intent intent = new Intent(this, MyAppointmentActivity.class);
        intent.putExtra(ServiceRequestActivity.EXTRA_CAR, dashboardCar);
        startActivity(intent);
    }



    public void dealerNavClicked(View view){
        Intent intent = new Intent(MainActivity.this, DealershipActivity.class);
        intent.putExtra("dashboardCar", dashboardCar);
        startActivity(intent);
    }
    /**
     * Onclick method for Messaging button
     *
     * @param view
     */
    public void startChat(View view) {
        if (!checkDealership()) return;

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
    }

    /**
     * Onclick method for Navigating button in tools
     *
     * @param view
     */
    public void navigateToDealer(View view) {
        if (!checkDealership()) return;

        mixpanelHelper.trackButtonTapped("Directions to " + dashboardCar.getDealership().getName(),
                MixpanelHelper.TOOLS_VIEW);

        String uri = String.format(Locale.ENGLISH,
                "http://maps.google.com/maps?daddr=%s",
                dashboardCar.getDealership().getAddress());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    /**
     * Onclick method for Calling Dealer button in tools
     *
     * @param view
     */
    public void callDealer(View view) {
        if (!checkDealership()) return;

        mixpanelHelper.trackButtonTapped("Confirm call to " + dashboardCar.getDealership().getName(),
                MixpanelHelper.TOOLS_VIEW);

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                dashboardCar.getDealership().getPhone()));
        startActivity(intent);
    }

    /**
     * Given the tutorial should be shown to the user, show tutorial sequence
     */
    private void presentShowcaseSequence(boolean discountAvailable, String discountUnit, float discountAmount) {
        Log.i(TAG, "running present show case");

        tutorialSequence = new MaterialShowcaseSequence(this);
        mixpanelHelper.trackViewAppeared(MixpanelHelper.TUTORIAL_VIEW_APPEARED);

        StringBuilder firstServicePromotion = new StringBuilder();
        firstServicePromotion.append(getResources().getString(R.string.first_service_booking_1));

        if (discountAvailable) {
            if (discountAmount != 0 && discountUnit != null) {
                firstServicePromotion.append(" You can also receive a discount of ");
                if (discountUnit.contains("%")) {
                    firstServicePromotion.append((int) discountAmount)
                            .append(discountUnit).append(" towards your first service");
                } else {
                    firstServicePromotion.append(discountUnit)
                            .append(String.format("%.2f", discountAmount)).append(" towards your first service.");
                }
            }
        }

        final MaterialShowcaseView firstBookingDiscountShowcase = new MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.dashboard_request_service_btn))
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

        tutorialSequence.addSequenceItem(firstBookingDiscountShowcase)
                .addSequenceItem(tentativeDateShowcase);

        tutorialSequence.setOnItemShownListener(new MaterialShowcaseSequence.OnSequenceItemShownListener() {
            @Override
            public void onShow(MaterialShowcaseView materialShowcaseView, int i) {
                if (materialShowcaseView.equals(firstBookingDiscountShowcase)) {
                    try {
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    mixpanelHelper.trackButtonTapped(MixpanelHelper.TUTORIAL_GET_STARTED_TAPPED, MixpanelHelper.DASHBOARD_VIEW);
                }
            }
        });

        tutorialSequence.setOnItemDismissedListener(new MaterialShowcaseSequence.OnSequenceItemDismissedListener() {
            @Override
            public void onDismiss(MaterialShowcaseView materialShowcaseView, int i) {

                if (!materialShowcaseView.equals(firstBookingDiscountShowcase)) return;

                //Change the color and text back to the original request service button
                try {
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        //viewPager.setCurrentItem(0);
        isFirstAppointment = true;
        tutorialSequence.start();
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
    public void prepareAndStartTutorialSequence() {
        if (!checkDealership()) return;

        showLoading("Loading dealership information...");
        networkHelper.getUserSettingsById(application.getCurrentUserId(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                hideLoading();
                String unit = "";
                float amount = 0f;
                boolean enableDiscountTutorial = false;

                if (response != null) Log.d("FSB", response);
                if (requestError != null) Log.d("FSB", requestError.toString());

                if (isLoading) hideLoading();

                if (requestError == null && response != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.has("shop")) {
                            JSONObject shop = jsonObject.getJSONObject("shop");
                            JSONObject firstAppointmentDiscount = shop.getJSONObject("firstAppointmentDiscount");
                            if (firstAppointmentDiscount.has("amount")) amount = (float) firstAppointmentDiscount.getDouble("amount");
                            if (firstAppointmentDiscount.has("unit")) unit = firstAppointmentDiscount.getString("unit");
                        }
                    } catch (JSONException je) {
                        je.printStackTrace();
                        Log.d(TAG, "Error occurred in retrieving first service booking promotion");
                    }
                } else {
                    Log.e(TAG, "Login: " + requestError.getError() + ": " + requestError.getMessage());
                }

                enableDiscountTutorial = (unit != null && !unit.isEmpty()) && (amount > 0);

                //Show the tutorial
                try {
                    presentShowcaseSequence(enableDiscountTutorial, unit, amount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * clear the tutorial item from dashboard
     */
    private void removeTutorial() {
        Log.d(TAG, "Remove tutorial");
        mixpanelHelper.trackButtonTapped("Tutorial - removeTutorial", MixpanelHelper.DASHBOARD_VIEW);
    }

    private boolean checkDealership() {
        if (dashboardCar == null) {
            return false;
        }

        if (dashboardCar.getDealership() == null) {
            Snackbar.make(rootView, "Please select your dealership first!", Snackbar.LENGTH_LONG)
                    .setAction("Select", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            selectDealershipForDashboardCar();
                        }
                    })
                    .show();
            return false;
        }
        return true;
    }

    private void selectDealershipForDashboardCar() {
        final List<Dealership> dealerships = shopLocalStore.getAllDealerships();
        final List<String> shops = new ArrayList<>();
        final List<String> shopIds = new ArrayList<>();

        // Try local store for dealerships
        if (dealerships.isEmpty()) {
            showLoading("Getting shop information..");
            networkHelper.getShops(new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
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
                            showSelectDealershipDialog(shops.toArray(new String[shops.size()]),
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
                }
            });
        } else {
            for (Dealership shop : dealerships) {
                shops.add(shop.getName());
                shopIds.add(String.valueOf(shop.getId()));
            }
            showSelectDealershipDialog(shops.toArray(new String[shops.size()]),
                    shopIds.toArray(new String[shopIds.size()]));
        }
    }

    private void showSelectDealershipDialog(final String[] shops, final String[] shopIds) {
        final int[] pickedPosition = {-1};

        final AlertDialog dialog = new AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setSingleChoiceItems(shops, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        pickedPosition[0] = which;
                    }
                })
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("CONFIRM", null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (pickedPosition[0] == -1) {
                            Toast.makeText(MainActivity.this, "Please select a dealership", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final int shopId = Integer.parseInt(shopIds[pickedPosition[0]]);

                        try {
                            mixpanelHelper.trackCustom("Button Tapped",
                                    new JSONObject(String.format("{'Button':'Select Dealership', 'View':'%s', 'Make':'%s', 'Model':'%s'}",
                                            MixpanelHelper.SETTINGS_VIEW, dashboardCar.getMake(), dashboardCar.getModel())));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        networkHelper.updateCarShop(dashboardCar.getId(), shopId, new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                dialog.dismiss();
                                if (requestError == null) {
                                    Log.i(TAG, "Dealership updated - carId: " + dashboardCar.getId() + ", dealerId: " + shopId);
                                    // Update car in local database
                                    dashboardCar.setShopId(shopId);
                                    dashboardCar.setDealership(shopLocalStore.getDealership(shopId));
                                    carLocalStore.updateCar(dashboardCar);

                                    final Map<String, Object> properties = User.getCurrentUser().getProperties();
                                    properties.put("Email", shopLocalStore.getDealership(shopId).getEmail());
                                    User.getCurrentUser().addProperties(properties);

                                    Toast.makeText(MainActivity.this, "Car dealership updated", Toast.LENGTH_SHORT).show();

                                    refreshFromLocal();
                                    resetMenus(false);
                                } else {
                                    Log.e(TAG, "Dealership update error: " + requestError.getError());
                                    Toast.makeText(MainActivity.this, "There was an error, please try again", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
            }
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void logAuthInfo(){
        if (BuildConfig.DEBUG){
            Log.d(TAG, "RefreshToken: " + application.getRefreshToken());
            Log.d(TAG, "AccessToken: " + application.getAccessToken());
        }
    }

    public void toggleConnectionStatusActionBar(boolean isConnected){
        if (getSupportActionBar() != null){
            //getSupportActionBar().setSubtitle(isConnected ? R.string.connected_device:R.string.disconnected_device);
            //Above is temporarily removed until bluetooth status appearance is fixed and in order.
        }

    }


}
