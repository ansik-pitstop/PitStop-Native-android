package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.florent37.viewanimator.ViewAnimator;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.report.DieselEmissionsReport;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.PetrolEmissionsReport;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportHolder;
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

    @BindView (R.id.emissions_content)
    View emissionsContentHolder;

    @BindView (R.id.result_right_chevron)
    View resultRightChevron;

    private EmissionsReportPresenter presenter;

    private boolean dropDownInProgress;
    private boolean emissionsNotReadyStepsToggled = false;
    private boolean emissionsResultsToggled = false;
    private boolean emissionsUnavailableToggled = false;

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
        dropDownInProgress = false;
        MixpanelHelper mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());
        presenter = new EmissionsReportPresenter(mixpanelHelper);

        emissionsNotReadyStepsToggled = false;
        emissionsResultsToggled = false;
        emissionsUnavailableToggled = false;
        dropDownInProgress = false;

        //todo: show details later
//        cellOne.setOnClickListener(view1 -> presenter.onCellClicked(cellOneDetails));
//        cellTwo.setOnClickListener(view12 -> presenter.onCellClicked(cellTwoDetails));
//        cellThree.setOnClickListener(view13 -> presenter.onCellClicked(cellThreeDetails));
//        cellFour.setOnClickListener(view14 -> presenter.onCellClicked(cellFourDetails));
//        cellFive.setOnClickListener(view15 -> presenter.onCellClicked(cellFiveDetails));
//        cellSix.setOnClickListener(view16 -> presenter.onCellClicked(cellSixDetails));

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.loadEmissionsTest();
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
        emissionsContentHolder.setVisibility(View.GONE);
    }

    @Override
    public void displayEmissionsUnavailableDialog() {
        Log.d(TAG,"displayEmissionsUnavailableDialog()");
        new AlertDialog.Builder(getActivity()).setTitle("Emissions Unavailable")
                .setMessage("At the time of the scan this vehicle was ineligible for emissions" +
                        " testing through the use of the Pitstop device.")
                .setPositiveButton("Ok",null)
                .create()
                .show();
    }

    @Override
    public void displayDieselEmissionsReport(DieselEmissionsReport dieselEmissionsReport) {
        Log.d(TAG,"displayDieselEmissionsReport() dieselEmissionsReport: "+dieselEmissionsReport);
        //Holders
        emissionsContentHolder.setVisibility(View.VISIBLE);
        petrolEmissionsContent.setVisibility(View.GONE);
        dieselEmissionsContent.setVisibility(View.VISIBLE);

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
        toggleEmissionsResults(true);

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
        Log.d(TAG,"toggleEmissionsNotReadySteps()");
        if (!emissionsNotReadyStepsToggled)
            ViewAnimator.animate(resultRightChevron)
                    .rotation(0,90)
                    .andAnimate(readySteps)
                    .height(0,readySteps.getHeight())
                    .duration(200)
                    .onStart(() -> readySteps.setVisibility(View.VISIBLE))
                    .start();
        else
            ViewAnimator.animate(resultRightChevron)
                    .rotation(90,0)
                    .andAnimate(readySteps)
                    .height(readySteps.getHeight(),0)
                    .duration(200)
                    .start();

        emissionsNotReadyStepsToggled = !emissionsNotReadyStepsToggled;
    }

    @Override
    public void toggleEmissionsResults(boolean petrol) {
        Log.d(TAG,"toggleEmissionsResults()");

        if (!emissionsResultsToggled)
            ViewAnimator.animate(resultRightChevron)
                    .rotation(0,90)
                    .andAnimate(emissionsContentHolder)
                    .height(0,emissionsContentHolder.getHeight())
                    .duration(200)
                    .onStart(() -> {
                        if (petrol){
                            emissionsContentHolder.setVisibility(View.VISIBLE);
                            dieselEmissionsContent.setVisibility(View.GONE);
                            petrolEmissionsContent.setVisibility(View.VISIBLE);
                        }
                        else{
                            emissionsContentHolder.setVisibility(View.VISIBLE);
                            dieselEmissionsContent.setVisibility(View.VISIBLE);
                            petrolEmissionsContent.setVisibility(View.GONE);
                        }
                    })
                    .start();
        else
            ViewAnimator.animate(resultRightChevron)
                    .rotation(90,0)
                    .andAnimate(emissionsContentHolder)
                    .height(emissionsContentHolder.getHeight(),0)
                    .duration(200)
                    .start();

        emissionsResultsToggled = !emissionsResultsToggled;
    }

    @Override
    public void toggleEmissionsUnavailable() {
        Log.d(TAG,"toggleEmissionsUnavailable()");
//
//        if (!emissionsUnavailableToggled)
//            ViewAnimator.animate(resultRightChevron)
//                    .rotation(0,90)
//                    .andAnimate(readySteps)
//                    .height(0,readySteps.getHeight())
//                    .duration(200)
//                    .start();
//        else
//            ViewAnimator.animate(resultRightChevron)
//                    .rotation(90,0)
//                    .andAnimate(readySteps)
//                    .height(readySteps.getHeight(),0)
//                    .duration(200)
//                    .start();
        emissionsUnavailableToggled = !emissionsResultsToggled;

    }
}
