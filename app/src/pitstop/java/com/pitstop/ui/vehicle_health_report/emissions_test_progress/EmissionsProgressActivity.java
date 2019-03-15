package com.pitstop.ui.vehicle_health_report.emissions_test_progress;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.continental.rvd.mobile_sdk.AvailableSubscriptions;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothService;
import com.pitstop.bluetooth.BluetoothWriter;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view.InProgressFragment;
import com.pitstop.ui.vehicle_health_report.show_report.emissions_report.EmissionsReportFragment;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

/**
 * Created by Matt on 2017-08-14.
 */

public class EmissionsProgressActivity extends IBluetoothServiceActivity implements EmissionsProgressView,EmissionsProgressCallback {

    private final String TAG = getClass().getSimpleName();

    private EmissionsProgressPresenter presenter;
    private InProgressFragment inProgressFragment;
    private EmissionsReportFragment emissionsReportFragment;
    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private BluetoothService bluetoothService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emissions_progress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        presenter = new EmissionsProgressPresenter(this);
        inProgressFragment = new InProgressFragment();
        emissionsReportFragment = new EmissionsReportFragment();
        inProgressFragment.setCallback(this);

        ((GlobalApplication)getApplicationContext()).getServices()
                .filter(next -> next instanceof BluetoothService)
                .map(next -> (BluetoothService)next)
                .subscribe(next -> {
                    bluetoothService = next;
                    bluetoothConnectionObservable = next;
                    checkPermissions();
                    inProgressFragment.setBluetooth(bluetoothConnectionObservable);
                }, err ->{
                    err.printStackTrace();
                    Log.e(TAG,"error getting bluetooth service");
                });
    }

    @Override
    public void onGotAvailableSubscriptions(@NotNull AvailableSubscriptions subscriptions) {
    }

    @Override
    public void onMessageFromDevice(@NotNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFoundDevices() {

    }

    @Override
    protected void onResume() {
        Log.d(TAG,"onResume()");
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public BluetoothConnectionObservable getBluetoothConnectionObservable() {
        return bluetoothService;
    }

    @Override
    public BluetoothWriter getBluetoothWriter() {
        return bluetoothService;
    }

    @Override
    public void setColors() {
        Log.d(TAG,"setColors()");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getApplicationContext(),R.color.highlight)));
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this , R.color.highlight_dark));
        }
    }

    @Override
    public void setViewProgress() {
        Log.d(TAG,"setViewProgress()");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.emissions_progress_fragment_holder,inProgressFragment);
        transaction.commit();
    }


    @Override
    public void setViewReport(JSONObject emissionsResults) {
        Log.d(TAG,"setViewReport()");
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.emissions_progress_fragment_holder,emissionsReportFragment);
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG,"onOptionsItemSelected()");
        int id = item.getItemId();
        if(id == android.R.id.home){
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void end() {
        Log.d(TAG,"end()");
        finish();
    }
}
