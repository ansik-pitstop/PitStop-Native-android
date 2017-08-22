package com.pitstop.ui.vehicle_health_report.health_report_progress;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;


import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.pitstop.R;
import com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view.HealthReportFragment;
import com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view.ReportProgressFragment;

/**
 * Created by Matt on 2017-08-14.
 */

public class ReportActivity extends AppCompatActivity implements ReportView,ReportCallback {

    private ReportPresenter presenter;
    private ReportProgressFragment reportProgressFragment;
    private HealthReportFragment healthReportFragment;

    private FragmentManager fragmentManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_progress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragmentManager = getFragmentManager();
        presenter = new ReportPresenter(this);
        reportProgressFragment = new ReportProgressFragment();
        healthReportFragment = new HealthReportFragment();

    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void setReportProgressView() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.report_progress_fragment_holder,reportProgressFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void setReportView() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.report_progress_fragment_holder,healthReportFragment);
        fragmentTransaction.commit();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
