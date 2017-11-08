package com.pitstop.ui.vehicle_health_report.show_report;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;

import com.pitstop.R;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportHolder;
import com.pitstop.ui.vehicle_health_report.show_report.emissions_report.EmissionsReportFragment;
import com.pitstop.ui.vehicle_health_report.show_report.health_report.HealthReportFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Karol Zdebel on 9/29/2017.
 */

public class ShowReportActivity extends AppCompatActivity implements ReportHolder{

    public final static String EXTRA_VHR = "extra_vehicle_health_report";
    public final static String EXTRA_ET = "extra_emission_test";

    private VehicleHealthReport vehicleHealthReport;
    private EmissionsReport emissionsReport;
    private HealthReportFragment healthReportFragment;
    private EmissionsReportFragment emissionsReportFragment;
    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.scroll_view)
    protected ScrollView scrollView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate()");
        setContentView(R.layout.activity_show_report);
        ButterKnife.bind(this);
        healthReportFragment = new HealthReportFragment();
        emissionsReportFragment = new EmissionsReportFragment();

        this.vehicleHealthReport = getIntent().getParcelableExtra(EXTRA_VHR);
        this.emissionsReport = getIntent().getParcelableExtra(EXTRA_ET);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Log.d(TAG,"Retrieved vhr from parcel:" + vehicleHealthReport
                + "\n\n retrieved et from parcel: "+emissionsReport);

        if (vehicleHealthReport != null){
            displayHealthReport();
        }
        displayEmissionsReport();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            super.onBackPressed();
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }

    public void displayHealthReport(){
        Log.d(TAG,"displayReport()");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.holder_top, healthReportFragment)
                .commit();
    }

    public void displayEmissionsReport(){
        Log.d(TAG,"displayEmissionsReport()");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.holder_bottom, emissionsReportFragment)
                .commit();
    }

    @Override
    public VehicleHealthReport getVehicleHealthReport() {
        return vehicleHealthReport;
    }

    @Override
    public EmissionsReport getEmissionsReport() {
        return emissionsReport;
    }

    public void scrollToBottom(){
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}
