package com.pitstop.ui.scan_car;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.EdgeDetail;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;
import com.pitstop.BuildConfig;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.issue_detail.view_fragments.IssuePagerAdapter;
import com.pitstop.ui.mainFragments.CarDataFragment;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.facebook.FacebookSdk.getApplicationContext;

public class ScanCarFragment extends CarDataFragment implements ScanCarContract.View{

    private static String TAG = ScanCarFragment.class.getSimpleName();
    public static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_SCAN);

    private MixpanelHelper mixpanelHelper;
    private ScanCarContract.Presenter presenter;

    @BindView(R.id.progress) View loadingView;
    @BindView(R.id.scan_details_cards) LinearLayout scanDetailsLayout;

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

    @BindView(R.id.car_view_layout) CardView carCard;
    @BindView(R.id.recalls_scan_details) CardView recallCard;
    @BindView(R.id.services_scan_details) CardView serviceCard;
    @BindView(R.id.engine_scan_details) CardView dtcCard;
    @BindView(R.id.update_mileage) Button updateMileageButton;

    private AlertDialog updateMileageDialog;
    private AlertDialog uploadHistoricalDialog;
    private AlertDialog connectTimeoutDialog;

    private int numberOfIssues = 0;

    private boolean isScanning = false;
    private boolean isLoading = false;
    private boolean gotEngineCodes = false;
    private boolean gotServices = false;
    private boolean gotRecalls = false;

    private IssuePagerAdapter pagerAdapter;
    private IBluetoothServiceActivity bluetoothServiceActivity;

    private UseCaseComponent useCaseComponent;

    public static ScanCarFragment newInstance(){
        return new ScanCarFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootview = inflater.inflate(R.layout.fragment_car_scan,null);
        ButterKnife.bind(this,rootview);
        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext().getApplicationContext()))
                .build();

        setStaticUI();
        presenter = new ScanCarPresenter(bluetoothServiceActivity, useCaseComponent);
        presenter.bind(this);
        presenter.update();

        return rootview;
    }

    @Override
    public void onStop() {
        super.onStop();
        presenter.unbind();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.bind(this);
    }

    private void setStaticUI(){

        if (!BuildConfig.DEBUG) {
            updateMileageButton.setVisibility(View.GONE);
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

        if (gotEngineCodes){
            displayEngineCodes();
        }
        if (gotRecalls){
            displayRecalls();
        }
        if (gotServices){
            displayServices();
        }
        if (gotEngineCodes || gotRecalls || gotServices){
            updateCarHealthMeter();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        bluetoothServiceActivity = (IBluetoothServiceActivity) context;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        //Record that view has been opened
        if (isVisibleToUser && getView() != null) {

            useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
                @Override
                public void onCarRetrieved(Car car) {
                    try {
                        JSONObject properties = new JSONObject();
                        properties.put("View", MixpanelHelper.SCAN_CAR_VIEW);
                        properties.put("Car Make", car.getMake());
                        properties.put("Car Model", car.getModel());
                        mixpanelHelper.trackCustom(MixpanelHelper.EVENT_VIEW_APPEARED, properties);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onNoCarSet() {

                }

                @Override
                public void onError() {

                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        if (presenter != null){
            presenter.unbind();
        }
        hideLoading(null);
        super.onDestroyView();
    }

    @Override
    public void updateUI() {
        presenter.update();
    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    @OnClick(R.id.dashboard_car_scan_btn)
    public void startCarScan(View view) {
        mixpanelHelper.trackButtonTapped("Start car scan", MixpanelHelper.SCAN_CAR_VIEW);
        startCarScan();
    }

    @OnClick(R.id.update_mileage)
    public void updateMileage(final View view) {
        if (presenter == null || isRemoving() || (updateMileageDialog != null && updateMileageDialog.isShowing())) {
            return;
        }

        final View dialogLayout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_input_mileage, null);
        final TextInputEditText input = (TextInputEditText) dialogLayout.findViewById(R.id.mileage_input);
        input.setText(String.valueOf((int) presenter.getLatestMileage()));

        if (updateMileageDialog == null) {
            updateMileageDialog = new AnimatedDialogBuilder(getActivity())
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
                            mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_CONFIRM_SCAN, MixpanelHelper.SCAN_CAR_VIEW);
                            // POST (entered mileage - the trip mileage) so (mileage in backend + trip mileage) = entered mileage
                            final double mileage = Double.parseDouble(input.getText().toString());
                            if (mileage > 20000000) {
                                Toast.makeText(getActivity(), "Please enter valid mileage", Toast.LENGTH_SHORT).show();
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
    public void onLoadedMileage(double mileage) {
        carMileage.setText(String.format("%.2f", mileage));
    }

    @Override
    public void onDeviceConnected() {
        if (connectTimeoutDialog != null && connectTimeoutDialog.isShowing()) connectTimeoutDialog.dismiss();
        if (isScanning) startCarScan();
    }

    @Override
    public void onConnectingTimeout() {
        if (isRemoving()) { // You don't want to add a dialog to a finished activity
            return;
        }
        if (connectTimeoutDialog == null) {
            connectTimeoutDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Device not connected")
                    .setMessage("Make sure your vehicle engine is on and " +
                            "OBD device is properly plugged in.\n\nTry again ?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_RETRY_SCAN, MixpanelHelper.SCAN_CAR_VIEW);
                            carScanButton.performClick();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_CANCEL_SCAN, MixpanelHelper.SCAN_CAR_VIEW);
                            dialog.cancel();
                        }
                    }).create();
        }

        connectTimeoutDialog.show();
    }

    @Override
    public void onInputtedMileageUpdated(double updatedMileage) {
        if (IBluetoothCommunicator.CONNECTED == (bluetoothServiceActivity).autoConnectService.getState()
                || bluetoothServiceActivity.autoConnectService.isCommunicatingWithDevice()) {
            carMileage.setText(String.format("%.2f", updatedMileage));
            Log.i(TAG, "Asking for RTC and Mileage, if connected to 215");
            getAutoConnectService().get215RtcAndMileage();
            startCarScan();
        } else {
            numberOfIssues = 0;
            presenter.connectToDevice();
        }
    }

    @Override
    public void onTripMileageUpdated(final double updatedMileage) {
        Log.d(TAG, "Updated Mileage: " + updatedMileage);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                carMileage.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.mileage_update));
                carMileage.setText(String.valueOf(updatedMileage));
            }
        });
    }

    private Set<CarIssue> recalls;

    @Override
    public void onRecallRetrieved(@Nullable Set<CarIssue> recalls) {
        Log.d(TAG, "onRecallRetrieved, num: " + (recalls == null? 0: recalls.size()));

        if (recalls == null){
            this.recalls = new HashSet<>();
        }
        else{
            this.recalls = recalls;
        }

        gotRecalls = true;
        numberOfIssues += recalls.size();
        updateCarHealthMeter();
        displayRecalls();
        checkScanProgress();
    }

    private void displayRecalls(){

        if (recalls == null) return;

        loadingRecalls.setVisibility(View.GONE);

        if (this.recalls.size() > 0) {
            recallsCountLayout.setVisibility(View.VISIBLE);
            recallsStateLayout.setVisibility(View.GONE);
            recallsCount.setText(String.valueOf(this.recalls.size()));
            recallsText.setText("Recalls");

            Drawable background = recallsCountLayout.getBackground();
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setColor(Color.rgb(203, 77, 69));
        } else {
            recallsStateLayout.setVisibility(View.VISIBLE);
            recallsCountLayout.setVisibility(View.GONE);
            recallsText.setText("No recalls");
        }
    }

    private Set<CarIssue> services;

    @Override
    public void onServicesRetrieved(@Nullable Set<CarIssue> services) {
        Log.d(TAG, "onServicesRetrieved, num: " + (services == null ? 0 : services.size()));
        gotServices = true;
        this.services = services == null ? new HashSet<CarIssue>() : services;
        numberOfIssues += (services == null ? 0 : services.size());
        updateCarHealthMeter();
        displayServices();
        checkScanProgress();
    }

    private void displayServices(){

        if (services == null) return;

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
    }

    private Set<String> dtcCodes = new HashSet<>();

    @Override
    public void onEngineCodesRetrieved(@Nullable Set<String> dtcCodes) {
        Log.d(TAG, "onEngineCodesRetrieved, num: " + (dtcCodes == null ? 0 : dtcCodes.size()));
        gotEngineCodes = true;
        this.dtcCodes = dtcCodes == null ? new HashSet<String>() : dtcCodes;
        numberOfIssues += dtcCodes == null ? 0 : dtcCodes.size();
        updateCarHealthMeter();
        displayEngineCodes();
        checkScanProgress();
    }

    private void displayEngineCodes(){

        if (dtcCodes == null) return;

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
    }

    /**
     * Show user that it takes a long time to retrieve all historical data
     */
    @Override
    public void onGetRealTimeDataTimeout() {
        if (isRemoving()) {
            return;
        }

        if (uploadHistoricalDialog == null) {
            uploadHistoricalDialog = new AnimatedDialogBuilder(getActivity())
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
        if (NetworkHelper.isConnected(getActivity())) {
            return true;
        } else {
            if (errorToShow != null) {
            } else {
                //hideLoading("No network connection! Please check your network connection and try again.");
            }
            return false;
        }
    }

    @Override
    public void onNetworkError(@NonNull String errorMessage) {
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    @Override
    public BluetoothAutoConnectService getAutoConnectService() {
        return bluetoothServiceActivity.autoConnectService;
    }

    @Override
    public IBluetoothServiceActivity getBluetoothActivity() {
        return bluetoothServiceActivity;
    }

    private void checkScanProgress() {
        isScanning = loadingEngineIssues.isShown() || loadingRecalls.isShown() || loadingServices.isShown();
        carScanButton.setEnabled(!isScanning);
//        if (!isScanning) {
//            mixpanelHelper.trackTimeEventEnd(MixpanelHelper.TIME_EVENT_SCAN_CAR);
//            // Finished car scan
//            try {
//                JSONObject properties = new JSONObject();
//                properties.put("View", MixpanelHelper.SCAN_CAR_VIEW);
//                properties.put("Mileage Updated To", dashboardCar.getTotalMileage());
//                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_SCAN_COMPLETE, properties);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }
    }

    @Override
    public void hideLoading(String string) {
        if (isLoading){

            //Hard coded for now since view will be changed entirely
            recallCard.setCardElevation(4);
            dtcCard.setCardElevation(4);
            serviceCard.setCardElevation(4);
            dtcCard.setCardElevation(4);
            carCard.setCardElevation(4);

            loadingView.setVisibility(View.GONE);
            isLoading = false;
        }
    }

    @Override
    public void showLoading(final String string) {
        if (!isLoading){

            //Hard coded for now since view will be changed entirely
            recallCard.setCardElevation(0);
            dtcCard.setCardElevation(0);
            serviceCard.setCardElevation(0);
            dtcCard.setCardElevation(0);
            carCard.setCardElevation(0);

            loadingView.bringToFront();
            loadingView.setVisibility(View.VISIBLE);
            isLoading = true;
        }
    }

    @Override
    public void setPresenter(ScanCarContract.Presenter presenter) {
        this.presenter = presenter;
        presenter.bind(this);
        presenter.bindBluetoothService();
    }

}
