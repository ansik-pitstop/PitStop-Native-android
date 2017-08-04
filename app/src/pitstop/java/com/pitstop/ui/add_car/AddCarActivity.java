package com.pitstop.ui.add_car;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.models.Car;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.add_car.ask_has_device.AskHasDeviceFragment;
import com.pitstop.ui.add_car.device_search.DeviceSearchFragment;
import com.pitstop.ui.add_car.vin_entry.VinEntryFragment;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class AddCarActivity extends IBluetoothServiceActivity implements FragmentSwitcher{

    private final String TAG = getClass().getSimpleName();

    //Activity result variables
    public static final int ADD_CAR_SUCCESS_NO_DEALER = 51;
    public static final int ADD_CAR_SUCCESS_HAS_DEALER = 53;
    public static final int ADD_CAR_FAILED = 56;
    public static final int RC_PENDING_ADD_CAR = 1043;

    private AskHasDeviceFragment askHasDeviceFragment;
    private DeviceSearchFragment deviceSearchFragment;
    private VinEntryFragment vinEntryFragment;
    private Fragment currentFragment;
    private MixpanelHelper mixpanelHelper;
    private BluetoothConnectionObservable bluetoothConnectionObservable;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"onServiceConnected() name: "+name.toString());
            bluetoothConnectionObservable = ((BluetoothAutoConnectService.BluetoothBinder)service)
                    .getService();
            if (currentFragment == deviceSearchFragment)
                deviceSearchFragment.setBluetoothConnectionObservable(bluetoothConnectionObservable);

            // Send request to user to turn on locations
            if (BluetoothAdapter.getDefaultAdapter() != null) {
                Log.d(TAG,"onServiceConnected() Bluetooth adapter is not null!");
                final String[] locationPermissions = getResources().getStringArray(R.array.permissions_location);
                for (String permission : locationPermissions) {
                    Log.d(TAG,"Checking permisssion: "+permission);
                    if (ContextCompat.checkSelfPermission(AddCarActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG,"Permission not granted! requesting permission!");
                        requestPermission(AddCarActivity.this, locationPermissions, RC_LOCATION_PERM,
                                true, getString(R.string.request_permission_location_message));
                        break;
                    }
                }
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"onServiceDisconnected() name: "+name.toString());
            bluetoothConnectionObservable = null;
            deviceSearchFragment.setBluetoothConnectionObservable(null);

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"onCreate()");

        setContentView(R.layout.activity_add_car);
        bindService(new Intent(getApplicationContext(), BluetoothAutoConnectService.class)
                , serviceConnection, Context.BIND_AUTO_CREATE);
        serviceBound = true;
        mixpanelHelper = new MixpanelHelper((GlobalApplication)getApplicationContext());

        askHasDeviceFragment = AskHasDeviceFragment.getInstance();
        deviceSearchFragment = DeviceSearchFragment.getInstance();
        vinEntryFragment = VinEntryFragment.getInstance();

        setViewAskHasDevice();
    }

    @Override
    protected void onResume() {

        //Service may have been unbound in onStop(), so bring it back here
        if (!serviceBound){
            bindService(new Intent(getApplicationContext(), BluetoothAutoConnectService.class)
                    , serviceConnection, Context.BIND_AUTO_CREATE);
            serviceBound = true;
        }
        super.onResume();
    }

    @Override
    public void setViewAskHasDevice() {
        Log.d(TAG,"setViewAskHasDevice()");

        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_ASK_HAS_DEVICE_VIEW);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, askHasDeviceFragment);
        currentFragment = askHasDeviceFragment;
        fragmentTransaction.commit();
    }

    @Override
    public void setViewDeviceSearch() {
        Log.d(TAG,"setViewDeviceSearch()");

        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_SEARCH_DEVICE_VIEW);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, deviceSearchFragment);
        currentFragment = deviceSearchFragment;
        fragmentTransaction.commit();
    }

    @Override
    public void setViewVinEntry(String scannerId, String scannerName) {
        Log.d(TAG,"setViewVinEntry() scannerId: "+scannerId+", scannerName: "+scannerName);

        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_VIN_ENTRY_VIEW);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        vinEntryFragment.onGotDeviceInfo(scannerId, scannerName);
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, vinEntryFragment);
        currentFragment = vinEntryFragment;
        fragmentTransaction.commit();
    }

    @Override
    public void setViewVinEntry() {
        Log.d(TAG,"setViewVinEntry()");

        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_VIN_ENTRY_VIEW);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, vinEntryFragment);
        currentFragment = vinEntryFragment;
        fragmentTransaction.commit();
    }

    @Override
    public void endAddCarSuccess(Car car, boolean hasDealership) {
        Log.d(TAG,"endAddCarSuccess() hasDealership? "+hasDealership+", createdCar: "+car);

        currentFragment = null;
        Intent data = new Intent();
        data.putExtra(MainActivity.CAR_EXTRA, car);
        if (hasDealership){
            setResult(ADD_CAR_SUCCESS_HAS_DEALER, data);
        }
        else{
            setResult(ADD_CAR_SUCCESS_NO_DEALER, data);
        }
        finish();
    }

    @Override
    public void endAddCarFailure() {
        Log.d(TAG,"endAddCarFailure()");

        Intent data = new Intent();
        setResult(ADD_CAR_FAILED, data);
        finish();
    }

    @Override
    public void beginPendingAddCarActivity(String vin, double mileage, String scannerId) {
        Intent intent = new Intent(this, PendingAddCarActivity.class);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_MILEAGE, mileage);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_SCANNER, scannerId);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_VIN, vin);
        startActivityForResult(intent, RC_PENDING_ADD_CAR);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG,"onBackPressed()");

        if (currentFragment == vinEntryFragment){
            vinEntryFragment.onBackPressed();
        }
        //Special case that requires extra layer of logic
        else if (currentFragment == deviceSearchFragment){
            deviceSearchFragment.onBackPressed();
        }
        else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        if (serviceBound){
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onStop();
    }

    public BluetoothConnectionObservable getBluetoothConnectionObservable(){
        return bluetoothConnectionObservable;
    }
}
