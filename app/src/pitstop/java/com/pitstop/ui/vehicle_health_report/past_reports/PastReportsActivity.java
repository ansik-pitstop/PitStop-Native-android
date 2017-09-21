package com.pitstop.ui.vehicle_health_report.past_reports;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.pitstop.R;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportHolder;
import com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view.HealthReportFragment;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class PastReportsActivity extends AppCompatActivity implements PastReportsViewSwitcher
        , ReportHolder {

    private final String TAG = getClass().getSimpleName();

    private final Fragment pastReportsFragment = new PastReportsFragment();
    private final Fragment healthReportFragment = new HealthReportFragment();

    private VehicleHealthReport vehicleHealthReport;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_progress);
    }

    @Override
    public void setPastReportsView() {
        Log.d(TAG,"setPastReportView()");
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.left_in,R.animator.right_out);
        fragmentTransaction.replace(R.id.report_progress_fragment_holder,pastReportsFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void setReportView(VehicleHealthReport vehicleHealthReport) {
        Log.d(TAG,"setReportView()");
        this.vehicleHealthReport = vehicleHealthReport;
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.left_in,R.animator.right_out);
        fragmentTransaction.replace(R.id.report_progress_fragment_holder,healthReportFragment);
        fragmentTransaction.commit();
    }

    @Override
    public VehicleHealthReport getVehicleHealthReport() {
        Log.d(TAG,"getVehicleHealthReport()");
        return vehicleHealthReport;
    }
}
