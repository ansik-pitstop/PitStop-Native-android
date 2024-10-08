package com.pitstop.ui.vehicle_health_report.start_report;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.application.GlobalVariables;
import com.pitstop.bluetooth.BluetoothService;
import com.pitstop.database.LocalCarStorage;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalUserStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Settings;
import com.pitstop.models.User;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.EmissionsProgressActivity;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportProgressActivity;
import com.pitstop.ui.vehicle_health_report.past_reports.PastReportsActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.wang.avi.AVLoadingIndicatorView;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

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

    @BindView(R.id.more_graphs_button)
    Button moreGraphsButton;

    @BindView(R.id.start_report_button)
    Button startReportButton;

    @BindView(R.id.start_report_animation)
    AVLoadingIndicatorView startAnimation;

    @BindView(R.id.clear_engine_code)
    Button clearEngineCodes;


    private boolean emissionsMode;

    private StartReportPresenter presenter;

    private Context context;
    private AlertDialog promptBluetoothSearchDialog;
    private AlertDialog promptSearchInProgressDialog;
    private AlertDialog promptOfflineDialog;
    private AlertDialog promptAddCar;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Map<String, LineGraphSeries<DataPoint>> lineGraphSeriesMap;
    private AlertDialog clearEngineCodesDailog;

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
        moreGraphsButton.setOnClickListener(view1 -> presenter.onGraphClicked());

        clearEngineCodes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                clearEngineCodesDailog = new AnimatedDialogBuilder(getActivity())
                        .setTitle("Warning")
                        .setMessage("Are you sure to clear the engine codes?")
                        .setPositiveButton(getString(R.string.yes_button_text), (dialog, which) -> {
                            Log.d(TAG,"clearEngineCodesDailog.positiveButtonClicked()");

                            ((GlobalApplication)context.getApplicationContext()).getServices()
                                    .filter(next -> next instanceof BluetoothService)
                                    .map(next -> (BluetoothService)next)
                                    .subscribe(next -> {
                                        next.clearDTCs();
                                    }, error -> {
                                        Log.e(TAG,"clear engine code button clicked:"+error);
                                        error.printStackTrace();
                                    });

                        })
                        .setNegativeButton(getString(R.string.no_button_text), null)
                        .setCancelable(false)
                        .create();
                clearEngineCodesDailog.show();
           }
        });


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
                    .setTitle(getString(R.string.title_activity_add_car))
                    .setMessage(getString(R.string.prompt_add_car))
                    .setPositiveButton(getString(R.string.yes_button_text), (dialog, which) -> {
                        Log.d(TAG,"promptAddCarClicked()");
                        presenter.onAddCarClicked();
                    })
                    .setNegativeButton(getString(R.string.no_button_text), null)
                    .setCancelable(false)
                    .create();
        }
        promptAddCar.show();
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
                    .setTitle(getText(R.string.offline_error_title))
                    .setMessage(getText(R.string.offline_error))
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

        LocalUserStorage localUserStorage = new LocalUserStorage(LocalDatabaseHelper.getInstance(context));
        User user = localUserStorage.getUser();
        if (user == null) {
            return;
        }
        Settings settings = user.getSettings();
        if (settings == null) {
            return;
        }
        int carId = settings.getCarId();
        LocalCarStorage localCarStorage = new LocalCarStorage(LocalDatabaseHelper.getInstance(context));
        Car car = localCarStorage.getCar(carId);
        if (car != null && car.getScannerId() != null) {
            if (car.getScannerId().contains("danlaw")) {
                vehicleHealthTitle.setText("");
                return;
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lineGraphSeriesMap = new HashMap<>();
        GraphView graph = getActivity().findViewById(R.id.graph);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().scrollToEnd();
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);
        presenter.subscribe(this);
        Disposable d = ((MainActivity)getActivity()).getBluetoothService()
                .take(1)
                .subscribe(next -> {
                    presenter.setBluetoothConnectionObservable(next);
                },err -> {
                        Log.d(TAG,"error = "+err);
                });
        compositeDisposable.add(d);
        presenter.onViewReadyForLoad(getMainCarId());
    }

    private Integer getMainCarId() {
        return GlobalVariables.Companion.getMainCarId(context);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");
        super.onDestroyView();
        compositeDisposable.clear();
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

    @Override
    public boolean checkPermissions(){
        if (getActivity() != null && getActivity() instanceof IBluetoothServiceActivity){
            return ((IBluetoothServiceActivity)getActivity()).checkPermissions();
        }else{
            return true;
        }
    }

    @Override
    public void startBluetoothService() {
        Log.d(TAG,"startBluetoothService()");
        if (getActivity() != null){
            GlobalApplication app = ((GlobalApplication)getActivity().getApplication());
            app.startBluetoothService();
        }
    }

    @Override
    public boolean isBluetoothServiceRunning() {
        Log.d(TAG,"isBluetoothServiceRunning()");
        if (getActivity() != null){
            GlobalApplication app = ((GlobalApplication)getActivity().getApplication());
            return app.isBluetoothServiceRunning();
        }else{
            return false;
        }
    }

    @Override
    public void displaySeriesData(String series, DataPoint dataPoint) {
        Log.d(TAG,"displaySeriesData() series: "+series+", coordinate:"+dataPoint);
        LineGraphSeries<DataPoint> lineGraphSeries = lineGraphSeriesMap.get(series);
        if (lineGraphSeries == null){
            lineGraphSeries = new LineGraphSeries<>();
            lineGraphSeriesMap.put(series, lineGraphSeries);
            GraphView graph = getActivity().findViewById(R.id.graph);
                graph.addSeries(lineGraphSeries);
        }

        lineGraphSeries.appendData(dataPoint,true,40);
    }

    @Override
    public void startGraphActivity() {
        Log.d(TAG,"startGraphActivity()");
        try{
            ((MainActivity)getActivity()).startGraphsActivity();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void displayBluetoothConnectionRequirePrompt(){
        Toast.makeText(getActivity(),"Bluetooth connection required",Toast.LENGTH_LONG).show();
    }

    @Override
    public void displayLiveDataNotSupportedPrompt(){
        Toast.makeText(getActivity(),"Live data not supported for this vehicle",Toast.LENGTH_LONG).show();
    }

    @Override
    public Observable<BluetoothConnectionObservable> getBluetoothConnectionObservable() {
        return ((MainActivity)getActivity()).getBluetoothService().map((next)-> next);
    }

    @Override
    public void setLiveDataButtonEnabled(boolean enabled){
        if (enabled)
            moreGraphsButton.setBackgroundColor(getResources().getColor(R.color.primary));
        else moreGraphsButton.setBackgroundColor(getResources().getColor(R.color.dark_grey));
    }

    @OnClick(R.id.show_reports_button)
    public void onShowReportsButtonClicked(){
        Log.d(TAG,"onShowReportsButtonClicked()");
        presenter.onShowReportsButtonClicked(emissionsMode);
    }
}
