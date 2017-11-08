package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.github.florent37.viewanimator.ViewAnimator;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.report.DieselEmissionsReport;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.PetrolEmissionsReport;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportHolder;
import com.pitstop.ui.vehicle_health_report.show_report.ShowReportActivity;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Matt on 2017-08-17.
 */

public class EmissionsReportFragment extends Fragment implements EmissionsReportView {

    private final String TAG = getClass().getSimpleName();

    //Shared
    @BindView (R.id.emission_result)
    protected TextView pass;

    @BindView(R.id.misfire)
    protected TextView misfire;

    @BindView(R.id.ignition)
    protected TextView ignition;

    @BindView(R.id.components)
    protected TextView components;

    @BindView(R.id.fuel_system)
    protected TextView fuelSystem;

    //Petrol
    @BindView(R.id.nmhc_catalyst)
    protected TextView NMHCCatalyst;

    @BindView(R.id.egr_vtt_system)
    protected TextView EGRVTTSystem;

    @BindView(R.id.nox_scr_monitor)
    protected TextView NOxSCRMonitor;

    @BindView(R.id.boost_pressure)
    protected TextView boostPressure;

    @BindView(R.id.exhaust_sensor)
    protected TextView exhaustSensor;

    @BindView(R.id.pm_filter_monitoring)
    protected TextView PMFilterMonitoring;

    //Diesel
    @BindView(R.id.heated_catalyst)
    protected TextView heatedCatalyst;

    @BindView(R.id.catalyst)
    protected TextView catalyst;

    @BindView(R.id.evap)
    protected TextView evap;

    @BindView(R.id.secondary_air_filter)
    protected TextView secondaryAirFilter;

    @BindView(R.id.ac_refrigerant)
    protected TextView ACRefrigerant;

    @BindView(R.id.o2_sensor)
    protected TextView O2Sensor;

    @BindView(R.id.o2_sensor_heater)
    protected TextView O2SensorHeater;

    @BindView(R.id.egr)
    protected TextView EGR;

    //Not ready
    @BindView(R.id.view_ready_steps)
    View readySteps;

    //Holders
    @BindView (R.id.petrol_emissions_content)
    View petrolEmissionsContent;

    @BindView (R.id.diesel_emissions_content)
    View dieselEmissionsContent;

    @BindView (R.id.emissions_shared_content)
    View sharedEmissionsContent;

    @BindView (R.id.emissions_content)
    View emissionsContentHolder;

    @BindView (R.id.result_right_chevron)
    View resultRightChevron;

    @BindView (R.id.unavailable_emissions_content)
    View unavailableEmissionsContent;

    private EmissionsReportPresenter presenter;

    private int emissionsSharedContentHeight = -1;
    private int emissionsPetrolContentHeight = -1;
    private int emissionsDieselContentHeight = -1;
    private int emissionsReadyStepsContentHeight = -1;
    private boolean dropDownInProgress;
    private boolean emissionsNotReadyStepsToggled = false;
    private boolean emissionsResultsToggled = false;

    @OnClick(R.id.emission_result_holder)
    public void onEmissionResultHolderClicked(){
        presenter.onEmissionResultHolderClicked();
        Log.d(TAG,"onEmissionResultHolderClicked()");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view = inflater.inflate(R.layout.fragment_emissions_report,container,false);
        ButterKnife.bind(this,view);
        MixpanelHelper mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());
        presenter = new EmissionsReportPresenter(mixpanelHelper);

