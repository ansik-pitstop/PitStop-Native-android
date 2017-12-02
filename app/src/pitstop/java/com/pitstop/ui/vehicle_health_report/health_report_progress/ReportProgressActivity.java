package com.pitstop.ui.vehicle_health_report.health_report_progress;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;

import com.pitstop.R;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.vehicle_health_report.show_report.ShowReportActivity;
import com.pitstop.ui.vehicle_health_report.show_report.health_report.HealthReportFragment;
import com.pitstop.ui.vehicle_health_report.health_report_progress.report_in_progress_view.HealthReportProgressFragment;

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


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"serviceConnection.onServiceConnected()");
            setAutoConnectService(((BluetoothAutoConnectService.BluetoothBinder)service)
                    .getService());

            bluetoothConnectionObservable = ((BluetoothAutoConnectService.BluetoothBinder)service)
                    .getService();
            checkPermissions();
            healthReportProgressFragment.setBluetooth(bluetoothConnectionObservable);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"serviceConnection.onServiceDisconnected()");
            bluetoothConnectionObservable = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_progress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bindService(new Intent(getApplicationContext(), BluetoothAutoConnectService.class)
                , serviceConnection, Context.BIND_AUTO_CREATE);
        fragmentManager = getSupportFragmentManager();
        presenter = new ReportPresenter(this);
        healthReportProgressFragment = new HealthReportProgressFragment();
        healthReportProgressFragment.setCallback(this);
        healthReportFragment = new HealthReportFragment();

    }


    @Override
    public void onFoundDevices() {

    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume()");
        super.onResume();
        bindService(new Intent(getApplicationContext(), BluetoothAutoConnectService.class)
                , serviceConnection, Context.BIND_AUTO_CREATE);
        presenter.subscribe(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
        unbindService(serviceConnection);
        presenter.unsubscribe();
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
