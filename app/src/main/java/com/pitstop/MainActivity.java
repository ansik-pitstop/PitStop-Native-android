package com.pitstop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
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
import com.pitstop.DataAccessLayer.DataAdapters.CarAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.CarIssueAdapter;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.database.DBModel;
import com.pitstop.parse.ParseApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;

public class MainActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener {
    public static Intent serviceIntent;

    public static int RC_ADD_CAR = 50;
    public static int RC_SCAN_CAR = 51;
    public static int RC_SETTINGS = 52;

    public static int RESULT_OK = 60;


    public ArrayList<DBModel> array;

    final static String pfName = "com.pitstop.login.name";
    final static String pfCodeForObjectID = "com.pitstop.login.objectID";

    final static String pfShopName = "com.pitstop.shop.name";
    final static String pfCodeForShopObjectID = "com.pitstop.shop.objectID";

    public static String CAR_EXTRA = "car";
    public static String CAR_ISSUE_EXTRA = "car_issue";
    public static String CAR_LIST_EXTRA = "car_list";
    public static String HAS_CAR_IN_DASHBOARD = "has_car";
    public static String REFRESH_LOCAL = "refresh_local";

    public static MixpanelAPI mixpanelAPI;

    private boolean isLoading = false;

    private boolean isUpdatingMileage = false;

    private RecyclerView recyclerView;
    private CustomAdapter carIssuesAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private RelativeLayout mainView;
    private RelativeLayout connectedCarIndicator;
    private RelativeLayout serviceCountBackground;
    private RelativeLayout addressLayout, phoneNumberLayout, chatLayout;
    private TextView serviceCountText;

    private TextView carName, dealershipName;
    private Button carScan;

    private ProgressDialog progressDialog;

    private List<ParseObject> cars = new ArrayList<>();
    private Car dashboardCar;
    private List<Car> carList = new ArrayList<>();
    private List<CarIssue> carIssueList = new ArrayList<>();

    private CarAdapter carAdapter;
    private CarIssueAdapter carIssueAdapter;



    public static String TAG = "MainActivity --> ";

