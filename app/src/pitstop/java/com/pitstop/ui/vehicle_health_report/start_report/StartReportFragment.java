package com.pitstop.ui.vehicle_health_report.start_report;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.EmissionsProgressActivity;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportProgressActivity;
import com.pitstop.ui.vehicle_health_report.past_reports.PastReportsActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.wang.avi.AVLoadingIndicatorView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Matt on 2017-08-11.
 */

public class StartReportFragment extends Fragment implements StartReportView {

    private final String TAG = StartReportFragment.class.getSimpleName();

//    @BindView(R.id.emissions_switch)
//    Switch modeSwitch;

//    @BindView(R.id.emissions_label)
//    TextView emissionsLabel;

//    @BindView(R.id.health_report_label)
//    TextView healthReportLabel;

    @BindView(R.id.title_vehicle_health)
    TextView vehicleHealthTitle;

    @BindView(R.id.show_reports_button)
    Button pastReportsButton;

    @BindView(R.id.start_report_button)
    Button startReportButton;

    @BindView(R.id.start_report_animation)
    AVLoadingIndicatorView startAnimation;

    private boolean emissionsMode;

    private StartReportPresenter presenter;

    private Context context;
    private AlertDialog promptBluetoothSearchDialog;
    private AlertDialog promptSearchInProgressDialog;
    private AlertDialog promptOfflineDialog;
    private AlertDialog promptAddCar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view = inflater.inflate(R.layout.fragment_start_report,container,false);
        context = getActivity().getApplicationContext();
        ButterKnife.bind(this,view);
        emissionsMode = false;
        MixpanelHelper mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());
        UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext()))
                .build();
        presenter = new StartReportPresenter(useCaseComponent, mixpanelHelper);

        startReportButton.setOnClickListener(view1 -> presenter
                .startReportButtonClicked(emissionsMode));
        //modeSwitch.setOnCheckedChangeListener((compoundButton, b) -> presenter.onSwitchClicked(b));
        return view;
    }

    public void setBluetoothConnectionObservable(
            BluetoothConnectionObservable bluetoothConnectionObservable){
        Log.d(TAG,"setBluetoothConnectionObservable()");
        if (presenter != null)
            presenter.setBluetoothConnectionObservable(bluetoothConnectionObservable);
    }

    @Override
    public void startEmissionsProgressActivity(){
        Log.d(TAG,"startEmissionsProgressActivity()");
        Intent intent = new Intent(getActivity(), EmissionsProgressActivity.class);
        startActivity(intent);
    }

    @Override
    public void startVehicleHealthReportProgressActivity(){
        Log.d(TAG,"startVehicleHealthReportProgressActivity()");
        Intent intent = new Intent(getActivity(), ReportProgressActivity.class);
        startActivity(intent);
    }

    @Override
    public void startPastReportsActivity() {
        Log.d(TAG,"startPastReportsActivity()");
        Intent intent = new Intent(getActivity(), PastReportsActivity.class);
        startActivity(intent);
    }

    @Override
    public void promptBluetoothSearch() {
        Log.d(TAG,"displayNoBluetoothConnection()");
        if (promptBluetoothSearchDialog == null) {
            promptBluetoothSearchDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(getString(R.string.no_device_connection))
                    .setMessage(getString(R.string.no_device_dialog_message))
                    .setPositiveButton(getString(R.string.yes_button_text), (dialog, which) -> {
                        Log.d(TAG,"promptBluetoothSearchDialog.positiveButtonClicked()");
                        presenter.onBluetoothSearchRequested();
                    })
                    .setNegativeButton(getString(R.string.no_button_text), null)
                    .setCancelable(false)
                    .create();
        }
        promptBluetoothSearchDialog.show();
    }

    @Override
    public void promptAddCar() {
        Log.d(TAG,"promptAddCar()");
        if (promptAddCar == null) {
            promptAddCar = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(getString(R.string.add_car_alert_title))
                    .setMessage(getString(R.string.prompt_add_car))
                    .setPositiveButton(getString(R.string.yes_button_text), (dialog, which) -> {
                        Log.d(TAG,"promptAddCarClicked()");
                        presenter.onAddCarClicked();
                    })
                    .setNegativeButton(getString(R.string.no_button_text), null)
                    .setCancelable(false)
                    .create();
        }
        promptBluetoothSearchDialog.show();
    }

    @Override
    public void startAddCar() {
        Log.d(TAG,"startAddCar()");
        if (getActivity() != null)
            ((MainActivity)getActivity()).openAddCarActivity();
    }

    @Override
    public void displaySearchInProgress() {
        Log.d(TAG,"displaySearchInProgress()");
        if (promptSearchInProgressDialog == null) {
            promptSearchInProgressDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(getString(R.string.bluetooth_search_in_progress))
                    .setMessage(getString(R.string.bluetooth_search_dialog_message))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok_button),null)
                    .create();
        }
        promptSearchInProgressDialog.show();
    }

    @Override
    public void displayOffline() {
        Log.d(TAG,"displayOffline()");
        if (promptOfflineDialog == null) {
            promptOfflineDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Couldn't Connect to Internet")
                    .setMessage("We couldn't establish a connection with our servers. " +
                            "Please make sure you're connected to the internet before starting")
                    .setCancelable(false)
                    .setPositiveButton("OK",null)
                    .create();
        }
        promptOfflineDialog.show();
    }

    @Override
    public void changeTitle(int stringId, boolean progress) {
        Log.d(TAG,"changeTitle() stringId: "+stringId+", string: "+getString(stringId));
        String title = String.format("%s%s",getText(stringId),progress? "..." : "");
        vehicleHealthTitle.setText(title);
    }

    @Override
    public BluetoothConnectionObservable getBluetoothConnectionObservable() {
        BluetoothConnectionObservable bluetoothConnectionObservable
                = ((MainActivity)getActivity()).getBluetoothConnectService();
        Log.d(TAG,"getBluetoothConnectionObservable() null ? "
                + (bluetoothConnectionObservable == null));
        return bluetoothConnectionObservable;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.onViewReadyForLoad();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void setModeEmissions() {
        Log.d(TAG,"setModeEmissions()");
        emissionsMode = true;
        //emissionsLabel.setTextColor(ContextCompat.getColor(context,R.color.highlight));
        //healthReportLabel.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
        vehicleHealthTitle.setTextColor(ContextCompat.getColor(context,R.color.highlight));
        pastReportsButton.setBackground(ContextCompat.getDrawable(context,R.drawable.color_button_rectangle_highlight));
        startReportButton.setBackground(ContextCompat.getDrawable(context,R.drawable.color_button_green_highlight));
        startAnimation.setIndicatorColor(ContextCompat.getColor(context,R.color.highlight));
        //modeSwitch.getThumbDrawable().setColorFilter(ContextCompat.getColor(context,R.color.highlight), PorterDuff.Mode.MULTIPLY);
    }


    @Override
    public void setModeHealthReport() {
        Log.d(TAG,"setModeHealthReport()");
        emissionsMode = false;
        //emissionsLabel.setTextColor(ContextCompat.getColor(context,R.color.dark_grey));
        //healthReportLabel.setTextColor(ContextCompat.getColor(context,R.color.primary));
        vehicleHealthTitle.setTextColor(ContextCompat.getColor(context,R.color.primary));
        pastReportsButton.setBackground(ContextCompat.getDrawable(context,R.drawable.color_button_rectangle_primary));
        startReportButton.setBackground(ContextCompat.getDrawable(context,R.drawable.color_button_primary));
        startAnimation.setIndicatorColor(ContextCompat.getColor(context,R.color.primary));
        //modeSwitch.getThumbDrawable().setColorFilter(ContextCompat.getColor(context,R.color.primary), PorterDuff.Mode.MULTIPLY);
    }

    @OnClick(R.id.show_reports_button)
    public void onShowReportsButtonClicked(){
        Log.d(TAG,"onShowReportsButtonClicked()");
        presenter.onShowReportsButtonClicked(emissionsMode);
    }
}
