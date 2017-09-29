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
import com.pitstop.ui.vehicle_health_report.show_report.emissions_report.EmissionsReportFragment;
import com.pitstop.ui.vehicle_health_report.show_report.health_report.HealthReportFragment;

/**
 * Created by Karol Zdebel on 9/29/2017.
 */

public class ShowReportActivity extends AppCompatActivity {

    public final static String EXTRA_VHR = "extra_vehicle_health_report";
    public final static String EXTRA_ET = "extra_emission_test";

    private HealthReportFragment healthReportFragment;
    private EmissionsReportFragment emissionsReportFragment;
    private final String TAG = getClass().getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        View rootView = getLayoutInflater().inflate(R.layout.activity_show_report
                , findViewById(android.R.id.content));
        setContentView(rootView);
        healthReportFragment = new HealthReportFragment();
        emissionsReportFragment = new EmissionsReportFragment();

        VehicleHealthReport vehicleHealthReport = getIntent().getParcelableExtra(EXTRA_VHR);
        EmissionsReport emissionsReport = getIntent().getParcelableExtra(EXTRA_ET);

        Log.d(TAG,"Retrievied vhr from parcel:" + vehicleHealthReport
                + ", retrieved et from parcel: "+emissionsReport);

        super.onCreate(savedInstanceState, persistentState);
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
}
