package com.pitstop.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.Utils;
import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.EdgeDetail;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;
import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Paul Soladoye  on 3/8/2016.
 */
public class CarScanActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener {

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
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

    private int searchAttempts = 0;

    private TextView carMileage;
    private Button carScanButton;
    private DecoView arcView;
    private TextView textPercent;
    private SeriesItem seriesItem;
    private int seriesIndex;

    private int numberOfIssues = 0;
    private int recalls = 0, services = 0;

    private Car dashboardCar;
    private double baseMileage;
    private boolean updatedMileageOrDtcsFound = false;
    private ProgressDialog progressDialog;

    private LocalCarAdapter localCarAdapter;

    private NetworkHelper networkHelper;

    private static String TAG = CarScanActivity.class.getSimpleName();

    private boolean askingForDtcs = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "onServiceConnection");
            // cast the IBinder and get MyService instance
            serviceIsBound = true;

            autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
            autoConnectService.setCallbacks(CarScanActivity.this); // register

            if (BluetoothAdapter.getDefaultAdapter()!=null) {

                if(ContextCompat.checkSelfPermission(CarScanActivity.this, MainActivity.LOC_PERMS[0]) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(CarScanActivity.this, MainActivity.LOC_PERMS[1]) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(CarScanActivity.this, MainActivity.LOC_PERMS, MainActivity.RC_LOCATION_PERM);
                } else {
                    //autoConnectService.startBluetoothSearch();
                }
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

        networkHelper = new NetworkHelper(getApplicationContext());

        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);
        bindService(new Intent(this, BluetoothAutoConnectService.class),
                serviceConnection, BIND_AUTO_CREATE);

        localCarAdapter = new LocalCarAdapter(this);

        dashboardCar = getIntent().getParcelableExtra(MainActivity.CAR_EXTRA);

        try {
            mixpanelHelper.trackViewAppeared(TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setupUiReferences();
        baseMileage = dashboardCar.getTotalMileage();
        carMileage.setText(String.valueOf(((int) (localCarAdapter.getCar(dashboardCar.getId()).getDisplayedMileage() * 100)) / 100.0));

        if(!BuildConfig.DEBUG) {
            findViewById(R.id.update_mileage).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(runnable);
        handler.removeCallbacks(connectCar);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id ==  android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra(MainActivity.REFRESH_FROM_SERVER, updatedMileageOrDtcsFound);
        setResult(MainActivity.RESULT_OK, data);
        super.finish();
        overridePendingTransition(R.anim.activity_slide_right_in, R.anim.activity_slide_right_out);
    }

    public void returnToMainActivity(View view) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == MainActivity.RC_ENABLE_BT
                && resultCode == MainActivity.RC_ENABLE_BT) {
            carScanButton.performClick();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void setupUiReferences() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        carMileage = (TextView) findViewById(R.id.car_mileage);

        carScanButton = (Button) findViewById(R.id.car_scan_btn);

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

    public void updateMileage(final View view) {
        if(isFinishing() || isDestroyed()) {
            return;
        }
        //else if(autoConnectService.getState() == IBluetoothCommunicator.CONNECTED &&
        //        dashboardCar.getDisplayedMileage() - baseMileage > 1.0) {
        //    Toast.makeText(this, "Mileage must be updated at the start of a trip", Toast.LENGTH_LONG).show();
        //    return;
        //}

        final EditText input = new EditText(CarScanActivity.this);
        input.setText(String.valueOf((int) Double.parseDouble(carMileage.getText().toString())));
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setRawInputType(Configuration.KEYBOARD_12KEY);

        if(!showingDialog) {
            showingDialog = true;
            new AlertDialog.Builder(CarScanActivity.this)
                    .setTitle("Update Mileage")
                    .setView(input)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                mixpanelHelper.trackButtonTapped("Confirm Scan", TAG);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            updatedMileageOrDtcsFound = true;

                            showingDialog = false;
                            // POST (entered mileage - the trip mileage) so (mileage in backend + trip mileage) = entered mileage
                            final double mileage = Double.parseDouble(input.getText().toString())
                                    - (dashboardCar.getDisplayedMileage() - baseMileage);
                            if (mileage > 20000000) {
                                Toast.makeText(CarScanActivity.this, "Please enter valid mileage", Toast.LENGTH_SHORT).show();
                            } else {
                                networkHelper.updateCarMileage(dashboardCar.getId(), mileage, new RequestCallback() {
                                            @Override
                                            public void done(String response, RequestError requestError) {
                                                if (requestError == null) {
                                                    Toast.makeText(CarScanActivity.this, "Mileage updated", Toast.LENGTH_SHORT).show();
                                                    dashboardCar.setDisplayedMileage(Double.parseDouble(input.getText().toString()));
                                                    dashboardCar.setTotalMileage(mileage);
                                                    localCarAdapter.updateCar(dashboardCar);
                                                    baseMileage = mileage;
                                                    carMileage.setText(input.getText().toString());

                                                    if(autoConnectService.getState() == IBluetoothCommunicator.CONNECTED && autoConnectService.getLastTripId() != -1) {
                                                        networkHelper.updateMileageStart(mileage, autoConnectService.getLastTripId(), null);
                                                    }

                                                    if(view == null) {
                                                        if (IBluetoothCommunicator.CONNECTED == autoConnectService.getState() || autoConnectService.isCommunicatingWithDevice()) {
                                                            startCarScan();
                                                        } else {
                                                            progressDialog.setMessage("Connecting to car");
                                                            progressDialog.show();
                                                            carSearchStartTime = System.currentTimeMillis();
                                                            autoConnectService.startBluetoothSearch();  // for car scan
                                                            handler.postDelayed(connectCar, 3000);
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(CarScanActivity.this, "An error occurred while updating mileage. Please try again.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });

                                dialogInterface.dismiss();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showingDialog = false;
                            dialog.cancel();
                        }
                    })
                    .show();
        }
    }

    private boolean showingDialog = false;

    public void startCarScan(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Start car scan", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MainActivity.RC_ENABLE_BT);
            return;
        }

        updateMileage(null);
    }

    private void startCarScan() {
        Log.i(TAG, "Starting car scan");
        autoConnectService.manuallyUpdateMileage = true;
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

        checkForServices();

        checkForEngineIssues();
    }

    private void checkForServices() {

        services = 0;
        recalls = 0;

        networkHelper.getCarsById(dashboardCar.getId(), new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(requestError == null) {
                    try {
                        Object issuesArr = new JSONObject(response).get("issues");
                        ArrayList<CarIssue> issues = new ArrayList<>();

                        if (issuesArr instanceof JSONArray) {
                            issues = CarIssue.createCarIssues((JSONArray) issuesArr, dashboardCar.getId());
                        }

                        for(CarIssue issue : issues) {
                            if(!issue.getStatus().equals(CarIssue.ISSUE_DONE)) {
                                if (issue.getIssueType().equals(CarIssue.RECALL)) {
                                    ++recalls;
                                } else if (issue.getIssueType().contains(CarIssue.SERVICE)) {
                                    ++services;
                                }
                            }
                        }

                        loadingServices.setVisibility(View.GONE);
                        loadingRecalls.setVisibility(View.GONE);

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

                        if (!loadingEngineIssues.isShown() && !loadingRecalls.isShown() &&
                                !loadingServices.isShown()) {
                            carScanButton.setEnabled(true);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e(TAG, "getCarsById response: " + requestError.getMessage());
                }
            }
        });
    }

    private void checkForEngineIssues() {
        dtcCodes.clear();
        askingForDtcs = true;
        startTime = System.currentTimeMillis();
        handler.post(runnable);
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
                try {
                    mixpanelHelper.trackButtonTapped("Retry Scan (Vehicle not connected)", TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                (carScanButton).performClick();
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mixpanelHelper.trackButtonTapped("Cancel (Vehicle not connected)", TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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

    @Override
    public void getBluetoothState(int state) {
        Log.i(TAG, "getBluetoothState: " + state);
        if(state == IBluetoothCommunicator.CONNECTED) {
            handler.sendEmptyMessage(1);
            handler.removeCallbacks(runnable);
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
    public void tripData(TripInfoPackage tripInfoPackage) {
        if(tripInfoPackage.flag == TripInfoPackage.TripFlag.UPDATE) { // live mileage update
            final double newTotalMileage = ((int) (baseMileage + tripInfoPackage.mileage * 100)) / 100.0; // round to 2 decimal places

            Log.v(TAG, "Mileage updated: tripMileage: " + tripInfoPackage.mileage + ", baseMileage: " + baseMileage + ", newMileage: " + newTotalMileage);

            if(dashboardCar.getDisplayedMileage() < newTotalMileage) {
                dashboardCar.setDisplayedMileage(newTotalMileage);
                localCarAdapter.updateCar(dashboardCar);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        carMileage.startAnimation(AnimationUtils.loadAnimation(CarScanActivity.this, R.anim.mileage_update));
                    }
                });
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    carMileage.setText(String.valueOf(newTotalMileage));
                }
            });
        }
    }

    @Override
    public void parameterData(ParameterPackage parameterPackage) {
    }

    @Override
    public void pidData(PidPackage pidPackage) {

    }

    private Set<String> dtcCodes = new HashSet<>();

    @Override
    public void dtcData(DtcPackage dtcPackage) {
        Log.i(TAG, "DTC data received: " + dtcPackage.dtcNumber);
        if(dtcPackage.dtcs != null && askingForDtcs) {
            dtcCodes.addAll(Arrays.asList(dtcPackage.dtcs));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    numberOfIssues = services + recalls + dtcCodes.size();
                    updateCarHealthMeter();

                    if(numberOfIssues != 0) {
                        updatedMileageOrDtcsFound = true;
                    }

                    loadingEngineIssues.setVisibility(View.GONE);
                    engineIssuesStateLayout.setVisibility(View.GONE);
                    engineIssuesCountLayout.setVisibility(View.VISIBLE);
                    engineIssuesCount.setText(String.valueOf(dtcCodes.size()));
                    engineIssuesText.setText("Engine issues");

                    Log.i(TAG, "Finished car scan, dtcs found");
                    handler.removeCallbacks(runnable);

                    Drawable background = engineIssuesCountLayout.getBackground();
                    GradientDrawable gradientDrawable = (GradientDrawable) background;
                    gradientDrawable.setColor(Color.rgb(203, 77, 69));
                }
            });
        }
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        Log.i(MainActivity.TAG, "Result "+dataPackageInfo.result);
        Log.i(MainActivity.TAG, "DTC "+dataPackageInfo.dtcData);

        if(dataPackageInfo.result == 5 && dataPackageInfo.tripMileage != null && !dataPackageInfo.tripMileage.isEmpty()) { // live mileage update
            final double newTotalMileage = ((int) ((baseMileage
                    + Double.parseDouble(dataPackageInfo.tripMileage)/1000) * 100)) / 100.0; // round to 2 decimal places

            Log.v(TAG, "Mileage updated: tripMileage: " + dataPackageInfo.tripMileage + ", baseMileage: " + baseMileage + ", newMileage: " + newTotalMileage);

            if(dashboardCar.getDisplayedMileage() < newTotalMileage) {
                dashboardCar.setDisplayedMileage(newTotalMileage);
                localCarAdapter.updateCar(dashboardCar);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        carMileage.startAnimation(AnimationUtils.loadAnimation(CarScanActivity.this, R.anim.mileage_update));
                        //carMileage.setText(String.valueOf(newTotalMileage));
                    }
                });
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    carMileage.setText(String.valueOf(newTotalMileage));
                }
            });
        }

        //if(!Utils.isEmpty(dataPackageInfo.dtcData) && askingForDtcs) {
