package com.pitstop.ui.add_car;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
import com.pitstop.ui.custom_shops.CustomShopActivity;
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
    public static final int RC_SELECT_DEALERSHIP = 57;

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

            //Set this for parent class which references auto connect service, this should be restructed later
            setAutoConnectService(((BluetoothAutoConnectService.BluetoothBinder)service)
                    .getService());

            bluetoothConnectionObservable = ((BluetoothAutoConnectService.BluetoothBinder)service)
                    .getService();
            if (currentFragment == deviceSearchFragment)
                deviceSearchFragment.setBluetoothConnectionObservable(bluetoothConnectionObservable);

            checkPermissions();

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

        checkPermissions();

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
    public void setViewVinEntry(String scannerId, String scannerName, int mileage) {
        Log.d(TAG,"setViewVinEntry() scannerId: "+scannerId+", scannerName: "+scannerName);

        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_VIN_ENTRY_VIEW);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, vinEntryFragment);
        currentFragment = vinEntryFragment;
        fragmentTransaction.commit();
        vinEntryFragment.onGotDeviceInfo(scannerId, scannerName, mileage);
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
        if (car == null) return;

        if (bluetoothConnectionObservable != null){
            bluetoothConnectionObservable.requestPidInitialization();
        }

        currentFragment = null;
        Intent data = new Intent();
        data.putExtra(MainActivity.CAR_EXTRA, car);

        //Go back to previous activity
        if (hasDealership){
            setResult(ADD_CAR_SUCCESS_HAS_DEALER, data);
            finish();
        }
        //Begin dealership selection
        else{
            Intent intent = new Intent(this, CustomShopActivity.class);
            intent.putExtra(CustomShopActivity.CAR_EXTRA,car);
            intent.putExtra(CustomShopActivity.START_SOURCE_EXTRA,getClass().getName());
            startActivityForResult(intent,RC_SELECT_DEALERSHIP);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //Dealership has been selected, pass down the result
        if (requestCode == RC_SELECT_DEALERSHIP){
            setResult(resultCode,data);
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void endAddCarFailure() {
        Log.d(TAG,"endAddCarFailure()");

        Intent data = new Intent();
        setResult(ADD_CAR_FAILED, data);
        finish();
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
