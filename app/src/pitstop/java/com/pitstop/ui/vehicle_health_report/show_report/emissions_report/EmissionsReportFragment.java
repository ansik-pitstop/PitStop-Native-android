package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.github.florent37.viewanimator.ViewAnimator;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.report.EmissionsReport;
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

    //Not ready
    @BindView(R.id.view_ready_steps)
    View readySteps;

    @BindView(R.id.sensor_content)
    RecyclerView sensorContent;

    @BindView (R.id.emissions_content)
    View emissionsContentHolder;

    @BindView (R.id.result_right_chevron)
    View resultRightChevron;

    @BindView (R.id.unavailable_emissions_content)
    View unavailableEmissionsContent;

    private SensorDataAdapter sensorDataAdapter;
    private EmissionsReportPresenter presenter;

    private int sensorContentHeight = -1;
    private int emissionsReadyStepsContentHeight = -1;
    private boolean dropDownInProgress;
    private boolean emissionsNotReadyStepsToggled = false;
    private boolean emissionsResultsToggled = true;

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
        emissionsResultsToggled = true;
        dropDownInProgress = false;

        return view;
    }

    @Override
    public void onResume() {
        Log.d(TAG,"onResume()");
        super.onResume();
        setViewHeightListeners();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.loadEmissionsReport();
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
        pass.setText(R.string.unavailable);
    }

    @Override
    public void displayEmissionsReport(EmissionsReport emissionsReport) {
        Log.d(TAG,"displayEmissionsReport() Emissions Report: "+emissionsReport);

        resultRightChevron.setRotation(90);
        pass.setText(emissionsReport.isPass() ? "Pass" : emissionsReport.getReason().isEmpty() ? "Fail" : emissionsReport.getReason());

        if (sensorDataAdapter == null){
            sensorDataAdapter = new SensorDataAdapter(emissionsReport.getSensors());
            sensorContent.setAdapter(sensorDataAdapter);
            sensorContent.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        sensorDataAdapter.notifyDataSetChanged();

    }

    @Override
    public void displayEmissionsNotReady() {
        Log.d(TAG,"displayEmissionsNotReady()");
        sensorContent.setVisibility(View.GONE);
        toggleEmissionsNotReadySteps();
        pass.setText("Not Ready");
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
    public void toggleEmissionsResults() {
        Log.d(TAG,"toggleEmissionsResults() toggled? "+emissionsResultsToggled);
        Log.d(TAG,"height: "+ sensorContentHeight);
        if (!emissionsResultsToggled) {
            ViewAnimator.animate(resultRightChevron)
                    .onStart(() -> {
                        emissionsContentHolder.setVisibility(View.VISIBLE);
                    }).onStop(() -> {
                if (getActivity() != null)
                    ((ShowReportActivity) getActivity()).scrollToBottom();
            }).rotation(0, 90)
                    .andAnimate(emissionsContentHolder)
                    .height(0, sensorContentHeight)
                    .duration(200)
                    .start();
        }else{
            ViewAnimator.animate(resultRightChevron)
                    .rotation(90, 0)
                    .andAnimate(emissionsContentHolder)
                    .height(sensorContentHeight, 0)
                    .duration(200)
                    .start();
        }
        emissionsResultsToggled = !emissionsResultsToggled;
    }

    private boolean heightsLoaded(){
        return sensorContentHeight != -1 && emissionsReadyStepsContentHeight != -1;
    }

    private void setViewHeightListeners(){
        sensorContentHeight = -1;
        emissionsReadyStepsContentHeight = -1;
        readySteps.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener(){
                    @Override
                    public void onGlobalLayout() {
                        Log.d(TAG,"readySteps.onGlobalLayout() height: "+readySteps.getHeight());
                        emissionsReadyStepsContentHeight = readySteps.getHeight();
                        readySteps.getViewTreeObserver().removeOnGlobalLayoutListener( this );
                        readySteps.setVisibility( View.GONE );
                    }
                }
        );
        sensorContent.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> {
                    Log.d(TAG,"sensorContent.onGlobalLayout() height: "+sensorContent.getHeight());
                    if (sensorContent.getHeight() > sensorContentHeight){
                        sensorContentHeight = sensorContent.getHeight();
                    }
                }
        );
    }
}
