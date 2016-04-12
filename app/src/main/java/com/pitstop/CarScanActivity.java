package com.pitstop;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.EdgeDetail;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.CarIssue;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.parse.ParseApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by Paul Soladoye  on 3/8/2016.
 */
public class CarScanActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener,
        EasyPermissions.PermissionCallbacks {

    private static final int RC_LOCATION_PERM = 101;
    private String[] perms = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    private ParseApplication application;
    private BluetoothAutoConnectService autoConnectService;
    private boolean serviceIsBound;

    private RelativeLayout loadingRecalls, recallsStateLayout, recallsCountLayout;
    private ImageView recallsState;
    private TextView recallsText, recallsCount;

    private RelativeLayout loadingServices, servicesStateLayout, servicesCountLayout;
    private ImageView servicesState;
    private TextView servicesText, servicesCount;

    private RelativeLayout loadingEngineIssues, engineIssuesStateLayout, engineIssuesCountLayout;
    private ImageView engineIssuesState;
    private TextView engineIssuesText, engineIssuesCount;

    private TextView carMileage;
    private Button carScanButton;
    private DecoView arcView;
    private TextView textPercent;
    private SeriesItem seriesItem;
    private int seriesIndex;

    private int numberOfIssues = 0;
    private int recalls = 0, services = 0;

    private Car dashboardCar;
    private boolean performedScan = false;


    private static String TAG = "CarScanActivity";

    private boolean askingForDtcs = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "onServiceConnection");
            // cast the IBinder and get MyService instance
            serviceIsBound = true;

            autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
            autoConnectService.setCallbacks(CarScanActivity.this); // register
            if(EasyPermissions.hasPermissions(CarScanActivity.this,perms)) {
                autoConnectService.startBluetoothSearch();
            } else {
                EasyPermissions.requestPermissions(CarScanActivity.this,
                        getString(R.string.location_request_rationale), RC_LOCATION_PERM, perms);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            serviceIsBound = false;
            autoConnectService = null;
            Log.i("Disconnecting","onServiceConnection");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_scan);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        application = (ParseApplication) getApplicationContext();
        bindService(new Intent(this, BluetoothAutoConnectService.class),
                serviceConnection, BIND_AUTO_CREATE);

        dashboardCar = (Car) getIntent().getSerializableExtra(MainActivity.CAR_EXTRA);

        try {
            application.getMixpanelAPI().track("View Appeared",
                    new JSONObject("{'View':'CarScanActivity'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setupUiReferences();
        carMileage.setText(String.valueOf(dashboardCar.getTotalMileage()));
    }

    @Override
    protected void onPause() {
        dtcRequestTimer.removeCallbacks(runnable);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(serviceIsBound) {
            Log.i(TAG, "unbinding service");
            unbindService(serviceConnection);
        }
    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent();
        data.putExtra(MainActivity.REFRESH_LOCAL, performedScan);
        setResult(MainActivity.RESULT_OK, data);
        finish();
    }

    private void setupUiReferences() {
        carMileage = (TextView) findViewById(R.id.car_mileage);

        carScanButton = (Button) findViewById(R.id.car_scan_btn);
        carScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    application.getMixpanelAPI().track("Button Clicked",
                            new JSONObject("{'Button':'Start car scan','View':'CarScanActivity'}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if(autoConnectService.isCommunicatingWithDevice()) {
                    updateMileage();
                } else {
                    tryAgainDialog();
                }
            }
        });

        recallsText = (TextView) findViewById(R.id.recalls_text);
        servicesText = (TextView) findViewById(R.id.services_text);
        engineIssuesText = (TextView) findViewById(R.id.engine_issues_text);

        recallsCount = (TextView) findViewById(R.id.recalls_count);
        servicesCount = (TextView) findViewById(R.id.services_count);
        engineIssuesCount = (TextView) findViewById(R.id.engine_issues_count);

        loadingRecalls = (RelativeLayout) findViewById(R.id.loading_recalls);
        loadingRecalls.setVisibility(View.GONE);
        recallsCountLayout = (RelativeLayout) findViewById(R.id.recalls_count_layout);

        recallsStateLayout = (RelativeLayout) findViewById(R.id.recalls_state_layout);
        recallsStateLayout.setVisibility(View.GONE);
        recallsState = (ImageView) findViewById(R.id.recalls_state_image_icon);

        loadingServices = (RelativeLayout) findViewById(R.id.loading_services);
        loadingServices.setVisibility(View.GONE);
        servicesCountLayout = (RelativeLayout) findViewById(R.id.services_count_layout);

        servicesStateLayout = (RelativeLayout) findViewById(R.id.services_state_layout);
        servicesStateLayout.setVisibility(View.GONE);
        servicesState = (ImageView) findViewById(R.id.services_state_image_icon);

        loadingEngineIssues = (RelativeLayout) findViewById(R.id.loading_engine_issues);
        loadingEngineIssues.setVisibility(View.GONE);
        engineIssuesCountLayout = (RelativeLayout) findViewById(R.id.engine_issues_count_layout);

        engineIssuesStateLayout = (RelativeLayout) findViewById(R.id.engine_issues_state_layout);
        engineIssuesStateLayout.setVisibility(View.GONE);
        engineIssuesState = (ImageView) findViewById(R.id.engine_issues_state_image_icon);

        setUpCarHealthMeter();
    }

    private void setUpCarHealthMeter() {

        arcView = (DecoView)findViewById(R.id.dynamicArcView);
        textPercent = (TextView)findViewById(R.id.textPercentage);

        // Create background track
        arcView.addSeries(new SeriesItem.Builder(Color.argb(255, 218, 218, 218))
                .setRange(0, 100, 100)
                .setInitialVisibility(true)
                .setLineWidth(40f)
                .build());

        //Create data series track
        seriesItem = new SeriesItem.Builder(Color.argb(255, 64, 196, 0))
                .setRange(0, 100, 100)
                .setLineWidth(40f)
                .addEdgeDetail(new EdgeDetail(EdgeDetail.EdgeType.EDGE_OUTER,
                        Color.parseColor("#22000000"), 0.4f))
                .build();

        final String format = "%.0f%%";

        seriesIndex = arcView.addSeries(seriesItem);

        seriesItem.addArcSeriesItemListener(new SeriesItem.SeriesItemListener() {
            @Override
            public void onSeriesItemAnimationProgress(float percentComplete, float currentPosition) {
                // We found a percentage so we insert a percentage
                float percentFilled =
                        ((currentPosition - seriesItem.getMinValue()) / (seriesItem.getMaxValue() - seriesItem.getMinValue())) * 100f;
                textPercent.setText(String.format(format, percentFilled));

                if (percentFilled > 75) {
                    seriesItem.setColor(Color.argb(255, 64, 196, 0));
                } else if (percentFilled > 40) {
                    seriesItem.setColor(Color.parseColor("#FFB300"));
                } else {
                    seriesItem.setColor(Color.parseColor("#E53935"));
                }
            }

            @Override
            public void onSeriesItemDisplayProgress(float percentComplete) {
            }
        });
    }

    public void updateMileage() {

        final EditText input = new EditText(CarScanActivity.this);
        input.setText(carMileage.getText().toString());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setRawInputType(Configuration.KEYBOARD_12KEY);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(CarScanActivity.this);

        alertDialog.setTitle("Update Mileage");
        alertDialog.setView(input);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                carScanButton.setEnabled(false);
                recallsStateLayout.setVisibility(View.GONE);
                recallsCountLayout.setVisibility(View.GONE);
                loadingRecalls.setVisibility(View.VISIBLE);
                recallsText.setText("Checking for recalls");

                servicesStateLayout.setVisibility(View.GONE);
                servicesCountLayout.setVisibility(View.GONE);
                loadingServices.setVisibility(View.VISIBLE);
                servicesText.setText("Checking for services");

                engineIssuesStateLayout.setVisibility(View.GONE);
                engineIssuesCountLayout.setVisibility(View.GONE);
                loadingEngineIssues.setVisibility(View.VISIBLE);
                engineIssuesText.setText("Checking for engine issues");

                updateMileage(input.getText().toString());
                dialogInterface.dismiss();
            }
        }).show();
    }

    private void updateMileage (String mileage) {
        Log.i(TAG, mileage);

        carMileage.setText(mileage);
        Car dashboardCar = (Car) getIntent().getSerializableExtra(MainActivity.CAR_EXTRA);

        final HashMap<String, Object> params = new HashMap<String, Object>();

        params.put("carVin", dashboardCar.getVin());
        params.put("mileage", Integer.valueOf(mileage));

        // update the server information
        ParseCloud.callFunctionInBackground("carServicesUpdate", params, new FunctionCallback<Object>() {
            public void done(Object o, ParseException e) {
                if (e == null) {
                    startCarScan();
                } else {
                    Toast.makeText(CarScanActivity.this,
                            "failed to update mileage", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Parse Error: " + e.getMessage());

                    carScanButton.setEnabled(true);
                    recallsCountLayout.setVisibility(View.VISIBLE);
                    loadingRecalls.setVisibility(View.GONE);
                    recallsText.setText("Recalls");

                    servicesCountLayout.setVisibility(View.VISIBLE);
                    loadingServices.setVisibility(View.GONE);
                    servicesText.setText("Services");

                    engineIssuesCountLayout.setVisibility(View.VISIBLE);
                    loadingEngineIssues.setVisibility(View.GONE);
                    engineIssuesText.setText("Engine issues");
                }
            }
        });
    }

    private void startCarScan() {

        performedScan = true;

        checkForServices();

        checkForEngineIssues();
    }

    private void checkForServices() {

        String userId = "";
        services = 0;
        recalls = 0;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
        if (ParseUser.getCurrentUser() != null) {
            userId = ParseUser.getCurrentUser().getObjectId();
        }
        query.whereContains("owner", userId);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    Car currentCar = getMainCar(Car.createCarsList(objects));

                    if (currentCar != null) {
                        loadingServices.setVisibility(View.GONE);
                        loadingRecalls.setVisibility(View.GONE);

                        recalls += currentCar.getNumberOfRecalls();

                        services += currentCar.getPendingEdmundServicesIds().size();
                        services += currentCar.getPendingFixedServicesIds().size();
                        services += currentCar.getPendingIntervalServicesIds().size();

                        numberOfIssues += services;
                        numberOfIssues += recalls;
                        updateCarHealthMeter();

                        if (services > 0) {
                            servicesCountLayout.setVisibility(View.VISIBLE);
                            servicesCount.setText(String.valueOf(services));
                            servicesText.setText("Services");

                            Drawable background = servicesCountLayout.getBackground();
                            GradientDrawable gradientDrawable = (GradientDrawable) background;
                            gradientDrawable.setColor(Color.rgb(203, 77, 69));

                        } else {
                            servicesStateLayout.setVisibility(View.VISIBLE);
                            servicesText.setText("No services due");
                        }

                        if (recalls > 0) {
                            recallsCountLayout.setVisibility(View.VISIBLE);
                            recallsCount.setText(String.valueOf(recalls));
                            recallsText.setText("Recalls");

                            Drawable background = recallsCountLayout.getBackground();
                            GradientDrawable gradientDrawable = (GradientDrawable) background;
                            gradientDrawable.setColor(Color.rgb(203, 77, 69));

                        } else {
                            recallsStateLayout.setVisibility(View.VISIBLE);
                            recallsText.setText("No recalls");
                        }

                    } else {
                        Log.i(TAG, "Main car is null");
                    }

                    if (!loadingEngineIssues.isShown() && !loadingRecalls.isShown() &&
                            !loadingServices.isShown()) {
                        carScanButton.setEnabled(true);
                    }
                } else {
                    Log.i(TAG, e.getMessage());
                }
            }
        });
    }

    private void checkForEngineIssues() {
        dtcCodes.clear();
        askingForDtcs = true;
        startTime = System.currentTimeMillis();
        dtcRequestTimer.post(runnable);
        autoConnectService.getPendingDTCs();
        autoConnectService.getDTCs();
    }

    private void tryAgainDialog() {

        if(isFinishing()) { // You don't want to add a dialog to a finished activity
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(CarScanActivity.this);
        alertDialog.setTitle("Device not connected");

        // Alert message
        alertDialog.setMessage("Make sure your vehicle engine is on and " +
                "OBD device is properly plugged in.\n\nTry again ?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    autoConnectService.startBluetoothSearch();
                }
                (carScanButton).performClick();
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });
        alertDialog.show();
    }

    private void updateCarHealthMeter() {

        if(numberOfIssues >= 3) {
            arcView.addEvent(new DecoEvent.Builder(30)
                    .setIndex(seriesIndex).build());
        } else if(numberOfIssues < 3 && numberOfIssues > 0) {
            arcView.addEvent(new DecoEvent.Builder(65)
                    .setIndex(seriesIndex).build());
        } else {
            arcView.addEvent(new DecoEvent.Builder(100)
                    .setIndex(seriesIndex).build());
        }
    }

    private Car getMainCar(List<Car> cars) {
        for(Car car : cars) {
            if(car.isCurrentCar()) {
                return car;
            }
        }
        return null;
    }

    public void checkIssues(View view) {
        onBackPressed();
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

    private Set<String> dtcCodes = new HashSet<>();

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        Log.i(MainActivity.TAG, "Result "+dataPackageInfo.result);
        Log.i(MainActivity.TAG, "DTC "+dataPackageInfo.dtcData);

        if(dataPackageInfo.dtcData != null && !dataPackageInfo.dtcData.equals("") && askingForDtcs) {

            String[] dtcs = dataPackageInfo.dtcData.split(",");
            for(String dtc : dtcs) {
                dtcCodes.add(dtc);
            }

            numberOfIssues = services + recalls + dtcCodes.size();
            updateCarHealthMeter();

            loadingEngineIssues.setVisibility(View.GONE);
            engineIssuesStateLayout.setVisibility(View.GONE);
            engineIssuesCountLayout.setVisibility(View.VISIBLE);
            engineIssuesCount.setText(String.valueOf(dtcCodes.size()));
            engineIssuesText.setText("Engine issues");

            Drawable background = engineIssuesCountLayout.getBackground();
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setColor(Color.rgb(203, 77, 69));
        }
    }

    private long startTime = 0;
    Handler dtcRequestTimer = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 0 ) {
                if(dtcCodes.isEmpty()) {
                    loadingEngineIssues.setVisibility(View.GONE);
                    engineIssuesCountLayout.setVisibility(View.GONE);
                    engineIssuesStateLayout.setVisibility(View.VISIBLE);
                    engineIssuesText.setText("No Engine Issues");
                }
                updateCarHealthMeter();
                carScanButton.setEnabled(true);
            }
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - startTime;
            int seconds = (int) (timeDiff / 1000);

            if(seconds > 10 && askingForDtcs) {
                askingForDtcs = false;
                dtcRequestTimer.sendEmptyMessage(0);
                dtcRequestTimer.removeCallbacks(runnable);
            } else {
                dtcRequestTimer.post(runnable);
            }

        }
    };

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

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
}