    private BluetoothAutoConnectService service;
    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            Log.i("connecting","onServiceConnection");
            // cast the IBinder and get MyService instance
            BluetoothAutoConnectService.BluetoothBinder binder = (BluetoothAutoConnectService.BluetoothBinder) service1;
            service = binder.getService();
            service.setCallbacks(MainActivity.this); // register
            if (BluetoothAdapter.getDefaultAdapter()!=null&&BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                service.startBluetoothSearch();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            Log.i("Disconnecting","onServiceConnection");
        }
    };

    Handler indicatorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 0) {
                if(service != null && service.isCommunicatingWithDevice()) {
                    updateConnectedCarIndicator(true);
                } else {
                    updateConnectedCarIndicator(false);
                }
                indicatorHandler.post(runnable);
            }
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            indicatorHandler.sendEmptyMessage(0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mixpanelAPI = ParseApplication.mixpanelAPI;
        serviceIntent= new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "On create..");

        carAdapter = new CarAdapter(this);
        carIssueAdapter = new CarIssueAdapter(this);

        //setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.primary));
        setSupportActionBar(toolbar);

        setUpUIReferences();
        getCarDetails();
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
            refreshUi();
            return true;
        } else if(id == R.id.add) {
            startAddCarActivity();
            return true;
        } else if(id == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);

            IntentProxyObject proxyObject = new IntentProxyObject();

            proxyObject.setCarList(carList);
            intent.putExtra(CAR_LIST_EXTRA,proxyObject);
            startActivityForResult(intent, RC_SETTINGS);

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "onResume");
        /*if(dashboardCar !=null) {
            refreshUi();
        }*/

        indicatorHandler.postDelayed(runnable, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_ADD_CAR && resultCode==AddCarActivity.ADD_CAR_SUCCESS) {

            if(data.getBooleanExtra(REFRESH_LOCAL,false)) {
                refreshUi();
            } else {
                dashboardCar = (Car) data.getSerializableExtra(CAR_EXTRA);
            }
        } else if(requestCode == RC_SCAN_CAR && resultCode == RESULT_OK) {
            if(data.getBooleanExtra(REFRESH_LOCAL,false)) {
                refreshUi();
            }
        } else if(requestCode == RC_SETTINGS && resultCode == RESULT_OK) {
            if(data.getBooleanExtra(REFRESH_LOCAL,false)) {
                refreshUi();
            }
        }
    }

    @Override
    protected void onPause() {
        indicatorHandler.removeCallbacks(runnable);
        unbindService(serviceConnection);
        ParseApplication.mixpanelAPI.flush();
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    /**
     * UI update methods
     */
    private void setUpUIReferences() {

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

        carScan = (Button) findViewById(R.id.car_scan_btn);
        carScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CarScanActivity.class);
                intent.putExtra(CAR_EXTRA,dashboardCar);

                startActivityForResult(intent, RC_SCAN_CAR);
            }
        });

        addressLayout = (RelativeLayout) findViewById(R.id.address_layout);
        addressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uri = String.format(Locale.ENGLISH,
                        "http://maps.google.com/maps?daddr=%s",
                        dashboardCar.getDealerShip().getAddress());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        });

        phoneNumberLayout = (RelativeLayout) findViewById(R.id.phone_layout);
        phoneNumberLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                        dashboardCar.getDealerShip().getPhone()));
                startActivity(intent);
            }
        });

        chatLayout = (RelativeLayout) findViewById(R.id.chat_layout);
        chatLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        mainView.setVisibility(View.VISIBLE);
        isLoading = false;
    }

    private void showLoading(String text) {
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
                                //--DTCS are disabled still TODO add DTCS Response
                                if(carIssuesAdapter.getItemViewType(position)
                                        == CustomAdapter.VIEW_TYPE_EMPTY) {
                                    return false;
                                }

                                CarIssue carIssue = carIssuesAdapter.getItem(position);
                                return !carIssue.getIssueType().equals("dtc");
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView,
                                                               final int[] reverseSortedPositions) {

                                try {
                                    ParseApplication.mixpanelAPI.track("Button Clicked",
                                            new JSONObject("{'Button':'Swiped Away Service/Recall'," +
                                                    "'View':'MainActivity'}"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

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

    public void requestMultiService(View view) {
        try {
            ParseApplication.mixpanelAPI.track("Button Clicked",
                    new JSONObject("{'Button':'Request Service','View':'MainActivity'}"));
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
                additionalComment[0] = userInput.getText().toString();
                sendRequest(additionalComment[0]);
            }
        });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    private void sendRequest(String additionalComment) {

        String userId = ParseUser.getCurrentUser().getObjectId();
        HashMap<String,Object> output = new HashMap<>();
        List<HashMap<String,String>> services = new ArrayList<>();

        for(CarIssue carIssue : carIssueList ) {
            if(carIssue.getIssueType().equals("recall")) {


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

        ParseCloud.callFunctionInBackground("sendServiceRequestEmail", output, new FunctionCallback<Object>() {
            @Override
            public void done(Object object, ParseException e) {
                if (e == null) {
                    Toast.makeText(MainActivity.this,
                            "Request sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getCarDetails() {


        showLoading("Retrieving car details");

        // Try local store
        List<Car> localCars = carAdapter.getAllCars();

        if(localCars.isEmpty()) {
            loadCarDetailsFromServer();
        } else {
            Log.i(MainActivity.TAG,"Trying local store for cars");
            carList = localCars;

            setDashboardCar(carList);
            setCarDetailsUI();
            hideLoading();
        }
    }

    private void setCarDetailsUI() {
        setDealership();
        populateCarIssuesAdapter();

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

    private void loadCarDetailsFromServer() {
        String userId = "";

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
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
                        carAdapter.storeCars(carList);
                        setCarDetailsUI();

                    } else {
                        startAddCarActivity();
                        if (isLoading) {
                            hideLoading();
                        }
                    }

                } else {
                    if (isLoading) {
                        hideLoading();
                    }
                    Toast.makeText(MainActivity.this,
                            "Error retrieving car details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setDealership() {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Shop");
        query.whereEqualTo("objectId", dashboardCar.getShopId());

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null) {
                    Dealership dealership = Dealership
                            .createDealership(objects.get(0), dashboardCar.getParseId());
                    dashboardCar.setDealerShip(dealership);
                    dealershipName.setText(dashboardCar.getDealerShip().getName());
                } else {
                    Toast.makeText(MainActivity.this, "Failed to retrieve dealership info",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public void getBluetoothState(int state) {
        if(state==BluetoothManage.DISCONNECTED) {
            Log.i(BluetoothAutoConnectService.R4_TAG,"Bluetooth disconnected");
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
                equals(String.valueOf(BluetoothAutoConnectService.DEVICE_LOGOUT))) {
            Log.i(BluetoothAutoConnectService.R4_TAG, "Device logout");
        }
    }

    private void populateCarIssuesAdapter() {
        if(isLoading) {
            hideLoading();
        }

        // Try local store
        List<CarIssue> carIssues = carIssueAdapter.getAllCarIssues(dashboardCar.getParseId());
        if(carIssues.isEmpty()) {
            Log.i(TAG, "No car issues in local store");
            List<String> issueIds;

            issueIds = dashboardCar.getPendingEdmundServicesIds();
            getIssues(issueIds, "EdmundsService", CarIssue.EDMUNDS);

            issueIds = dashboardCar.getPendingFixedServicesIds();
            getIssues(issueIds, "ServiceFixed", CarIssue.FIXED);

            issueIds = dashboardCar.getPendingIntervalServicesIds();
            getIssues(issueIds, "ServiceInterval", CarIssue.INTERVAL);

            /*issueIds = car.getList("recalls");
            getIssues(issueIds, "RecallEntry", "recall");*/

            List<String> dtcCodes = dashboardCar.getStoredDTCs();
            getDTCs(dtcCodes, "DTC", CarIssue.DTC);
        } else {
            Log.i(MainActivity.TAG, "Trying local store for carIssues");
            Log.i(TAG,"CarIssues count: "+carIssues.size());
            dashboardCar.setIssues(carIssues);
            carIssueList.clear();
            carIssueList.addAll(dashboardCar.getIssues());
            carIssuesAdapter.notifyDataSetChanged();
        }


    }

    private void getIssues(List<String> serviceIds, String resource, final String issueType) {

        ParseQuery<ParseObject> query = ParseQuery.getQuery(resource);
        query.whereContainedIn("objectId", serviceIds);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    Log.i(TAG, "Parse objects count: " + objects.size());

                    List<CarIssue> issues = CarIssue.createCarIssues(objects, issueType,
                            dashboardCar.getParseId());
                    dashboardCar.getIssues().addAll(issues);
                    // Store in local
                    carIssueAdapter.storeCarIssues(issues);

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
                    Log.i(TAG, "Parse objects count: " + objects.size());

                    List<CarIssue> dtcs = CarIssue.createCarIssues(objects, issueType,
                            dashboardCar.getParseId());
                    Log.i(TAG, "DTC count: "+ dtcs.size());
                    dashboardCar.getIssues().addAll(dtcs);
                    // Store in local
                    carIssueAdapter.storeCarIssues(dtcs);

                    carIssueList.clear();
                    carIssueList.addAll(dashboardCar.getIssues());
                    carIssuesAdapter.notifyDataSetChanged();

                } else {
                    Log.i(TAG, "Parse error: " + e.getMessage());
                }
            }
        });
    }

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

        if(!carIssue.getIssueType().equals("recall") || !carIssue.getIssueType().equals("dtc")) {
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
        } else {
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
        }
    }

    private void updateCarOnParse(final int typeService, final CarIssue carIssue
            ,final int[] reverseSortedPositions) {
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

                            Log.i("Car parse (Adapter)-->",obj1.getIssueDetail().getItem());
                            Log.i("Car on parse -->", obj2.getIssueDetail().getItem());
                            carIssueList.remove(position);
                            carIssuesAdapter.notifyDataSetChanged();
                        }
                        refreshUi();
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
                            CarIssue obj1 = carIssuesAdapter.getItem(position);
                            CarIssue obj2 = carIssueList.get(position);

                            Log.i("Recall (Adapter)-->",obj1.getIssueDetail().getItem());
                            Log.i("Recall parse -->", obj2.getIssueDetail().getItem());

                            carIssueList.remove(position);
                            carIssuesAdapter.notifyDataSetChanged();
                        }
                        refreshUi();
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

    private void refreshUi() {
        carIssueList.clear();
        carAdapter.deleteAllCars();
        carIssueAdapter.deleteAllCarIssues();
        getCarDetails();
    }

    private void startAddCarActivity() {
        Intent intent = new Intent(MainActivity.this, AddCarActivity.class);
        if(dashboardCar != null) {
            intent.putExtra(HAS_CAR_IN_DASHBOARD, true);
            intent.putExtra(CAR_EXTRA, dashboardCar);
        }
        startActivityForResult(intent, RC_ADD_CAR);
    }

    private void setDashboardCar(List<Car> carList) {
        for(Car car : carList) {
            if(car.isCurrentCar()) {
                dashboardCar = car;
                break;
            }
        }
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
            Log.i(TAG,"On bind view holder");

            int viewType = getItemViewType(position);

            if(viewType == VIEW_TYPE_EMPTY) {
                holder.description.setMaxLines(2);
                holder.description.setText("You have no pending Engine Code, Recalls or Services");
                holder.title.setText("Congrats!");
                holder.imageView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_check_circle_green_400_36dp));
            } else {
                final CarIssue carIssue = carIssueList.get(position);

                //holder.description.setSingleLine();
                holder.description.setText(carIssue.getIssueDetail().getDescription());
                holder.description.setEllipsize(TextUtils.TruncateAt.END);
                if(carIssue.getIssueType().equals("recall")) {
                    holder.title.setText(carIssue.getIssueDetail().getItem());
                    holder.imageView.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_error_red_600_24dp));

                } else if(carIssue.getIssueType().equals("dtc")) {
                    holder.title.setText(carIssue.getIssueDetail().getItem());
                    holder.imageView.setImageDrawable(getResources().
                            getDrawable(R.drawable.ic_announcement_blue_600_24dp));

                } else {
                    holder.description.setText(carIssue.getIssueDetail().getDescription());
                    holder.title.setText(carIssue.getIssueDetail().getItem());
                    holder.imageView.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_warning_amber_300_24dp));
                }

                holder.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, DisplayItemActivity.class);
                        intent.putExtra(CAR_ISSUE_EXTRA, carIssueList.get(position));
                        intent.putExtra(CAR_EXTRA,dashboardCar);
                        startActivity(intent);
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
            if(carIssueList.isEmpty()) {
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
