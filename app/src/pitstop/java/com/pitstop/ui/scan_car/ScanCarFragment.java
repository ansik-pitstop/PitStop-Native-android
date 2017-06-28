package com.pitstop.ui.scan_car;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.EdgeDetail;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.observer.BluetoothObservable;
import com.pitstop.observer.BluetoothObserver;
import com.pitstop.ui.mainFragments.CarDataFragment;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.facebook.FacebookSdk.getApplicationContext;

public class ScanCarFragment extends CarDataFragment implements ScanCarContract.View {

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

    @BindView(R.id.dashboard_car_scan_btn) Button carScanButton;
    @BindView(R.id.dynamicArcView) DecoView arcView;
    @BindView(R.id.textPercentage) TextView textPercent;
    private SeriesItem seriesItem;
    private int seriesIndex;

    @BindView(R.id.car_view_layout) CardView carCard;
    @BindView(R.id.recalls_scan_details) CardView recallCard;
    @BindView(R.id.services_scan_details) CardView serviceCard;
    @BindView(R.id.engine_scan_details) CardView dtcCard;

    @BindColor(R.color.scan_element_background) int scanResultBackgroundColor;

    private AlertDialog uploadHistoricalDialog;
    private AlertDialog noDeviceFoundDialog;
    private AlertDialog scanInterruptedDialog;

    private int numberOfIssues = 0;

    private boolean isScanning = false;
    private boolean isLoading = false;
    private boolean gotEngineCodes = false;
    private boolean gotServices = false;
    private boolean gotRecalls = false;

    private BluetoothObservable<BluetoothObserver> bluetoothObservable;
    private UseCaseComponent useCaseComponent;

    private Set<CarIssue> recalls;
    private Set<String> dtcCodes = new HashSet<>();
    private Set<CarIssue> services;

