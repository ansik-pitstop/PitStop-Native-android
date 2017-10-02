package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.florent37.viewanimator.ViewAnimator;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportHolder;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Matt on 2017-08-17.
 */

public class EmissionsReportFragment extends Fragment implements EmissionsReportView {

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.emissions_report_cell_one)
    RelativeLayout cellOne;
    @BindView(R.id.emissions_report_cell_one_details)
    RelativeLayout cellOneDetails;
    @BindView(R.id.cell_one_text)
    TextView cellOneText;

    @BindView(R.id.emissions_report_cell_two)
    RelativeLayout cellTwo;
    @BindView(R.id.emissions_report_cell_two_details)
    RelativeLayout cellTwoDetails;

    @BindView(R.id.emissions_report_cell_three)
    RelativeLayout cellThree;
    @BindView(R.id.emissions_report_cell_three_details)
    RelativeLayout cellThreeDetails;

    @BindView(R.id.emissions_report_cell_four)
    RelativeLayout cellFour;
    @BindView(R.id.emissions_report_cell_four_details)
    RelativeLayout cellFourDetails;

    @BindView(R.id.emissions_report_cell_five)
    RelativeLayout cellFive;
    @BindView(R.id.emissions_report_cell_five_details)
    RelativeLayout cellFiveDetails;

    @BindView(R.id.emissions_report_cell_six)
    RelativeLayout cellSix;
    @BindView(R.id.emissions_report_cell_six_details)
    RelativeLayout cellSixDetails;

    @BindView(R.id.egr)
    TextView egr;

    @BindView(R.id.evap)
    TextView evap;

    @BindView(R.id.misfire)
    TextView misfire;

    @BindView(R.id.catalyst)
    TextView catalyst;

    @BindView(R.id.o2Sensor)
    TextView o2sensor;

    @BindView(R.id.componetns)
    TextView components;

    @BindView (R.id.pass)
    TextView pass;

    @BindView (R.id.emissions_content)
    View emissionsContent;

    private boolean dropDownInProgress;
    private EmissionsReportPresenter presenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view = inflater.inflate(R.layout.fragment_emissions_report,container,false);
        ButterKnife.bind(this,view);
        dropDownInProgress = false;
        MixpanelHelper mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());
        presenter = new EmissionsReportPresenter(mixpanelHelper);

        cellOne.setOnClickListener(view1 -> presenter.onCellClicked(cellOneDetails));
        cellTwo.setOnClickListener(view12 -> presenter.onCellClicked(cellTwoDetails));
        cellThree.setOnClickListener(view13 -> presenter.onCellClicked(cellThreeDetails));
        cellFour.setOnClickListener(view14 -> presenter.onCellClicked(cellFourDetails));
        cellFive.setOnClickListener(view15 -> presenter.onCellClicked(cellFiveDetails));
        cellSix.setOnClickListener(view16 -> presenter.onCellClicked(cellSixDetails));

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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
        if (getActivity() != null && getActivity() instanceof ReportHolder){
            return ((ReportHolder)getActivity()).getEmissionsReport();
        }else{
            return null;
        }
    }

    @Override
    public void displayEmissionsReport(EmissionsReport emissionsReport) {
        emissionsContent.setVisibility(View.VISIBLE);

        egr.setText(emissionsReport.getEGRVVTSystem());
        evap.setText(emissionsReport.getFuelSystem());
        misfire.setText(emissionsReport.getMisfire());
        catalyst.setText(emissionsReport.getNMHCCatalyst());
        o2sensor.setText(emissionsReport.getNOxSCRMonitor());
        components.setText(emissionsReport.getComponents());
        pass.setText(emissionsReport.isPass() ? "Pass" : "Fail");
    }

    @Override
    public void displayEmissionsUnavailable() {
        emissionsContent.setVisibility(View.GONE);
    }
}
