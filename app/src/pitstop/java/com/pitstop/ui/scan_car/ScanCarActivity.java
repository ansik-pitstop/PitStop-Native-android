package com.pitstop.ui.scan_car;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.EdgeDetail;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;
import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothServiceConnection;
import com.pitstop.application.GlobalApplication;
import com.pitstop.ui.BSAbstractedFragmentActivity;
import com.pitstop.ui.MainActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ScanCarActivity extends BSAbstractedFragmentActivity implements ScanCarContract.View {

    private static String TAG = ScanCarActivity.class.getSimpleName();

    private MixpanelHelper mixpanelHelper;
    private ScanCarContract.Presenter presenter;

    @BindView(R.id.loading_recalls) RelativeLayout loadingRecalls;
    @BindView(R.id.recalls_state_layout) RelativeLayout recallsStateLayout;
    @BindView(R.id.recalls_count_layout) RelativeLayout recallsCountLayout;
    @BindView(R.id.recalls_text) TextView recallsText;
    @BindView(R.id.recalls_count) TextView recallsCount;

    @BindView(R.id.loading_services) RelativeLayout loadingServices;
    @BindView(R.id.services_state_layout) RelativeLayout servicesStateLayout;
    @BindView(R.id.services_count_layout) RelativeLayout servicesCountLayout;
    @BindView(R.id.services_text) TextView servicesText;
    @BindView(R.id.services_count) TextView servicesCount;

    @BindView(R.id.loading_engine_issues) RelativeLayout loadingEngineIssues;
    @BindView(R.id.engine_issues_state_layout) RelativeLayout engineIssuesStateLayout;
    @BindView(R.id.engine_issues_count_layout) RelativeLayout engineIssuesCountLayout;
    @BindView(R.id.engine_issues_text) TextView engineIssuesText;
    @BindView(R.id.engine_issues_count) TextView engineIssuesCount;

    @BindView(R.id.car_mileage) TextView carMileage;
    @BindView(R.id.dashboard_car_scan_btn) Button carScanButton;
    @BindView(R.id.dynamicArcView) DecoView arcView;
    @BindView(R.id.textPercentage) TextView textPercent;
    private SeriesItem seriesItem;
    private int seriesIndex;

    private AlertDialog updateMileageDialog;
    private AlertDialog uploadHistoricalDialog;
    private AlertDialog connectTimeoutDialog;

    private int numberOfIssues = 0;
    private Car dashboardCar;
    private double baseMileage;
    private boolean updatedMileageOrDtcsFound = false;
    private ProgressDialog progressDialog;

    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_scan);
        ButterKnife.bind(this);
        setPresenter(new ScanCarPresenter(this, (GlobalApplication) getApplicationContext(), (Car) getIntent().getParcelableExtra(MainActivity.CAR_EXTRA)));
        dashboardCar = getIntent().getParcelableExtra(MainActivity.CAR_EXTRA);
        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        baseMileage = dashboardCar.getTotalMileage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initUi();
        updateCarHealthMeter();
        try {
            JSONObject properties = new JSONObject();
            properties.put("View", MixpanelHelper.SCAN_CAR_VIEW);
            properties.put("Car Make", dashboardCar.getMake());
            properties.put("Car Model", dashboardCar.getModel());
            mixpanelHelper.trackCustom(MixpanelHelper.EVENT_VIEW_APPEARED, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        presenter.finishScan();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onActivityFinish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        presenter.finishScan();
        Intent data = new Intent();
        data.putExtra(MainActivity.REFRESH_FROM_SERVER, updatedMileageOrDtcsFound);
        setResult(MainActivity.RESULT_OK, data);
        super.finish();
        overridePendingTransition(R.anim.activity_slide_right_in, R.anim.activity_slide_right_out);
    }

    /**
     * Invoked when the user tap on any area except for buttons,
     *
     * @param view
     */
    public void returnToMainActivity(View view) {
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothServiceConnection.RC_ENABLE_BT:
                try {
                    if (resultCode == RESULT_OK) {
                        mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_ALLOW_BLUETOOTH_ON, MixpanelHelper.SCAN_CAR_VIEW);
                        if (isScanning) carScanButton.performClick();
                    } else {
                        mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_DENY_BLUETOOTH_ON, MixpanelHelper.SCAN_CAR_VIEW);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }

    private void initUi() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        carMileage.setText(String.format("%.2f", presenter.getLatestMileage()));
        if (!BuildConfig.DEBUG) {
            findViewById(R.id.update_mileage).setVisibility(View.GONE);
        }

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
        if (isFinishing() || isDestroyed() || (updateMileageDialog != null && updateMileageDialog.isShowing())) {
            return;
        }

        final View dialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_input_mileage, null);
        final TextInputEditText input = (TextInputEditText) dialogLayout.findViewById(R.id.mileage_input);
        input.setText(String.valueOf((int) presenter.getLatestMileage()));

        if (updateMileageDialog == null) {
            updateMileageDialog = new AnimatedDialogBuilder(this)
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Update Mileage")
                    .setView(dialogLayout)
                    .setPositiveButton("Confirm", null)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();

            updateMileageDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface d) {
                    updateMileageDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_CONFIRM_SCAN, MixpanelHelper.SCAN_CAR_VIEW);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            // POST (entered mileage - the trip mileage) so (mileage in backend + trip mileage) = entered mileage
                            final double mileage = Double.parseDouble(input.getText().toString()) - (dashboardCar.getDisplayedMileage() - baseMileage);
                            if (mileage > 20000000) {
                                Toast.makeText(ScanCarActivity.this, "Please enter valid mileage", Toast.LENGTH_SHORT).show();
                            } else {
                                presenter.updateMileage(mileage);
                                d.dismiss();
                            }
                        }
                    });
                }
            });

        }
        updateMileageDialog.show();
    }

    /**
     * Invoked when the "Scan Car" button is tapped
     *
     * @param view the "Scan Car" button
     */
    public void startCarScan(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Start car scan", MixpanelHelper.SCAN_CAR_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, MainActivity.RC_ENABLE_BT);
            return;
        }

        updateMileage(null);
    }

    private void startCarScan() {
        Log.i(TAG, "Starting car scan");
        isScanning = true;

        mixpanelHelper.trackTimeEventStart(MixpanelHelper.TIME_EVENT_SCAN_CAR);

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

        numberOfIssues = 0; // clear previous result
        updateCarHealthMeter();

        presenter.getServicesAndRecalls();
        presenter.checkRealTime(); // for DTC
    }

    private void updateCarHealthMeter() {
        if (numberOfIssues >= 3) {
            arcView.addEvent(new DecoEvent.Builder(30).setIndex(seriesIndex).build());
        } else if (numberOfIssues < 3 && numberOfIssues > 0) {
            arcView.addEvent(new DecoEvent.Builder(65).setIndex(seriesIndex).build());
        } else {
            arcView.addEvent(new DecoEvent.Builder(100).setIndex(seriesIndex).build());
        }
    }

    @Override
    public void onBackPressed() {
        try {
            mixpanelHelper.trackButtonTapped(MixpanelHelper.BUTTON_BACK, MixpanelHelper.SCAN_CAR_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onBackPressed();
    }

    @Override
    public void onDeviceConnected() {
        if (connectTimeoutDialog != null && connectTimeoutDialog.isShowing()) connectTimeoutDialog.dismiss();
        hideLoading(null);
        if (isScanning) startCarScan();
    }

    @Override
    public void onConnectingTimeout() {
        if (isFinishing()) { // You don't want to add a dialog to a finished activity
            return;
        }
        hideLoading(null);
        if (connectTimeoutDialog == null) {
            connectTimeoutDialog = new AnimatedDialogBuilder(this)
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Device not connected")
                    .setMessage("Make sure your vehicle engine is on and " +
                            "OBD device is properly plugged in.\n\nTry again ?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_RETRY_SCAN, MixpanelHelper.SCAN_CAR_VIEW);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            carScanButton.performClick();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_CANCEL_SCAN, MixpanelHelper.SCAN_CAR_VIEW);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            dialog.cancel();
                        }
                    }).create();
        }

        connectTimeoutDialog.show();
    }

    @Override
    public void onInputtedMileageUpdated(double updatedMileage) {
        if (IBluetoothCommunicator.CONNECTED == autoConnectService.getState()
                || autoConnectService.isCommunicatingWithDevice()) {
            updatedMileageOrDtcsFound = true;
            carMileage.setText(String.format("%.2f", updatedMileage));
            startCarScan();
        } else {
            numberOfIssues = 0;
            presenter.connectToDevice();
        }
    }

    @Override
    public void onTripMileageUpdated(final double updatedMileage) {
        Log.d(TAG, "Updated Mileage: " + updatedMileage);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                carMileage.startAnimation(AnimationUtils.loadAnimation(ScanCarActivity.this, R.anim.mileage_update));
                carMileage.setText(String.valueOf(updatedMileage));
            }
        });
    }

    private Set<CarIssue> recalls;

    @Override
    public void onRecallRetrieved(@Nullable Set<CarIssue> recalls) {
        Log.d(TAG, "onRecallRetrieved, num: " + (recalls == null ? 0 : recalls.size()));
        this.recalls = recalls == null ? new HashSet<CarIssue>() : recalls;
        numberOfIssues += (recalls == null ? 0 : recalls.size());
        updateCarHealthMeter();

        loadingRecalls.setVisibility(View.GONE);

        if (this.recalls.size() > 0) {
            recallsCountLayout.setVisibility(View.VISIBLE);
            recallsCount.setText(String.valueOf(this.recalls.size()));
            recallsText.setText("Recalls");

            Drawable background = recallsCountLayout.getBackground();
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setColor(Color.rgb(203, 77, 69));
        } else {
            recallsStateLayout.setVisibility(View.VISIBLE);
            recallsText.setText("No recalls");
        }

        checkScanProgress();
    }

    private Set<CarIssue> services;

    @Override
    public void onServicesRetrieved(@Nullable Set<CarIssue> services) {
        Log.d(TAG, "onServicesRetrieved, num: " + (services == null ? 0 : services.size()));
        this.services = services == null ? new HashSet<CarIssue>() : services;
        numberOfIssues += (services == null ? 0 : services.size());
        updateCarHealthMeter();

        loadingServices.setVisibility(View.GONE);

        if (this.services.size() > 0) {
            servicesCountLayout.setVisibility(View.VISIBLE);
            servicesCount.setText(String.valueOf(this.services.size()));
            servicesText.setText("Services");

            Drawable background = servicesCountLayout.getBackground();
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setColor(Color.rgb(203, 77, 69));

        } else {
            servicesStateLayout.setVisibility(View.VISIBLE);
            servicesText.setText("No services due");
        }

        checkScanProgress();
    }

    private Set<String> dtcCodes = new HashSet<>();

    @Override
    public void onEngineCodesRetrieved(@Nullable Set<String> dtcCodes) {
        Log.d(TAG, "onEngineCodesRetrieved, num: " + (dtcCodes == null ? 0 : dtcCodes.size()));
        this.dtcCodes = dtcCodes == null ? new HashSet<String>() : dtcCodes;
        updatedMileageOrDtcsFound = true;
        numberOfIssues += dtcCodes == null ? 0 : dtcCodes.size();
        updateCarHealthMeter();

        if (numberOfIssues != 0) updatedMileageOrDtcsFound = true;

        loadingEngineIssues.setVisibility(View.GONE);
        if (this.dtcCodes.size() != 0) {
            engineIssuesStateLayout.setVisibility(View.GONE);
            engineIssuesCountLayout.setVisibility(View.VISIBLE);
            engineIssuesCount.setText(String.valueOf(this.dtcCodes.size()));
            engineIssuesText.setText("Engine issues");
        } else {
            engineIssuesStateLayout.setVisibility(View.VISIBLE);
            engineIssuesCountLayout.setVisibility(View.GONE);
            engineIssuesText.setText("No Engine Issues");
        }

        Drawable background = engineIssuesCountLayout.getBackground();
        GradientDrawable gradientDrawable = (GradientDrawable) background;
        gradientDrawable.setColor(Color.rgb(203, 77, 69));

        checkScanProgress();
    }

    /**
     * Show user that it takes a long time to retrieve all historical data
     */
    @Override
    public void onGetRealTimeDataTimeout() {
        if (isFinishing()) {
            return;
        }

        if (uploadHistoricalDialog == null) {
            uploadHistoricalDialog = new AnimatedDialogBuilder(this)
                    .setTitle("Uploading historical data")
                    .setMessage("Device still uploading previous data. It seems you haven't connected to the device in a" +
                            "while and the device is still uploading all of that data to the phone which could take a" +
                            "while. You can continue driving and the engine codes will eventually popup on your phone.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton("", null).create();
        }
        uploadHistoricalDialog.show();

        mixpanelHelper.trackAlertAppeared("Device still uploading previous data.", MixpanelHelper.SCAN_CAR_VIEW);
    }

    @Override
    public void onRealTimeDataRetrieved() {
        if (uploadHistoricalDialog != null && uploadHistoricalDialog.isShowing()) uploadHistoricalDialog.dismiss();
        if (isScanning) presenter.getEngineCodes();
    }

    @Override
    public boolean checkNetworkConnection(@Nullable String errorToShow) {
        if (NetworkHelper.isConnected(this)) {
            return true;
        } else {
            if (errorToShow != null) {
                hideLoading(errorToShow);
            } else {
                hideLoading("No network connection! Please check your network connection and try again.");
            }
            return false;
        }
    }

    @Override
    public void onNetworkError(@NonNull String errorMessage) {
        hideLoading(errorMessage);
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public BluetoothAutoConnectService getAutoConnectService() {
        return autoConnectService;
    }

    @Override
    public BSAbstractedFragmentActivity getActivity() {
        return this;
    }

    private void checkScanProgress() {
        isScanning = loadingEngineIssues.isShown() || loadingRecalls.isShown() || loadingServices.isShown();
        carScanButton.setEnabled(!isScanning);
        if (!isScanning) {
            mixpanelHelper.trackTimeEventEnd(MixpanelHelper.TIME_EVENT_SCAN_CAR);
            // Finished car scan
            try {
                JSONObject properties = new JSONObject();
                properties.put("View", MixpanelHelper.SCAN_CAR_VIEW);
                properties.put("Mileage Updated To", dashboardCar.getTotalMileage());
                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_SCAN_COMPLETE, properties);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void hideLoading(String string) {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (string != null) {
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showLoading(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(string);
                if (!progressDialog.isShowing()) {
                    progressDialog.show();
                }
            }
        });
    }

    @Override
    public void setPresenter(ScanCarContract.Presenter presenter) {
        this.presenter = presenter;
    }
}