    public static ScanCarFragment newInstance(){
        return new ScanCarFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView()");

        View rootview = inflater.inflate(R.layout.fragment_car_scan,null);
        ButterKnife.bind(this,rootview);
        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        bluetoothObservable = (BluetoothObservable<BluetoothObserver>)getActivity();

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext().getApplicationContext()))
                .build();

        TempNetworkComponent networkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(getApplicationContext()))
                .build();

        setStaticUI();
        loadPreviousState();
        presenter = new ScanCarPresenter(bluetoothObservable, useCaseComponent
                , networkComponent.networkHelper());
        presenter.bind(this);
        presenter.update();

        return rootview;
    }

    @Override
    public void onResume() {
        Log.d(TAG,"onResume()");

        super.onResume();

        if (presenter != null){
            presenter.bind(this);
        }
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
        Log.d(TAG,"onDestroyView()");

        super.onDestroyView();
        if (presenter != null){
            presenter.unbind();
        }
        hideLoading(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (presenter != null){
            presenter.unbind();
        }
        hideLoading(null);
    }

    @Override
    public void updateUI() {
        Log.d(TAG,"updateUI()");
        if (presenter != null){
            presenter.update();
        }
    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    @OnClick(R.id.dashboard_car_scan_btn)
    public void startCarScan(View view) {
        Log.d(TAG,"@OnClick startCarScan()");

        mixpanelHelper.trackButtonTapped("Start car scan", MixpanelHelper.SCAN_CAR_VIEW);
        presenter.startScan();
    }

    @Override
    public void onScanStarted(){
        Log.d(TAG,"onScanStarted()");

        mixpanelHelper.trackTimeEventStart(MixpanelHelper.TIME_EVENT_SCAN_CAR);
        isScanning = true;
        numberOfIssues = 0; // clear previous result

        displayCheckingForRecalls();
        displayCheckingForServices();
        displayCheckingForEngineIssues();
        updateCarHealthMeter();

    }

    @Override
    public void onScanFailed(){
        isScanning = false;
        noDeviceFoundDialog.show();
    }

    @Override
    public void resetUI() {
        Log.d(TAG,"resetUI()");

        gotEngineCodes = false;
        gotRecalls = false;
        gotServices = false;
        numberOfIssues = 0;

        resetRecalls();
        resetEngineIssues();
        resetServices();
        updateCarHealthMeter();
    }

    @Override
    public void onScanInterrupted(){
        Log.d(TAG,"onScanInterrupted()");
        isScanning = false;
        scanInterruptedDialog.show();
        resetUI();
    }

    @Override
    public void onConnectingTimeout() {
        if (isRemoving()) { // You don't want to add a dialog to a finished activity
            return;
        }

        noDeviceFoundDialog.show();
    }

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

    /**
     * Show user that it takes a long time to retrieve all historical data
     */
    @Override
    public void onGetRealTimeDataTimeout() {
        if (isRemoving()) {
            return;
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

    private void checkScanProgress() {
        carScanButton.setEnabled(!isScanning);
    }

    @Override
    public void hideLoading(String string) {
        Log.d(TAG,"hideLoading() called, isLoading? "+isLoading);

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
        Log.d(TAG,"showLoading() called, isLoading? "+isLoading);

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

    private void resetRecalls(){
        recallsCount.setText("--");
        Drawable background = recallsCountLayout.getBackground();
        GradientDrawable gradientDrawable = (GradientDrawable) background;
        gradientDrawable.setColor(scanResultBackgroundColor);
        recallsCountLayout.setVisibility(View.VISIBLE);
        recallsStateLayout.setVisibility(View.GONE);
        loadingRecalls.setVisibility(View.GONE);
    }

    private void resetServices(){
        servicesCount.setText("--");
        Drawable background = servicesCountLayout.getBackground();
        GradientDrawable gradientDrawable = (GradientDrawable) background;
        gradientDrawable.setColor(scanResultBackgroundColor);
        servicesCountLayout.setVisibility(View.VISIBLE);
        servicesStateLayout.setVisibility(View.GONE);
        loadingServices.setVisibility(View.GONE);
    }

    private void resetEngineIssues(){
        engineIssuesCount.setText("--");
        Drawable background = engineIssuesCountLayout.getBackground();
        GradientDrawable gradientDrawable = (GradientDrawable) background;
        gradientDrawable.setColor(scanResultBackgroundColor);
        engineIssuesCountLayout.setVisibility(View.VISIBLE);
        engineIssuesStateLayout.setVisibility(View.GONE);
        loadingEngineIssues.setVisibility(View.GONE);
    }

    private void setStaticUI(){

        Log.d(TAG, "setStaticUI()");

        if (scanInterruptedDialog == null){
            scanInterruptedDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Device Disconnected")
                    .setMessage("Your device disconnected during the scan. Please re-connect and try again.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_RETRY_SCAN, MixpanelHelper.SCAN_CAR_VIEW);
                        }
                    })
                    .create();
        }

        if (noDeviceFoundDialog == null) {
            noDeviceFoundDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Device not connected")
                    .setMessage("Make sure your vehicle engine is on and " +
                            "OBD device is properly plugged in.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_RETRY_SCAN, MixpanelHelper.SCAN_CAR_VIEW);
                        }
                    })
                    .create();
        }

        if (uploadHistoricalDialog == null){
            uploadHistoricalDialog = new AnimatedDialogBuilder(getActivity())
                    .setTitle(getResources().getString(R.string.scan_historical_title))
                    .setMessage(getResources().getString(R.string.scan_historical_message))
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setNegativeButton("", null).create();
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

    private void loadPreviousState(){

        Log.d(TAG,"loadPreviousState() gotEngineCodes?"
                +gotEngineCodes+" gotRecalls?"+gotRecalls+" gotServices?"+gotServices);

        if (gotEngineCodes){
            displayEngineCodes();
        }else{
            resetEngineIssues();
        }

        if (gotRecalls){
            displayRecalls();
        }else{
            resetRecalls();
        }

        if (gotServices){
            displayServices();
        }else{
            resetServices();
        }

        if (gotEngineCodes || gotRecalls || gotServices){
            updateCarHealthMeter();
        }
        else{
            arcView.executeReset();
        }
    }

    private void displayCheckingForRecalls(){
        recallsStateLayout.setVisibility(View.GONE);
        recallsCountLayout.setVisibility(View.GONE);
        loadingRecalls.setVisibility(View.VISIBLE);
        recallsText.setText("Checking for recalls");
    }

    private void displayCheckingForServices(){
        servicesStateLayout.setVisibility(View.GONE);
        servicesCountLayout.setVisibility(View.GONE);
        loadingServices.setVisibility(View.VISIBLE);
        servicesText.setText("Checking for services");
    }

    private void displayCheckingForEngineIssues(){
        engineIssuesStateLayout.setVisibility(View.GONE);
        engineIssuesCountLayout.setVisibility(View.GONE);
        loadingEngineIssues.setVisibility(View.VISIBLE);
        engineIssuesText.setText("Checking for engine issues");
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
            servicesCountLayout.setVisibility(View.GONE);
            servicesText.setText("No services due");
        }
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

    private void updateCarHealthMeter() {
        if (numberOfIssues >= 3) {
            arcView.addEvent(new DecoEvent.Builder(30).setIndex(seriesIndex).build());
        } else if (numberOfIssues < 3 && numberOfIssues > 0) {
            arcView.addEvent(new DecoEvent.Builder(65).setIndex(seriesIndex).build());
        } else {
            arcView.addEvent(new DecoEvent.Builder(100).setIndex(seriesIndex).build());
        }
    }
}
