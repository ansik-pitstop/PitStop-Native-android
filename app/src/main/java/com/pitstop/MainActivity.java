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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
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
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.database.DBModel;
import com.pitstop.parse.GlobalApplication;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;
import pub.devrel.easypermissions.EasyPermissions;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class MainActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener,
        EasyPermissions.PermissionCallbacks {

    private static final int RC_ADD_CAR = 50;
    private static final int RC_SCAN_CAR = 51;
    private static final int RC_SETTINGS = 52;
    private static final int RC_DISPLAY_ISSUE = 53;

    private static final int RC_LOCATION_PERM = 101;
    public static final int RC_ENABLE_BT= 102;
    public static final int RESULT_OK = 60;

    private static final String SHOWCASE_ID = "main_activity_sequence_01";


    public ArrayList<DBModel> array;

    final static String pfName = "com.pitstop.login.name";
    private final static String pfTutorial = "com.pitstop.tutorial";
    final static String pfCodeForObjectID = "com.pitstop.login.objectID";
    final static String pfCurrentCar = "ccom.pitstop.currentcar";
    final static String pfShopName = "com.pitstop.shop.name";
    final static String pfCodeForShopObjectID = "com.pitstop.shop.objectID";

    public static final String CAR_EXTRA = "car";
    public static final String CAR_ISSUE_EXTRA = "car_issue";
    public static final String CAR_LIST_EXTRA = "car_list";
    public static final String HAS_CAR_IN_DASHBOARD = "has_car";
    public static final String REFRESH_FROM_SERVER = "_server";
    public static final String REFRESH_FROM_LOCAL = "_local";
    public static final String FROM_ACTIVITY = "from_activity";

    public static final String[] LOC_PERMS = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};
    private static final int LOC_PERM_REQ = 112;

    private boolean isLoading = false;

    private RecyclerView recyclerView;
    private CustomAdapter carIssuesAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private RelativeLayout mainView;
    private RelativeLayout connectedCarIndicator;
    private RelativeLayout serviceCountBackground;
    private RelativeLayout dealershipLayout;
    private RelativeLayout addressLayout, phoneNumberLayout, chatLayout;
    private Toolbar toolbar;
    private TextView serviceCountText;

    private TextView carName, dealershipName;
    private Button carScan;

    private ProgressDialog progressDialog;

    private Car dashboardCar;
    private List<Car> carList = new ArrayList<>();
    private List<CarIssue> carIssueList = new ArrayList<>();

    private LocalCarAdapter carLocalStore;
    private LocalCarIssueAdapter carIssueLocalStore;
    private LocalShopAdapter shopLocalStore;

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    private Intent splashScreenIntent;
    private Intent pushIntent;

    private SharedPreferences sharedPreferences;


    public static String TAG = MainActivity.class.getSimpleName();

    private BluetoothAutoConnectService autoConnectService;
    private Intent serviceIntent;
    private boolean serviceIsBound;
    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG,"connecting: onServiceConnection");
            // cast the IBinder and get MyService instance
            serviceIsBound = true;

            autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
            autoConnectService.setCallbacks(MainActivity.this);

            // Send request to user to turn on bluetooth if disabled
            if (BluetoothAdapter.getDefaultAdapter()!=null) {
                if(EasyPermissions.hasPermissions(MainActivity.this, LOC_PERMS)) {
                    autoConnectService.startBluetoothSearch();
                } else {
                    EasyPermissions.requestPermissions(MainActivity.this,
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

    /**
     * Monitor app connection to device, so that ui can be updated
     * appropriately.
     * */
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 0) {
                if(autoConnectService != null
                        && autoConnectService.getState() == IBluetoothCommunicator.CONNECTED
                        && dashboardCar != null
                        && dashboardCar.getScanner()
                        .equals(autoConnectService.getCurrentDeviceId())) {
                    updateConnectedCarIndicator(true);
                } else {
                    updateConnectedCarIndicator(false);
                }
                handler.post(carConnectedRunnable);
            }
        }
    };


    Runnable carConnectedRunnable = new Runnable() {
        @Override
        public void run() {
            handler.sendEmptyMessage(0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        serviceIntent= new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
        setContentView(R.layout.activity_main);

        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);
        splashScreenIntent = getIntent();
        pushIntent = getIntent();

        // Local db adapters
        carLocalStore = new LocalCarAdapter(this);
        carIssueLocalStore = new LocalCarIssueAdapter(this);
        shopLocalStore = new LocalShopAdapter(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        //setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.primary));
        setSupportActionBar(toolbar);

        setUpUIReferences();

        try {
            mixpanelHelper.trackViewAppeared(TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.refresh_main) {
            try {
                mixpanelHelper.trackButtonTapped("Refresh", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            refreshFromServer();
        } else if(id == R.id.add) {
            try {
                mixpanelHelper.trackButtonTapped("Add Car", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            startAddCarActivity(dashboardCar!=null);
        } else if(id == R.id.action_settings) {
            try {
                mixpanelHelper.trackButtonTapped("Settings", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            sharedPreferences.edit().putBoolean(REFRESH_FROM_SERVER, true).apply();

            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);

            IntentProxyObject proxyObject = new IntentProxyObject();

            proxyObject.setCarList(carList);
            intent.putExtra(CAR_LIST_EXTRA,proxyObject);
            startActivityForResult(intent, RC_SETTINGS);

        } else if(id == R.id.action_car_history) {
            try {
                mixpanelHelper.trackButtonTapped("History", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(MainActivity.this, CarHistoryActivity.class);
            intent.putExtra("carId",dashboardCar.getParseId());
            startActivity(intent);
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "onResume");

        Intent intent = getIntent();

        try {
            mixpanelHelper.trackViewAppeared(TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Always refresh from the server if resuming from log in activity
        if(splashScreenIntent != null
                && splashScreenIntent.getBooleanExtra(SplashScreen.LOGIN_REFRESH, false)) {
            Log.i(TAG, "refresh from login");
            splashScreenIntent = null;
            refreshFromServer();
        } else if(intent != null
                && SelectDealershipActivity.ACTIVITY_NAME.equals(intent.getStringExtra(FROM_ACTIVITY))) {
            // In the event the user pressed back button while in the select dealership activity
            // then load required data from local db.
            refreshFromLocal();
        } else if(pushIntent != null
                && PitstopPushBroadcastReceiver.ACTIVITY_NAME.equals(pushIntent.getStringExtra(FROM_ACTIVITY))) {
            // On opening a push notification, load required data from server
            refreshFromServer();
            pushIntent = null;
        }

        handler.postDelayed(carConnectedRunnable, 1000);
    }


    // TODO: Switch to fragments, as opposed to starting child activities
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivity");

        if(data != null) {
            boolean shouldRefreshFromServer = data.getBooleanExtra(REFRESH_FROM_SERVER,false);

            if(requestCode == RC_ADD_CAR && resultCode==AddCarActivity.ADD_CAR_SUCCESS) {

                if(shouldRefreshFromServer) {
                    refreshFromServer();
                } else {
                    dashboardCar = (Car) data.getSerializableExtra(CAR_EXTRA);
                    sharedPreferences.edit().putInt(pfCurrentCar, dashboardCar.getId()).commit();
                }
            } else if(requestCode == RC_SCAN_CAR && resultCode == RESULT_OK) {
                if(shouldRefreshFromServer) {
                    refreshFromServer();
                }
            } else if(requestCode == RC_SETTINGS && resultCode == RESULT_OK) {
                boolean refreshFromLocal = data.getBooleanExtra(REFRESH_FROM_LOCAL,false);
                if(shouldRefreshFromServer) {
                    refreshFromServer();
                } else if(refreshFromLocal) {
                    refreshFromLocal();
                }
            } else if(requestCode == RC_DISPLAY_ISSUE && resultCode == RESULT_OK) {
                if(shouldRefreshFromServer) {
                    refreshFromServer();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        handler.removeCallbacks(carConnectedRunnable);
        application.getMixpanelAPI().flush();

        super.onPause();
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
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed");
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    /**
     * UI update methods
     */
    private void setUpUIReferences() {

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.car_issues_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        carIssuesAdapter = new CustomAdapter(carIssueList);
        recyclerView.setAdapter(carIssuesAdapter);

        setSwipeDeleteListener(recyclerView);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);

        carName = (TextView) findViewById(R.id.car_name);
        serviceCountText = (TextView) findViewById(R.id.service_count_text);
        dealershipName = (TextView) findViewById(R.id.dealership_name);

        mainView = (RelativeLayout) findViewById(R.id.main_view);
        serviceCountBackground = (RelativeLayout) findViewById(R.id.service_count_background);
        dealershipLayout = (RelativeLayout) findViewById(R.id.dealership_info_layout);

        carScan = (Button) findViewById(R.id.car_scan_btn);
        carScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    application.getMixpanelAPI().track("Button Tapped",
                            new JSONObject(String.format("{'Button':'Scan', 'View':'%s', 'Make':'%s', 'carModel':'%s', 'Device':'Android'}",
                                    TAG, dashboardCar.getMake(), dashboardCar.getModel())));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sharedPreferences.edit().putBoolean(REFRESH_FROM_SERVER, true).apply();

                Intent intent = new Intent(MainActivity.this, CarScanActivity.class);
                intent.putExtra(CAR_EXTRA,dashboardCar);
                startActivityForResult(intent, RC_SCAN_CAR);
            }
        });

        addressLayout = (RelativeLayout) findViewById(R.id.address_layout);
        addressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
            }
        });

        phoneNumberLayout = (RelativeLayout) findViewById(R.id.phone_layout);
        phoneNumberLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mixpanelHelper.trackButtonTapped("Call " + dashboardCar.getDealership().getName(), TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                        dashboardCar.getDealership().getPhone()));
                startActivity(intent);
            }
        });

        chatLayout = (RelativeLayout) findViewById(R.id.chat_layout);
        chatLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                customProperties.put("Phone", ParseUser.getCurrentUser().get("phoneNumber"));
                Log.i(TAG, dashboardCar.getDealership().getEmail());
                customProperties.put("Email",dashboardCar.getDealership().getEmail());
                User.getCurrentUser().addProperties(customProperties);
                User.getCurrentUser().setFirstName(ParseUser.getCurrentUser().getString("name"));
                User.getCurrentUser().setEmail(ParseUser.getCurrentUser().getEmail());
                ConversationActivity.show(MainActivity.this);
            }
        });

        connectedCarIndicator = (RelativeLayout) findViewById(R.id.car_connected_indicator_layout);
    }

    private void updateConnectedCarIndicator(boolean isConnected) {
        if(isConnected) {
            connectedCarIndicator.setBackgroundColor(getResources().getColor(R.color.evcheck));
        } else {
            connectedCarIndicator.setBackgroundColor(getResources().getColor(R.color.not_connected));
        }
    }

    private void hideLoading() {
        progressDialog.dismiss();
        isLoading = false;

        if(isFinishing()) {
            return;
        }

        mainView.setVisibility(View.VISIBLE);

    }

    private void showLoading(String text) {
        if(isFinishing()) {
            return;
        }

        isLoading = true;

        progressDialog.setMessage(text);
        if(!progressDialog.isShowing()) {
            progressDialog.show();
        }

        mainView.setVisibility(View.INVISIBLE);
    }

    /**
     * Detect Swipes on each list item
     * @param //recyclerView
     */
    private void setSwipeDeleteListener(RecyclerView recyclerView) {
        SwipeableRecyclerViewTouchListener swipeTouchListener =
                new SwipeableRecyclerViewTouchListener(recyclerView,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {

                            @Override
                            public boolean canSwipe(int position) {
                                if(carIssuesAdapter.getItemViewType(position)
                                        == CustomAdapter.VIEW_TYPE_EMPTY) {
                                    return false;
                                }

                                return true;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(final RecyclerView recyclerView,
                                                               final int[] reverseSortedPositions) {

                                final CharSequence[] times = new CharSequence[]{
                                        "Recently", "2 Weeks Ago", "A Month Ago",
                                        "2 to 3 Months Ago", "3 to 6 Months Ago",
                                        "6 to 12 Months Ago"
                                };

                                final int[] estimate = new int[]{0,2,3,10,18,32};

                                Calendar cal = Calendar.getInstance();
                                final Date currentLocalTime = cal.getTime();
                                final DateFormat date = new SimpleDateFormat("yyy-MM-dd HH:mm:ss z");

                                final int i = reverseSortedPositions[0];
                                android.support.v7.app.AlertDialog setDoneDialog =
                                        new android.support.v7.app.AlertDialog.Builder(MainActivity.this)
                                                .setItems(times, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, final int position) {

                                                        //----- services
                                                        try {
                                                            mixpanelHelper.trackButtonTapped("Completed Service: "
                                                                    + carIssueList.get(i).getIssueDetail().getItem() + " " + times[position], TAG);
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }

                                                        CarIssue carIssue = carIssuesAdapter.getItem(i);
                                                        updateServiceHistoryOnParse(carIssue, position, estimate,
                                                                times, date, currentLocalTime, reverseSortedPositions);
                                                        dialogInterface.dismiss();
                                                    }
                                                }).setTitle("When did you complete this task?").show();
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView
                                    , int[] reverseSortedPositions) {
                                onDismissedBySwipeLeft(recyclerView,reverseSortedPositions);
                            }
                        });

        recyclerView.addOnItemTouchListener(swipeTouchListener);
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

        if(isFinishing()) {
            return;
        }

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Enter additional comment");

        final String[] additionalComment = {""};
        final EditText userInput = new EditText(MainActivity.this);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT);
        alertDialog.setView(userInput);

        alertDialog.setPositiveButton("SEND", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    application.getMixpanelAPI().track("Button Tapped",
                            new JSONObject("'Button':'Confirm Service Request','View':'" + TAG
                                    + "','Device':'Android','Number of Services Requested',"
                                    + dashboardCar.getIssues().size()));
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

        String userId = ParseUser.getCurrentUser().getObjectId();
        HashMap<String,Object> output = new HashMap<>();
        List<HashMap<String,String>> services = new ArrayList<>();

        if(carIssueList.isEmpty()) {
            output.put("services", services);
            output.put("carVin", dashboardCar.getVin());
            output.put("userObjectId", userId);
            output.put("comments",additionalComment);
        } else {
            for(CarIssue carIssue : carIssueList ) {
                if(carIssue.getIssueType().equals("recall")) {
                    HashMap<String, String> recall = new HashMap<>();
                    recall.put("item", carIssue.getIssueDetail().getItem());
                    recall.put("action", carIssue.getIssueDetail().getAction());
                    recall.put("itemDescription", carIssue.getIssueDetail().getDescription());
                    recall.put("priority", String.valueOf(carIssue.getPriority()));
                    services.add(recall);

                    output.put("services", services);
                    output.put("carVin", dashboardCar.getVin());
                    output.put("userObjectId", userId);
                    output.put("comments", additionalComment);

                } else if(carIssue.getIssueType().equals("dtc")) {

                    HashMap<String, String> dtc = new HashMap<>();
                    dtc.put("item", carIssue.getIssueDetail().getItem());
                    dtc.put("action", carIssue.getIssueDetail().getAction());
                    dtc.put("itemDescription", carIssue.getIssueDetail().getDescription());
                    dtc.put("priority", String.valueOf(carIssue.getPriority()));
                    services.add(dtc);

                    output.put("services", services);
                    output.put("carVin", dashboardCar.getVin());
                    output.put("userObjectId", userId);
                    output.put("comments", additionalComment);


                } else {
                    HashMap<String, String> service = new HashMap<>();
                    service.put("item",carIssue.getIssueDetail().getItem());
                    service.put("action",carIssue.getIssueDetail().getAction());
                    service.put("itemDescription",carIssue.getIssueDetail().getDescription());
                    service.put("priority",String.valueOf(carIssue.getPriority()));
                    services.add(service);

                    output.put("services", services);
                    output.put("carVin", dashboardCar.getVin());
                    output.put("userObjectId", userId);
                    output.put("comments",additionalComment);
                }
            }
        }

        ParseCloud.callFunctionInBackground("sendServiceRequestEmail", output, new FunctionCallback<Object>() {
            @Override
            public void done(Object object, ParseException e) {
                if (e == null) {
                    Toast.makeText(MainActivity.this,
                            "Request sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Error sending request", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, e.getMessage());
                }
            }
        });
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
            carList = localCars;

            setDashboardCar(carList);
            setCarDetailsUI();
            hideLoading();
        }
    }


    /**
     * Update ui with current car info
     * And retrieve available car issues
     * */
    private void setCarDetailsUI() {
        setDealership();
        populateCarIssuesAdapter();

        if(application.checkAppStart() == GlobalApplication.AppStart.FIRST_TIME
                || application.checkAppStart() == GlobalApplication.AppStart.FIRST_TIME_VERSION) {

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    presentShowcaseSequence();
                }
            }, 2000);
        }

        carName.setText(dashboardCar.getYear() + " "
                + dashboardCar.getMake() + " "
                + dashboardCar.getModel());

        int recallCount = dashboardCar.getNumberOfRecalls();
        int serviceCount = dashboardCar.getNumberOfServices();
        int total = recallCount + serviceCount;

        serviceCountText.setText(String.valueOf(total));

        Drawable background = serviceCountBackground.getBackground();
        GradientDrawable gradientDrawable = (GradientDrawable) background;

        if (total > 0) {
            gradientDrawable.setColor(Color.rgb(203, 77, 69));
        } else {
            gradientDrawable.setColor(Color.rgb(93, 172, 129));
        }
    }

    /** Call function to retrieve live data from parse
     * @see #getCarDetails()
     * */
    private void loadCarDetailsFromServer() {
        int userId = 0;

        if (GlobalApplication.getCurrentUser() != null) {
            userId = GlobalApplication.getCurrentUser().getUserId();
        }

        NetworkHelper.getCarsByUserId(userId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(requestError == null) {
                    try {
                        carList = Car.createCarsList(response);

                        if(carList.isEmpty()) {
                            if (isLoading) {
                                hideLoading();
                            }
                            startAddCarActivity(false);
                        } else {
                            setDashboardCar(carList);
                            carLocalStore.storeCars(carList);
                            setCarDetailsUI();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,
                                "Error retrieving car details", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Load cars error: " + requestError.getMessage());
                    Toast.makeText(MainActivity.this,
                            "Error retrieving car details", Toast.LENGTH_SHORT).show();
                }
            }
        });

        /*ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
        if (ParseUser.getCurrentUser() != null) {
            userId = ParseUser.getCurrentUser().getObjectId();
        }

        query.whereContains("owner", userId);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    if (!objects.isEmpty()) {

                        carList = Car.createCarsList(objects);

                        setDashboardCar(carList);
                        carLocalStore.storeCars(carList);
                        setCarDetailsUI();

                    } else {
                        if (isLoading) {
                            hideLoading();
                        }
                        startAddCarActivity(false);
                    }

                } else {
                    if (isLoading) {
                        hideLoading();
                    }
                    Toast.makeText(MainActivity.this,
                            "Error retrieving car details", Toast.LENGTH_SHORT).show();
                }
            }
        });*/
    }

    private void setDealership() {

        Dealership shop = shopLocalStore.getDealership(dashboardCar.getShopId());
        if(shop == null) {

            NetworkHelper.getShops(new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if(requestError == null) {
                        try {
                            List<Dealership> dl = Dealership.createDealershipList(response);
                            shopLocalStore.deleteAllDealerships();
                            shopLocalStore.storeDealerships(dl);

                            Dealership d = shopLocalStore.getDealership(dashboardCar.getShopId());

                            dashboardCar.setDealership(d);
                            if(dashboardCar.getDealership() != null) {
                                dealershipName.setText(dashboardCar.getDealership().getName());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e(TAG, "Get shops: " + requestError.getMessage());
                    }
                }
            });

            /*ParseQuery<ParseObject> query = ParseQuery.getQuery("Shop");
            query.whereEqualTo("objectId", dashboardCar.getShopId());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null) {
                        Dealership dealership = Dealership
                                .createDealership(objects.get(0), dashboardCar.getParseId());
                        dashboardCar.setDealership(dealership);
                        dealershipName.setText(dashboardCar.getDealership().getName());
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to retrieve dealership info",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });*/
        } else {
            dashboardCar.setDealership(shop);
            dealershipName.setText(dashboardCar.getDealership().getName());
        }

    }


    @Override
    public void getBluetoothState(int state) {
        if(state==BluetoothManage.DISCONNECTED) {
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
    public void getIOData(DataPackageInfo dataPackageInfo) {  }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if(loginPackageInfo.flag.
                equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            Log.i(TAG, "Device logout");
        }
    }

    private void populateCarIssuesAdapter() {
        if(isLoading) {
            hideLoading();
        }

        // Try local store
        Log.i(TAG, "DashboardCar id: (Try local store) "+dashboardCar.getId());
        List<CarIssue> carIssues = carIssueLocalStore.getAllCarIssues(String.valueOf(dashboardCar.getId()));
        if(carIssues.isEmpty() && (dashboardCar.getNumberOfServices() > 0
                || dashboardCar.getNumberOfRecalls() > 0)) {
            Log.i(TAG, "No car issues in local store");

            NetworkHelper.getCarsById(dashboardCar.getId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if(requestError == null) {
                        try {
                            dashboardCar.setIssues(CarIssue.createCarIssues(
                                    new JSONObject(response).getJSONArray("issues"), dashboardCar.getId()));
                            carIssueList.addAll(dashboardCar.getIssues());
                            carIssuesAdapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this,
                                    "Error retrieving car details", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Load issues error: " + requestError.getMessage());
                        Toast.makeText(MainActivity.this,
                                "Error retrieving car details", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            /*List<String> issueIds;

            issueIds = dashboardCar.getPendingEdmundServicesIds();
            getIssues(issueIds, "EdmundsService", CarIssue.EDMUNDS);

            issueIds = dashboardCar.getPendingFixedServicesIds();
            getIssues(issueIds, "ServiceFixed", CarIssue.FIXED);

            issueIds = dashboardCar.getPendingIntervalServicesIds();
            getIssues(issueIds, "ServiceInterval", CarIssue.INTERVAL);

            getRecalls();

            List<String> dtcCodes = dashboardCar.getStoredDTCs();

            getDTCs(dtcCodes, "DTC", CarIssue.DTC);

            List<String> pendingDtcs = dashboardCar.getPendingDTCs();

            getDTCs(pendingDtcs, "DTC", CarIssue.PENDING_DTC);*/
        } else {
            Log.i(TAG, "Trying local store for carIssues");
            dashboardCar.setIssues(carIssues);
            carIssueList.clear();
            carIssueList.addAll(dashboardCar.getIssues());
            carIssuesAdapter.notifyDataSetChanged();
        }


    }

    /*private void getIssues(List<String> serviceIds, String resource, final String issueType) {

        ParseQuery<ParseObject> query = ParseQuery.getQuery(resource);
        query.whereContainedIn("objectId", serviceIds);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {

                    List<CarIssue> issues = CarIssue.createCarIssues(objects, issueType,
                            dashboardCar.getParseId());
                    dashboardCar.getIssues().addAll(issues);
                    // Store in local
                    carIssueLocalStore.storeCarIssues(issues);

                    carIssueList.clear();
                    carIssueList.addAll(dashboardCar.getIssues());
                    carIssuesAdapter.notifyDataSetChanged();

                } else {
                    Log.i(TAG, "Parse error: " + e.getMessage());
                }
            }
        });
    }

    private void getDTCs(List<String> dtcCodes, String resource, final String issueType) {
        Log.i(TAG, "Getting dtcs");
        ParseQuery<ParseObject> query = ParseQuery.getQuery(resource);
        query.whereContainedIn("dtcCode", dtcCodes);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {

                    List<CarIssue> dtcs = CarIssue.createCarIssues(objects, issueType,
                            dashboardCar.getParseId());
                    dashboardCar.getIssues().addAll(dtcs);
                    // Store in local
                    carIssueLocalStore.storeCarIssues(dtcs);

                    carIssueList.clear();
                    carIssueList.addAll(dashboardCar.getIssues());
                    carIssuesAdapter.notifyDataSetChanged();

                } else {
                    Log.i(TAG, "Parse error: (getDTCs)" + e.getMessage());
                }
            }
        });
    }

    private void getRecalls() {
        Log.i(TAG, "Getting recalls");

        //custom call for recall entry
        ParseObject car = ParseObject.createWithoutData("Car",dashboardCar.getParseId());
        ParseQuery<ParseObject> query = ParseQuery.getQuery("RecallMasters");

        query.whereEqualTo("forCar", car);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> recallsList, ParseException e) {
                if (e == null) {

                    if(!recallsList.isEmpty()) {
                        ParseObject firstMatch = recallsList.get(0);
                        JSONArray recalls = firstMatch.getJSONArray("recalls");
                        List<String> recallsIds = new ArrayList<String>();

                        if(recalls != null && recalls.length() > 0) {

                            for(int i = 0; i < recalls.length(); i++) {
                                try {
                                    recallsIds.add(recalls.getJSONObject(i).getString("objectId"));
                                } catch (JSONException e1) {
                                    e1.printStackTrace();
                                }
                            }

                            ParseQuery<ParseObject> query = ParseQuery.getQuery("RecallEntry");
                            query.whereContainedIn("objectId",recallsIds);
                            query.findInBackground(new FindCallback<ParseObject>() {
                                @Override
                                public void done(List<ParseObject> objects, ParseException e) {
                                    if(e == null) {

                                        List<CarIssue> recalls = CarIssue.createCarIssues(objects, CarIssue.RECALL,
                                                dashboardCar.getParseId());
                                        Log.i(TAG, "Recalls count: "+ recalls.size());
                                        dashboardCar.getIssues().addAll(recalls);
                                        // Store in local
                                        carIssueLocalStore.storeCarIssues(recalls);

                                        carIssueList.clear();
                                        carIssueList.addAll(dashboardCar.getIssues());
                                        carIssuesAdapter.notifyDataSetChanged();
                                    } else {
                                        Log.i(TAG, e.getMessage());
                                    }
                                }
                            });
                        }

                        Log.i(TAG, recallsIds.toString());

                        // Limit 100 TODO review
                        Log.i(TAG, "recall list size: "+recallsList.size());
                    }

                }else{
                    Log.i(TAG,"Parse error on recalls");
                    Log.i(TAG, e.getMessage());
                }
            }
        });
    }*/

    private void updateServiceHistoryOnParse(final CarIssue carIssue, int position
            , int[] estimate, CharSequence[] times
            , DateFormat date, Date currentLocalTime, final int[] reverseSortedPositions ) {

        double mileage = dashboardCar.getTotalMileage();
        String type = carIssue.getIssueType();
        final int typeService = (type.equals("edmunds") ? 0 : (type.equals("fixed") ? 1 : 2));

        ParseObject saveCompletion = new ParseObject("ServiceHistory");
        saveCompletion.put("carId", dashboardCar.getParseId());
        saveCompletion.put("mileageSetByUser", mileage);
        saveCompletion.put("mileage", mileage - estimate[position] * 375);
        saveCompletion.put("serviceObjectId", carIssue.getParseId());
        saveCompletion.put("shopId", dashboardCar.getShopId());
        saveCompletion.put("userMarkedDoneOn", times[position] + " from " + date.format(currentLocalTime));
        if(carIssue.getIssueType().equals(CarIssue.DTC) || carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
            saveCompletion.put("serviceId", 123);
        } else if(carIssue.getIssueType().equals(CarIssue.RECALL)) {
            saveCompletion.put("serviceId", 124);
        } else {
            saveCompletion.put("serviceId", 125);
        }

        if(!carIssue.getIssueType().equals(CarIssue.RECALL) && !carIssue.getIssueType().equals(CarIssue.DTC)
                && !carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
            saveCompletion.put("type", typeService);
            saveCompletion.saveEventually(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if(e == null) {
                        Toast.makeText(MainActivity.this, "Updated Service History",
                                Toast.LENGTH_SHORT).show();
                        //update the car object on the server next
                        updateCarOnParse(typeService,carIssue,  reverseSortedPositions);
                    } else {
                        Toast.makeText(MainActivity.this, "Parse Error: "+e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if(carIssue.getIssueType().equals(CarIssue.RECALL)) {
            saveCompletion.saveEventually(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(MainActivity.this, "Updated Service History",
                                Toast.LENGTH_SHORT).show();
                        // Update recall object
                        updateRecallOnParse(carIssue, reverseSortedPositions);
                    } else {
                        Toast.makeText(MainActivity.this, "Parse Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            ParseQuery car = new ParseQuery("Car");
            try {
                car.get(dashboardCar.getParseId());
                car.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (e == null) {
                            if (objects.size() == 0) {
                                return;
                            }

                            dashboardCar.setStoredDTCs(new ArrayList<String>());
                            dashboardCar.setPendingDTCs(new ArrayList<String>());

                            List dtcCode = Arrays.asList(carIssue.getIssueDetail().getItem());
                            if (carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                                objects.get(0).removeAll("pendingDTCs", dtcCode);
                                dashboardCar.setPendingDTCs(new ArrayList<String>());
                            } else if (carIssue.getIssueType().equals(CarIssue.DTC)) {
                                objects.get(0).removeAll("storedDTCs", dtcCode);
                                dashboardCar.setStoredDTCs(new ArrayList<String>());
                            }
                            objects.get(0).saveEventually();
                        } else {
                            e.printStackTrace();
                        }
                    }
                });

                saveCompletion.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            Toast.makeText(MainActivity.this, "Updated Service History",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            e.printStackTrace();
                        }
                    }
                });
            }
            catch (ParseException e) {
                e.printStackTrace();
            }
        }

        refreshFromServer();
    }

    private void updateCarOnParse(final int typeService, final CarIssue carIssue,
                                  final int[] reverseSortedPositions) {
        ParseQuery updateObject = new ParseQuery("Car");
        try {
            updateObject.get(dashboardCar.getParseId());
            updateObject.findInBackground(new FindCallback<ParseObject>() {

                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e==null) {

                        String type = (typeService == 0 ? "pendingEdmundServices" :
                                (typeService == 1 ? "pendingFixedServices" : "pendingIntervalServices"));

                        JSONArray original = objects.get(0).getJSONArray(type);
                        ArrayList<String> updatedTexts = new ArrayList<>();

                        for (int j = 0; j < original.length(); j++) {
                            try {
                                if (!original.getString(j).equals(carIssue.getParseId())) {
                                    updatedTexts.add(original.getString(j));
                                }
                            } catch (JSONException e1) {
                                e1.printStackTrace();
                            }
                        }

                        //update object on server for Car
                        objects.get(0).put(type, updatedTexts);
                        objects.get(0).saveEventually();

                        for (int position : reverseSortedPositions) {
                            CarIssue obj1 = carIssuesAdapter.getItem(position);
                            CarIssue obj2 = carIssueList.get(position);

                            Log.i(TAG, "Car parse (Adapter)--> "+obj1.getIssueDetail().getItem());
                            Log.i(TAG, "Car on parse --> "+obj2.getIssueDetail().getItem());
                            carIssueList.remove(position);
                            carIssuesAdapter.notifyDataSetChanged();
                        }

                        // TODO check
                        refreshFromServer();
                    } else {
                        Toast.makeText(MainActivity.this,"Parse error: "+e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void updateRecallOnParse(CarIssue carIssue, final int[] reverseSortedPositions) {
        ParseQuery updateObject = new ParseQuery("RecallEntry");
        try {
            updateObject.get(carIssue.getParseId());
            updateObject.findInBackground(new FindCallback<ParseObject>() {

                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null) {
                        objects.get(0).put("state", "doneByUser");
                        ((ParseObject)objects.get(0)).saveEventually();

                        for (int position : reverseSortedPositions) {
                            CarIssue objToRemove = carIssuesAdapter.getItem(position);

                            Log.i(TAG, "Recall (Adapter)--> "+objToRemove.getIssueDetail().getItem());

                            carIssueList.remove(position);
                            carIssueLocalStore.deleteCarIssue(objToRemove);
                            carIssuesAdapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(MainActivity.this,"Parse error: "+e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void refreshFromServer() {
        carIssueList.clear();
        carLocalStore.deleteAllCars();
        carIssueLocalStore.deleteAllCarIssues();
        getCarDetails();
    }

    private void refreshFromLocal() {
        carIssueList.clear();
        getCarDetails();
    }

    private void startAddCarActivity(boolean hasCars) {
        Intent intent = new Intent(MainActivity.this, AddCarActivity.class);
        if(hasCars) {
            intent.putExtra(HAS_CAR_IN_DASHBOARD, true);
            intent.putExtra(CAR_EXTRA, dashboardCar);
        }
        startActivityForResult(intent, RC_ADD_CAR);
    }

    private void setDashboardCar(List<Car> carList) {
        int currentCarId = sharedPreferences.getInt(pfCurrentCar, -1);

        for(Car car : carList) {
            if(car.getId() == currentCarId) {
                dashboardCar = car;
                dashboardCar.setStoredDTCs(car.getStoredDTCs());
                dashboardCar.setPendingDTCs(car.getPendingDTCs());
                return;
            }
        }

        dashboardCar = carList.get(0);
        dashboardCar.setCurrentCar(true);
        carLocalStore.updateCar(dashboardCar);

        sharedPreferences.edit().putInt(pfCurrentCar, dashboardCar.getId()).commit();

        /*ParseQuery<ParseObject> cars = ParseQuery.getQuery("Car");
        ParseObject car = null;

        try {
            car = cars.get(dashboardCar.getParseId());

            dashboardCar.setStoredDTCs(car.<String>getList("storedDTCs"));
            dashboardCar.setPendingDTCs(car.<String>getList("pendingDTCs"));

            car.put("currentCar",true);
            car.saveEventually();
        } catch (ParseException e) {
            e.printStackTrace();
        }*/
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

    /**
     * Tutorial
     */
    private void presentShowcaseSequence() {

        boolean hasSeenTutorial = sharedPreferences.getBoolean(pfTutorial,false);
        if(hasSeenTutorial) {
            return;
        }

        Log.i(TAG, "running present show case");

        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500); // half second between each showcase view

        final MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this);

        try {
            mixpanelHelper.trackViewAppeared("Tutorial Onboarding");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        sequence.setConfig(config);

        sequence.setOnItemShownListener(new MaterialShowcaseSequence.OnSequenceItemShownListener() {
            @Override
            public void onShow(MaterialShowcaseView materialShowcaseView, int i) {
                sharedPreferences.edit().putBoolean(pfTutorial,true).apply();
            }
        });

        sequence.addSequenceItem(new MaterialShowcaseView.Builder(this)
                .setTarget(toolbar.findViewById(R.id.add))
                .setTitleText("Add Car")
                .setContentText("Click to add a new car")
                .setDismissOnTouch(true)
                .setDismissText("OK")
                .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this)
                        .setTarget(carScan)
                        .setTitleText("Scan Car")
                        .setContentText("Click to scan car for issues")
                        .setDismissOnTouch(true)
                        .setDismissText("OK")
                        .build()
        );

        sequence.addSequenceItem(
                new MaterialShowcaseView.Builder(this)
                        .setTarget(dealershipLayout)
                        .setTitleText("Your Dealership")
                        .setContentText("Feel free to click these to " +
                                "message/call/get directions to your dealership. " +
                                "You can edit this in your settings.")
                        .setDismissOnTouch(true)
                        .setDismissText("OK")
                        .withRectangleShape(true)
                        .build()
        );

        final MaterialShowcaseView finalShowcase = new MaterialShowcaseView.Builder(this)
                .setTarget(recyclerView)
                .setTitleText("Car Issues")
                .setContentText("Swipe to dismiss issues.")
                .setDismissOnTouch(true)
                .setDismissText("Get Started")
                .withRectangleShape(true)
                .build();

        sequence.addSequenceItem(finalShowcase);

        sequence.setOnItemDismissedListener(new MaterialShowcaseSequence.OnSequenceItemDismissedListener() {
            @Override
            public void onDismiss(MaterialShowcaseView materialShowcaseView, int i) {
                if(materialShowcaseView.equals(finalShowcase)) {
                    try {
                        mixpanelHelper.trackButtonTapped("Tutorial - removeTutorial", TAG);
                    } catch (JSONException e) {
                       e.printStackTrace();
                    }
                }
            }
        });

        sequence.start();

    }

    /**
     *
     */
    class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        private List<CarIssue> carIssueList;
        static final int VIEW_TYPE_EMPTY = 100;

        public CustomAdapter(List<CarIssue> carIssues) {
            carIssueList = carIssues;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.car_details_list_item, parent, false);
            ViewHolder viewHolder = new ViewHolder(v);
            return viewHolder;
        }

        public CarIssue getItem(int position) {
            return carIssueList.get(position);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            //Log.i(TAG,"On bind view holder");

            int viewType = getItemViewType(position);

            if(viewType == VIEW_TYPE_EMPTY) {
                holder.description.setMaxLines(2);
                holder.description.setText("You have no pending Engine Code, Recalls or Services");
                holder.title.setText("Congrats!");
                holder.imageView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_check_circle_green_400_36dp));
            } else {
                final CarIssue carIssue = carIssueList.get(position);

                holder.description.setText(carIssue.getIssueDetail().getDescription());
                holder.description.setEllipsize(TextUtils.TruncateAt.END);
                if(carIssue.getIssueType().equals(CarIssue.RECALL)) {
                    holder.title.setText(carIssue.getIssueDetail().getItem());
                    holder.imageView.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_error_red_600_24dp));

                } else if(carIssue.getIssueType().equals(CarIssue.DTC)) {
                    holder.title.setText(String.format("Engine issue: Code %s", carIssue.getIssueDetail().getItem()));
                    holder.imageView.setImageDrawable(getResources().
                            getDrawable(R.drawable.car_engine_red));

                } else if(carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                    holder.title.setText(String.format("Potential engine issue: Code %s", carIssue.getIssueDetail().getItem()));
                    holder.imageView.setImageDrawable(getResources().
                            getDrawable(R.drawable.car_engine_yellow));

                } else {
                    holder.description.setText(carIssue.getIssueDetail().getDescription());
                    holder.title.setText(carIssue.getIssueDetail().getItem());
                    holder.imageView.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_warning_amber_300_24dp));
                }

                holder.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            mixpanelHelper.trackButtonTapped(carIssueList.get(position).getIssueDetail().getItem(), TAG);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Intent intent = new Intent(MainActivity.this, DisplayItemActivity.class);
                        intent.putExtra(CAR_ISSUE_EXTRA, carIssueList.get(position));
                        intent.putExtra(CAR_EXTRA,dashboardCar);
                        startActivityForResult(intent, RC_DISPLAY_ISSUE);
                    }
                });
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(carIssueList.isEmpty()) {
                return VIEW_TYPE_EMPTY;
            }
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            if (carIssueList.isEmpty()) {
                return 1;
            }
            return carIssueList.size();
        }


        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView title;
            public TextView description;
            public ImageView imageView;
            public CardView container;

            public ViewHolder(View v) {
                super(v);
                title = (TextView) v.findViewById(R.id.title);
                description = (TextView) v.findViewById(R.id.description);
                imageView = (ImageView) v.findViewById(R.id.image_icon);
                container = (CardView) v.findViewById(R.id.list_car_item);
            }
        }
    }
}
