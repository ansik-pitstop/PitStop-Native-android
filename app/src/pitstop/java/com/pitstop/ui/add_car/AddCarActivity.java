package com.pitstop.ui.add_car;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.ui.add_car.ask_has_device.AskHasDeviceFragment;
import com.pitstop.ui.add_car.device_search.DeviceSearchFragment;
import com.pitstop.ui.add_car.vin_entry.VinEntryFragment;
import com.pitstop.ui.main_activity.MainActivity;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class AddCarActivity extends AppCompatActivity implements FragmentSwitcher{

    //Activity result variables
    public static final int ADD_CAR_SUCCESS_NO_DEALER = 51;
    public static final int ADD_CAR_SUCCESS_HAS_DEALER = 53;
    public static final int ADD_CAR_FAILED = 56;

    private AskHasDeviceFragment askHasDeviceFragment;
    private DeviceSearchFragment deviceSearchFragment;
    private VinEntryFragment vinEntryFragment;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {

        setContentView(R.layout.activity_add_car);

        askHasDeviceFragment = AskHasDeviceFragment.getInstance();
        deviceSearchFragment = DeviceSearchFragment.getInstance();
        vinEntryFragment = VinEntryFragment.getInstance();

        setViewAskHasDevice();

        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    public void setViewAskHasDevice() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, askHasDeviceFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void setViewDeviceSearch() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, deviceSearchFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void setViewVinEntry(String scannerId, String scannerName) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        vinEntryFragment.onGotDeviceInfo(scannerId, scannerName);
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, vinEntryFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void endAddCarSuccess(Car car, boolean hasDealership) {
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
        Intent data = new Intent();
        setResult(ADD_CAR_FAILED, data);
        finish();
    }

}
