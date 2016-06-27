package com.pitstop;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
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
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.application.GlobalApplication;
import com.pitstop.background.MigrationService;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

public class MainActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener {

    private static final int RC_ADD_CAR = 50;
    private static final int RC_SCAN_CAR = 51;
    private static final int RC_SETTINGS = 52;
    private static final int RC_DISPLAY_ISSUE = 53;

    public static final int RC_LOCATION_PERM = 101;
    public static final int RC_ENABLE_BT= 102;
    public static final int RESULT_OK = 60;

    private static final String SHOWCASE_ID = "main_activity_sequence_01";

    public final static String pfName = "com.pitstop.login.name";
    private final static String pfTutorial = "com.pitstop.tutorial";
    public final static String pfCodeForObjectID = "com.pitstop.login.objectID";
    public final static String pfCurrentCar = "ccom.pitstop.currentcar";
    public final static String pfShopName = "com.pitstop.shop.name";
    public final static String pfCodeForShopObjectID = "com.pitstop.shop.objectID";

    public static final String CAR_EXTRA = "car";
    public static final String CAR_ISSUE_EXTRA = "car_issue";
    public static final String CAR_LIST_EXTRA = "car_list";
    public static final String HAS_CAR_IN_DASHBOARD = "has_car";
    public static final String REFRESH_FROM_SERVER = "_server";
    public static final String FROM_ACTIVITY = "from_activity";
    public static final String FROM_NOTIF = "from_notfftfttfttf";

    public static final String[] LOC_PERMS = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    private boolean isLoading = false;

    private NetworkHelper networkHelper;

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

    private boolean askForCar = true;

    private SharedPreferences sharedPreferences;

    private boolean dialogShowing = false;

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

