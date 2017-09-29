package com.pitstop.ui.vehicle_health_report.show_report;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.pitstop.R;
import com.pitstop.models.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.ui.vehicle_health_report.health_report_progress.ReportHolder;
import com.pitstop.ui.vehicle_health_report.show_report.emissions_report.EmissionsReportFragment;
import com.pitstop.ui.vehicle_health_report.show_report.health_report.HealthReportFragment;

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        Log.d(TAG,"onCreate()");
        View rootView = getLayoutInflater().inflate(R.layout.activity_show_report
                , findViewById(android.R.id.content));
        setContentView(rootView);
        healthReportFragment = new HealthReportFragment();
        emissionsReportFragment = new EmissionsReportFragment();

        this.vehicleHealthReport = getIntent().getParcelableExtra(EXTRA_VHR);
        this.emissionsReport = getIntent().getParcelableExtra(EXTRA_ET);

        Log.d(TAG,"Retrieved vhr from parcel:" + vehicleHealthReport
                + ", retrieved et from parcel: "+emissionsReport);

        if (vehicleHealthReport != null){
            displayHealthReport();
        }
        if (emissionsReport != null){
            displayEmissionsReport();
        }

    }

    public void displayHealthReport(){
        Log.d(TAG,"displayHealthReport()");
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
}
