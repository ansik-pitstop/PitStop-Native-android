package com.pitstop.ui.vehicle_health_report.health_report_progress;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothWriter;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view.HealthReportProgressFragment;
import com.pitstop.ui.vehicle_health_report.show_report.ShowReportActivity;
import com.pitstop.ui.vehicle_health_report.show_report.health_report.HealthReportFragment;

/**
 * Created by Matt on 2017-08-14.
 */

public class ReportProgressActivity extends IBluetoothServiceActivity
        implements ReportView,ReportCallback {

    private final String TAG = getClass().getSimpleName();

    private ReportPresenter presenter;
    private HealthReportProgressFragment healthReportProgressFragment;
    private HealthReportFragment healthReportFragment;

    private FragmentManager fragmentManager;
    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private BluetoothAutoConnectService bluetoothAutoConnectService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_progress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragmentManager = getSupportFragmentManager();
        presenter = new ReportPresenter(this);
        healthReportProgressFragment = new HealthReportProgressFragment();
        healthReportProgressFragment.setCallback(this);
        healthReportFragment = new HealthReportFragment();

        ((GlobalApplication)getApplicationContext()).getServices()
                .filter(next -> next instanceof BluetoothAutoConnectService)
                .map(next -> (BluetoothAutoConnectService)next)
                .subscribe(next -> {
                    bluetoothConnectionObservable = next;
                    bluetoothAutoConnectService = next;
                    healthReportProgressFragment.setBluetooth(bluetoothConnectionObservable);
                    checkPermissions();

                }, error -> {

                });

    }


    @Override
    public void onFoundDevices() {

    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume()");
        presenter.subscribe(this);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public BluetoothConnectionObservable getBluetoothConnectionObservable() {
        return bluetoothAutoConnectService;
    }

    @Override
    public BluetoothWriter getBluetoothWriter() {
        return bluetoothAutoConnectService;
    }

    @Override
    public void setReportProgressView() {
        Log.d(TAG,"setReportProgressView()");
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.report_progress_fragment_holder, healthReportProgressFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void setReportView(VehicleHealthReport vehicleHealthReport) {
        Log.d(TAG,"setReportView() report: "+vehicleHealthReport);
        Intent intent = new Intent(ReportProgressActivity.this, ShowReportActivity.class);
        intent.putExtra(ShowReportActivity.EXTRA_VHR, vehicleHealthReport);
        startActivity(intent);
    }

    @Override
    public void setReportView(VehicleHealthReport vehicleHealthReport, EmissionsReport emissionsReport) {
        Log.d(TAG,"setReportView() vhr: "+vehicleHealthReport+", et: "+emissionsReport);
        Intent intent = new Intent(ReportProgressActivity.this, ShowReportActivity.class);
        intent.putExtra(ShowReportActivity.EXTRA_VHR, vehicleHealthReport);
        intent.putExtra(ShowReportActivity.EXTRA_ET,emissionsReport);
        startActivity(intent);
    }

    @Override
    public void finishActivity() {
        finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG,"onOptionsItemSelected()");
        int id = item.getItemId();
        if(id == android.R.id.home){
            finish();
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }
}
