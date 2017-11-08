package com.pitstop.ui.vehicle_health_report.past_reports;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.pitstop.models.report.FullReport;
import com.pitstop.ui.vehicle_health_report.show_report.ShowReportActivity;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class PastReportsActivity extends AppCompatActivity implements PastReportsViewSwitcher{

    private final String TAG = getClass().getSimpleName();

    private final Fragment pastReportsFragment = new PastReportsFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setPastReportsView();
    }

    @Override
    public void setPastReportsView() {
        Log.d(TAG,"setPastReportView()");
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content,pastReportsFragment)
                .commit();
    }

    @Override
    public void setReportView(FullReport report) {
        Log.d(TAG,"setReportView() FullReport: "+report);
        Intent intent = new Intent(PastReportsActivity.this, ShowReportActivity.class);
        intent.putExtra(ShowReportActivity.EXTRA_VHR, report.getVehicleHealthReport());
        intent.putExtra(ShowReportActivity.EXTRA_ET, report.getEmissionsReport());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG,"onOptionsItemSelected()");
        if(item.getItemId() == android.R.id.home){
            onBackPressed();
            return true;
        }else{
            return super.onOptionsItemSelected(item);
        }
    }
}
