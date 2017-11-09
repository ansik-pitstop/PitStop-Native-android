package com.pitstop.ui.vehicle_health_report.emissions_test_progress;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.pitstop.R;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view.InProgressFragment;
import com.pitstop.ui.vehicle_health_report.show_report.emissions_report.EmissionsReportFragment;

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

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"serviceConnection.onServiceConnected()");
            setAutoConnectService(((BluetoothAutoConnectService.BluetoothBinder)service)
                    .getService());

            bluetoothConnectionObservable = ((BluetoothAutoConnectService.BluetoothBinder)service)
                    .getService();
            checkPermissions();
            inProgressFragment.setBluetooth(bluetoothConnectionObservable);

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
        setContentView(R.layout.activity_emissions_progress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bindService(new Intent(getApplicationContext(), BluetoothAutoConnectService.class)
                , serviceConnection, Context.BIND_AUTO_CREATE);


        presenter = new EmissionsProgressPresenter(this);
        inProgressFragment = new InProgressFragment();
        emissionsReportFragment = new EmissionsReportFragment();
        inProgressFragment.setCallback(this);
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