        emissionsNotReadyStepsToggled = false;
        emissionsResultsToggled = false;
        dropDownInProgress = false;

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        setViewHeightListeners();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);

    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");
        presenter.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public void toggleCellDetails(View cell) {
        Log.d(TAG,"toggleCellDetails()");
        if(dropDownInProgress){return;}
        if(cell.getHeight() == 0){//open
            ViewAnimator.animate(cell)
                    .onStart(() -> dropDownInProgress = true)
                    .onStop(() -> dropDownInProgress = false)
                    .dp().height(0,100)
                    .duration(200)
                    .start();
        }else{//close
            ViewAnimator.animate(cell)
                    .onStart(() -> dropDownInProgress = true)
                    .onStop(() -> dropDownInProgress = false)
                    .dp().height(100,0)
                    .duration(200)
                    .start();
        }
    }

    @Override
    public EmissionsReport getEmissionsReport() {
        Log.d(TAG,"getEmissionsReport()");
        if (getActivity() != null && getActivity() instanceof ReportHolder){
            return ((ReportHolder)getActivity()).getEmissionsReport();
        }else{
            return null;
        }
    }

    @Override
    public void displayEmissionsUnavailable() {
        Log.d(TAG,"displayEmissionsUnavailable()");
        unavailableEmissionsContent.setVisibility(View.VISIBLE);
        resultRightChevron.setVisibility(View.INVISIBLE);
    }

    @Override
    public void displayDieselEmissionsReport(DieselEmissionsReport dieselEmissionsReport) {
        Log.d(TAG,"displayDieselEmissionsReport() dieselEmissionsReport: "+dieselEmissionsReport);
        displayEmissionsreport(dieselEmissionsReport);

        //Diesel
        heatedCatalyst.setText(dieselEmissionsReport.getHeatedCatalyst());
        catalyst.setText(dieselEmissionsReport.getCatalyst());
        evap.setText(dieselEmissionsReport.getEvap());
        secondaryAirFilter.setText(dieselEmissionsReport.getSecondaryAir());
        ACRefrigerant.setText(dieselEmissionsReport.getACRefrigirator());
        O2Sensor.setText(dieselEmissionsReport.getO2Sensor());
        O2SensorHeater.setText(dieselEmissionsReport.getO2SensorHeater());
        EGR.setText(dieselEmissionsReport.getEGR());
    }

    private void displayEmissionsreport(EmissionsReport emissionsReport){
        //Shared
        Log.d(TAG,"displayEmissionsReport() er: "+emissionsReport);
        misfire.setText(emissionsReport.getMisfire());
        ignition.setText(emissionsReport.getIgnition());
        components.setText(emissionsReport.getComponents());
        fuelSystem.setText(emissionsReport.getFuelSystem());
        pass.setText(emissionsReport.isPass() ? "Pass" : emissionsReport.getReason().isEmpty() ? "Fail" : emissionsReport.getReason());
    }

    @Override
    public void displayPetrolEmissionsReport(PetrolEmissionsReport petrolEmissionsReport) {
        Log.d(TAG,"displayPetrolEmissionsReport() petrolEmissionsReport: "+petrolEmissionsReport);
        displayEmissionsreport(petrolEmissionsReport);

        //Petrol
        NMHCCatalyst.setText(petrolEmissionsReport.getNMHCCatalyst());
        EGRVTTSystem.setText(petrolEmissionsReport.getEGRVTTSystem());
        NOxSCRMonitor.setText(petrolEmissionsReport.getNOxSCRMonitor());
        boostPressure.setText(petrolEmissionsReport.getBoostPressure());
        exhaustSensor.setText(petrolEmissionsReport.getExhaustSensor());
        PMFilterMonitoring.setText(petrolEmissionsReport.getPMFilterMonitoring());
    }

    @Override
    public void toggleEmissionsNotReadySteps() {
        Log.d(TAG,"toggleEmissionsNotReadySteps() height: "+emissionsReadyStepsContentHeight);
        if (!emissionsNotReadyStepsToggled)
            ViewAnimator.animate(resultRightChevron)
                    .rotation(0,90)
                    .andAnimate(readySteps)
                    .height(0,emissionsReadyStepsContentHeight)
                    .duration(200)
                    .onStart(() -> readySteps.setVisibility(View.VISIBLE))
                    .onStop(() -> {
                        if (getActivity() != null)
                            ((ShowReportActivity)getActivity()).scrollToBottom();
                    })
                    .start();
        else
            ViewAnimator.animate(resultRightChevron)
                    .rotation(90,0)
                    .andAnimate(readySteps)
                    .height(emissionsReadyStepsContentHeight,0)
                    .duration(200)
                    .start();

        emissionsNotReadyStepsToggled = !emissionsNotReadyStepsToggled;
    }

    @Override
    public void toggleEmissionsResults(boolean petrol) {
        Log.d(TAG,"toggleEmissionsResults() petrol? "+petrol);
        int height = petrol? emissionsPetrolContentHeight + emissionsSharedContentHeight
                : emissionsDieselContentHeight + emissionsSharedContentHeight;
        Log.d(TAG,"height: "+height);
        if (!emissionsResultsToggled)
            ViewAnimator.animate(resultRightChevron)
                    .onStart(() -> {
                        if (petrol){
                            emissionsContentHolder.setVisibility(View.VISIBLE);
                            sharedEmissionsContent.setVisibility(View.VISIBLE);
                            dieselEmissionsContent.setVisibility(View.GONE);
                            petrolEmissionsContent.setVisibility(View.VISIBLE);
                        }
                        else{
                            emissionsContentHolder.setVisibility(View.VISIBLE);
                            sharedEmissionsContent.setVisibility(View.VISIBLE);
                            dieselEmissionsContent.setVisibility(View.VISIBLE);
                            petrolEmissionsContent.setVisibility(View.GONE);
                        }
                    }).onStop(() -> {
                        if (getActivity() != null)
                            ((ShowReportActivity)getActivity()).scrollToBottom();
                    }).rotation(0,90)
                    .andAnimate(emissionsContentHolder)
                    .height(0,height)
                    .duration(200)
                    .start();
        else
            ViewAnimator.animate(resultRightChevron)
                    .rotation(90,0)
                    .andAnimate(emissionsContentHolder)
                    .height(height,0)
                    .duration(200)
                    .start();

        emissionsResultsToggled = !emissionsResultsToggled;
    }

    private boolean heightsLoaded(){
        return emissionsSharedContentHeight != -1 && emissionsPetrolContentHeight != -1
                && emissionsDieselContentHeight != -1 && emissionsReadyStepsContentHeight != -1;
    }

    private void setViewHeightListeners(){
        emissionsSharedContentHeight = -1;
        emissionsPetrolContentHeight = -1;
        emissionsDieselContentHeight = -1;
        emissionsReadyStepsContentHeight = -1;
        readySteps.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout() {
                        Log.d(TAG,"readySteps.onGlobalLayout() height: "+readySteps.getHeight());
                        emissionsReadyStepsContentHeight = readySteps.getHeight();
                        if (heightsLoaded())
                            presenter.onHeightsLoaded();
                        readySteps.getViewTreeObserver().removeOnGlobalLayoutListener( this );
                        readySteps.setVisibility( View.GONE );
                    }
                }
        );
        sharedEmissionsContent.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout() {
                        Log.d(TAG,"sharedEmissionsContent.onGlobalLayout() height: "+sharedEmissionsContent.getHeight());
                        emissionsSharedContentHeight = sharedEmissionsContent.getHeight();
                        if (heightsLoaded())
                            presenter.onHeightsLoaded();
                        sharedEmissionsContent.getViewTreeObserver().removeOnGlobalLayoutListener( this );
                        sharedEmissionsContent.setVisibility( View.GONE );
                    }
                }
        );
        petrolEmissionsContent.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout() {
                        Log.d(TAG,"petrolEmissionsContent.onGlobalLayout() height: "+petrolEmissionsContent.getHeight());
                        emissionsPetrolContentHeight = petrolEmissionsContent.getHeight();
                        if (heightsLoaded())
                            presenter.onHeightsLoaded();
                        petrolEmissionsContent.getViewTreeObserver().removeOnGlobalLayoutListener( this );
                        petrolEmissionsContent.setVisibility( View.GONE );
                    }
                }
        );
        dieselEmissionsContent.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout() {
                        Log.d(TAG,"dieselEmissionsContent.onGlobalLayout() height: "+dieselEmissionsContent.getHeight());
                        emissionsDieselContentHeight = dieselEmissionsContent.getHeight();
                        if (heightsLoaded())
                            presenter.onHeightsLoaded();
                        dieselEmissionsContent.getViewTreeObserver().removeOnGlobalLayoutListener( this );
                        dieselEmissionsContent.setVisibility( View.GONE );
                    }
                }
        );
    }
}