    /**
     * Monitor app connection to device, so that ui can be updated
     * appropriately.
     * */
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 0) {
                if(autoConnectService != null
                        && autoConnectService.getState() == IBluetoothCommunicator.CONNECTED
                        && dashboardCar != null
                        && dashboardCar.getScannerId() != null
                        && dashboardCar.getScannerId()
                        .equals(autoConnectService.getCurrentDeviceId())) { // car is connected

                    updateConnectedCarIndicator(true);

                } else if(autoConnectService != null
                        && askForCar
                        && !dialogShowing
                        && dashboardCar != null
                        && (dashboardCar.getScannerId() == null || dashboardCar.getScannerId().isEmpty())
                        && autoConnectService.getCurrentDeviceId() != null
                        && carLocalStore.getCarByScanner(autoConnectService.getCurrentDeviceId()) == null) { // scanner is connected but not associated with a local car

                    final ArrayList<Car> selectedCar = new ArrayList<>(1); // must be final because this is accessed in the inner class
                    final CarListAdapter carAdapter = new CarListAdapter(carList);

                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setCancelable(false);
                    dialog.setTitle("Module detected. Which car is it connected to?");
                    dialog.setSingleChoiceItems(carAdapter, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedCar.clear();
                            selectedCar.add((Car) carAdapter.getItem(which));
                        }
                    });
                    dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            askForCar = false;
                            dialog.dismiss();
                        }
                    });
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            if (selectedCar.isEmpty()) {
                                return;
                            }

                            networkHelper.createNewScanner(selectedCar.get(0).getId(), autoConnectService.getCurrentDeviceId(),
                                    new RequestCallback() {
                                        @Override
                                        public void done(String response, RequestError requestError) {
                                            if(requestError == null) {
                                                Toast.makeText(MainActivity.this, "Device added successfully", Toast.LENGTH_SHORT).show();
                                                sharedPreferences.edit().putInt(pfCurrentCar, selectedCar.get(0).getId()).commit();
                                                refreshFromServer();
                                            } else {
                                                Toast.makeText(MainActivity.this, "An error occurred, please try again", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    });

                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialogShowing = false;
                        }
                    });

                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            dialogShowing = false;
                        }
                    });

                    dialog.show();
                    dialogShowing = true;

                } else {
                    updateConnectedCarIndicator(false);
                }
                handler.postDelayed(carConnectedRunnable, 2000);
            }
        }
    };

    private class CarListAdapter extends BaseAdapter {
        private List<Car> ownedCars;

        public CarListAdapter(List<Car> cars) {
            ownedCars = cars;
        }

        @Override
        public int getCount () {
            return ownedCars.size();
        }

        @Override
        public Object getItem (int position) {
            return ownedCars.get(position);
        }

        @Override
        public long getItemId (int position) {
            return 0;
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = convertView != null ? convertView :
                    inflater.inflate(android.R.layout.simple_list_item_single_choice, parent, false);
            Car ownedCar = (Car) getItem(position);

            TextView carName = (TextView) rowView.findViewById(android.R.id.text1);
            carName.setText(String.format("%s %s", ownedCar.getMake(), ownedCar.getModel()));
            return rowView;
        }
    }


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

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(MigrationService.notificationId);

        application = (GlobalApplication) getApplicationContext();

        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
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
        setContentView(R.layout.activity_main);

        networkHelper = new NetworkHelper(getApplicationContext());

        mixpanelHelper = new MixpanelHelper(application);

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

        networkHelper.getShops(new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(requestError == null) {
                    try {
                        final List<Dealership> dl = Dealership.createDealershipList(response);
                        shopLocalStore.deleteAllDealerships();
                        shopLocalStore.storeDealerships(dl);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "Get shops: " + requestError.getMessage());
                }
            }
        });
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

            if(autoConnectService.getState() == IBluetoothCommunicator.DISCONNECTED) {
                autoConnectService.startBluetoothSearch();
            }
        } else if(id == R.id.add) {
            startAddCarActivity(null);
        } else if(id == R.id.action_settings) {
            try {
                mixpanelHelper.trackButtonTapped("Settings", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            sharedPreferences.edit().putBoolean(REFRESH_FROM_SERVER, true).apply();

            final Intent intent = new Intent(MainActivity.this, SettingsActivity.class);

            IntentProxyObject proxyObject = new IntentProxyObject();

            proxyObject.setCarList(carList);
            intent.putExtra(CAR_LIST_EXTRA,proxyObject);

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
            if(dashboardCar == null) {
                Toast.makeText(MainActivity.this, "There is no active car", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(MainActivity.this, CarHistoryActivity.class);
                //intent.putExtra("carId",dashboardCar.getId());
                intent.putExtra("dashboardCar", dashboardCar);
                startActivity(intent);
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "onResume");

        try {
            if(dashboardCar == null || dashboardCar.getDealership() == null) {
                mixpanelHelper.trackViewAppeared(TAG);
            } else {
                application.getMixpanelAPI().track("View Appeared",
                        new JSONObject("{'View':'" + TAG + "','Dealership':'" + dashboardCar.getDealership().getName()
                                + "','Device':'Android'}"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        handler.postDelayed(carConnectedRunnable, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivity");

        if(data != null) {
            boolean shouldRefreshFromServer = data.getBooleanExtra(REFRESH_FROM_SERVER,false);

            if(requestCode == RC_ADD_CAR && resultCode==AddCarActivity.ADD_CAR_SUCCESS) {

                findViewById(R.id.no_car_text).setVisibility(View.GONE);

                if(shouldRefreshFromServer) {
                    refreshFromServer();
                } else {
                    dashboardCar = data.getParcelableExtra(CAR_EXTRA);
                    sharedPreferences.edit().putInt(pfCurrentCar, dashboardCar.getId()).commit();
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
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        handler.removeCallbacks(carConnectedRunnable);
        application.getMixpanelAPI().flush();

        if(progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

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
                intent.putExtra(CAR_EXTRA, dashboardCar);
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
                customProperties.put("Phone", application.getCurrentUser().getPhone());
                Log.i(TAG, dashboardCar.getDealership().getEmail());
                customProperties.put("Email", dashboardCar.getDealership().getEmail());
                User.getCurrentUser().addProperties(customProperties);
                User.getCurrentUser().setFirstName(application.getCurrentUser().getFirstName());
                User.getCurrentUser().setEmail(application.getCurrentUser().getEmail());
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

                                final Calendar calendar = Calendar.getInstance();
                                calendar.setTimeInMillis(System.currentTimeMillis());
                                final int currentYear = calendar.get(Calendar.YEAR);
                                final int currentMonth = calendar.get(Calendar.MONTH);
                                final int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

                                final int i = reverseSortedPositions[0];

                                DatePickerDialog datePicker = new DatePickerDialog(MainActivity.this,
                                        new DatePickerDialog.OnDateSetListener() {
                                            @Override
                                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                                if(year > currentYear || (year == currentYear
                                                        && (monthOfYear > currentMonth
                                                        || (monthOfYear == currentMonth && dayOfMonth > currentDay)))) {
                                                    Toast.makeText(MainActivity.this, "Please choose a date that has passed", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    long currentTime = calendar.getTimeInMillis();

                                                    calendar.set(year, monthOfYear, dayOfMonth);

                                                    int daysAgo = (int) TimeUnit.MILLISECONDS.toDays(currentTime - calendar.getTimeInMillis());

                                                    try {
                                                        mixpanelHelper.trackButtonTapped("Completed Service: "
                                                                + carIssueList.get(i).getIssueDetail().getItem() + " " + daysAgo, TAG);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }

                                                    final CarIssue carIssue = carIssuesAdapter.getItem(i);

                                                    networkHelper.serviceDone(dashboardCar.getId(), carIssue.getId(),
                                                            daysAgo, dashboardCar.getTotalMileage(), new RequestCallback() {
                                                                @Override
                                                                public void done(String response, RequestError requestError) {
                                                                    if(requestError == null) {
                                                                        Toast.makeText(MainActivity.this, "Issue cleared", Toast.LENGTH_SHORT).show();
                                                                        if(carIssueList.size() > i) {
                                                                            carIssueList.remove(i);
                                                                        }
                                                                        carIssuesAdapter.notifyDataSetChanged();
                                                                        refreshFromServer();
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        },
                                        currentYear,
                                        currentMonth,
                                        currentDay
                                );
                                TextView titleView = new TextView(MainActivity.this);
                                titleView.setText("When was this service completed?");
                                titleView.setBackgroundColor(getResources().getColor(R.color.date_picker_background));
                                titleView.setTextColor(getResources().getColor(R.color.white_text));
                                titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                titleView.setTextSize(18);
                                titleView.setPadding(10,10,10,10);

                                datePicker.setCustomTitle(titleView);

                                datePicker.show();
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
                            Toast.makeText(MainActivity.this, "Service request sent", Toast.LENGTH_SHORT).show();
                            for(CarIssue issue : dashboardCar.getActiveIssues()) {
                                networkHelper.servicePending(dashboardCar.getId(), issue.getId(), null);
                            }
                        } else {
                            Log.e(TAG, "service request: " + requestError.getMessage());
                            Toast.makeText(MainActivity.this, "There was an error, please try again", Toast.LENGTH_SHORT).show();
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

        setIssuesCount();
    }

    private void setIssuesCount() { // sets the number of active issues to display
        int total = dashboardCar.getActiveIssues().size();

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

                    sharedPreferences.edit().putInt(pfCurrentCar, mainCarId).commit();

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
                                        findViewById(R.id.no_car_text).setVisibility(View.GONE);
                                        setDashboardCar(carList);
                                        carLocalStore.deleteAllCars();
                                        carLocalStore.storeCars(carList);
                                        setCarDetailsUI();
                                    }
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

    private void setDealership() {

        Dealership shop = dashboardCar.getDealership();
        if(shop == null) { // need to load shop
            shop = shopLocalStore.getDealership(dashboardCar.getShopId());
            if(shop != null) { // shop is stored locally
                dashboardCar.setDealership(shop);
                dealershipName.setText(shop.getName());
            } else { // shop needs to be retrieved from server
                networkHelper.getShops(new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            try {
                                final List<Dealership> dl = Dealership.createDealershipList(response);
                                shopLocalStore.deleteAllDealerships();
                                shopLocalStore.storeDealerships(dl);

                                Dealership d = shopLocalStore.getDealership(carLocalStore.getCar(dashboardCar.getId()).getShopId());

                                dashboardCar.setDealership(d);
                                if (dashboardCar.getDealership() != null) {
                                    dealershipName.setText(dashboardCar.getDealership().getName());
                                } else {
                                    String[] shopNames = new String[dl.size()];

                                    for (int i = 0; i < shopNames.length; i++) {
                                        shopNames[i] = dl.get(i).getName();
                                    }

                                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                                    dialog.setTitle(String.format("Please select the dealership for your %s %s %s",
                                            dashboardCar.getYear(), dashboardCar.getMake(), dashboardCar.getModel()));
                                    dialog.setSingleChoiceItems(shopNames, -1, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dashboardCar.setDealership(dl.get(which));
                                        }
                                    });

                                    dialog.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (dashboardCar.getDealership() == null) {
                                                return;
                                            }
                                            dialog.dismiss();

                                            networkHelper.updateCarShop(dashboardCar.getId(), dashboardCar.getDealership().getId(),
                                                    new RequestCallback() {
                                                        @Override
                                                        public void done(String response, RequestError requestError) {
                                                            if (requestError == null) {
                                                                Log.i(TAG, "Dealership updated - carId: " + dashboardCar.getId() + ", dealerId: " + dashboardCar.getDealership().getId());
                                                                Toast.makeText(MainActivity.this, "Car dealership updated", Toast.LENGTH_SHORT).show();
                                                                setDealership();
                                                            } else {
                                                                Log.e(TAG, "Dealership update error: " + requestError.getError());
                                                                Toast.makeText(MainActivity.this, "There was an error, please try again", Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                        }
                                    });
                                    dialog.show();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else {
                            Log.e(TAG, "Get shops: " + requestError.getMessage());
                        }
                    }
                });
            }
        } else { // shop is good
            dashboardCar.setDealership(shop);
            dealershipName.setText(shop.getName());
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
    public void getIOData(DataPackageInfo dataPackageInfo) {
        if(dataPackageInfo.dtcData != null && !dataPackageInfo.dtcData.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    refreshFromServer();
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

    private void populateCarIssuesAdapter() {
        if(isLoading) {
            hideLoading();
        }

        // Try local store
        Log.i(TAG, "DashboardCar id: (Try local store) "+dashboardCar.getId());
        List<CarIssue> carIssues = carIssueLocalStore.getAllCarIssues(dashboardCar.getId());
        if(carIssues.isEmpty() && (dashboardCar.getNumberOfServices() > 0
                || dashboardCar.getNumberOfRecalls() > 0)) {
            Log.i(TAG, "No car issues in local store");

            networkHelper.getCarsById(dashboardCar.getId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if(requestError == null) {
                        try {
                            dashboardCar.setIssues(CarIssue.createCarIssues(
                                    new JSONObject(response).getJSONArray("issues"), dashboardCar.getId()));
                            carIssueList.clear();
                            carIssueList.addAll(dashboardCar.getActiveIssues());
                            carIssuesAdapter.notifyDataSetChanged();
                            setIssuesCount();
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

        } else {
            Log.i(TAG, "Trying local store for carIssues");
            dashboardCar.setIssues(carIssues);
            carIssueList.clear();
            carIssueList.addAll(dashboardCar.getActiveIssues());
            carIssuesAdapter.notifyDataSetChanged();
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

    public void startAddCarActivity(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Add Car", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(MainActivity.this, AddCarActivity.class);

        intent.putExtra(HAS_CAR_IN_DASHBOARD, true);
        startActivityForResult(intent, RC_ADD_CAR);
    }

    private void setDashboardCar(List<Car> carList) {
        int currentCarId = sharedPreferences.getInt(pfCurrentCar, -1);

        for(Car car : carList) {
            if(car.getId() == currentCarId) {
                dashboardCar = car;
                return;
            }
        }

        dashboardCar = carList.get(0);
        dashboardCar.setCurrentCar(true);
        carLocalStore.updateCar(dashboardCar);

        sharedPreferences.edit().putInt(pfCurrentCar, dashboardCar.getId()).commit();
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions,
                                            int[] grantResults) {
        if(requestCode == RC_LOCATION_PERM) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                autoConnectService.startBluetoothSearch();
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

            holder.date.setVisibility(View.GONE);

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
                        intent.putExtra(CAR_EXTRA, dashboardCar);
                        intent.putExtra(CAR_ISSUE_EXTRA, carIssueList.get(position));
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
            public View date; // Not used here so it is set to GONE

            public ViewHolder(View v) {
                super(v);
                title = (TextView) v.findViewById(R.id.title);
                description = (TextView) v.findViewById(R.id.description);
                imageView = (ImageView) v.findViewById(R.id.image_icon);
                container = (CardView) v.findViewById(R.id.list_car_item);
                date = v.findViewById(R.id.date);
            }
        }
    }
}