//
        //    String[] dtcs = dataPackageInfo.dtcData.split(",");
        //    for(String dtc : dtcs) {
        //        dtcCodes.add(dtc);
        //    }
//
        //    runOnUiThread(new Runnable() {
        //        @Override
        //        public void run() {
        //            numberOfIssues = services + recalls + dtcCodes.size();
        //            updateCarHealthMeter();
//
        //            if(numberOfIssues != 0) {
        //                updatedMileageOrDtcsFound = true;
        //            }
//
        //            loadingEngineIssues.setVisibility(View.GONE);
        //            engineIssuesStateLayout.setVisibility(View.GONE);
        //            engineIssuesCountLayout.setVisibility(View.VISIBLE);
        //            engineIssuesCount.setText(String.valueOf(dtcCodes.size()));
        //            engineIssuesText.setText("Engine issues");
//
        //            Log.i(TAG, "Finished car scan, dtcs found");
        //            handler.removeCallbacks(runnable);
//
        //            Drawable background = engineIssuesCountLayout.getBackground();
        //            GradientDrawable gradientDrawable = (GradientDrawable) background;
        //            gradientDrawable.setColor(Color.rgb(203, 77, 69));
        //        }
        //    });
        //}
    }

    private long startTime = 0;
    private long carSearchStartTime = 0;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(isFinishing() || isDestroyed()) {
                return;
            }
            switch (msg.what) {
                case 0: {
                    Log.i(TAG, "Finished car scan, no dtcs");
                    if(dtcCodes.isEmpty()) {
                        loadingEngineIssues.setVisibility(View.GONE);
                        engineIssuesCountLayout.setVisibility(View.GONE);
                        engineIssuesStateLayout.setVisibility(View.VISIBLE);
                        engineIssuesText.setText("No Engine Issues");
                    } else {
                        updatedMileageOrDtcsFound = true;
                    }
                    updateCarHealthMeter();
                    carScanButton.setEnabled(true);
                    break;
                }

                case 1: {
                    progressDialog.dismiss();
                    startCarScan();
                    break;
                }

                case 2: {
                    searchAttempts = 0;
                    progressDialog.dismiss();
                    tryAgainDialog();
                    break;
                }
            }
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - startTime;
            int seconds = (int) (timeDiff / 1000);

            if(seconds > 20 && askingForDtcs) {
                askingForDtcs = false;
                handler.sendEmptyMessage(0);
                handler.removeCallbacks(runnable);
            } else {
                handler.post(runnable);
            }

        }
    };

    private Runnable connectCar = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - carSearchStartTime;
            int seconds = (int) (timeDiff / 1000);
            if(autoConnectService.getState() == IBluetoothCommunicator.CONNECTED && autoConnectService.isCommunicatingWithDevice()) {
                handler.sendEmptyMessage(1);
                handler.removeCallbacks(connectCar);
            } else if(seconds > 15) {
                if(searchAttempts++ > 2) {
                    handler.sendEmptyMessage(2);
                    handler.removeCallbacks(connectCar);
                } else {
                    Log.i(TAG, "Search attempt: " + searchAttempts);
                    carSearchStartTime = System.currentTimeMillis();
                    autoConnectService.startBluetoothSearch();
                    handler.postDelayed(connectCar, 1000);
                }
            } else {
                handler.postDelayed(connectCar, 1000);
            }
        }
    };

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions,
                                            int[] grantResults) {
        if(requestCode == MainActivity.RC_LOCATION_PERM) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                autoConnectService.startBluetoothSearch();  // after permissions granted
            } else {
                Snackbar.make(findViewById(R.id.car_scan_view), R.string.location_request_rationale, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(CarScanActivity.this, MainActivity.LOC_PERMS, MainActivity.RC_LOCATION_PERM);
                            }
                        })
                        .show();
            }
        }
    }
}
