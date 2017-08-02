package com.pitstop.ui.add_car;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.ui.add_car.ask_has_device.AskHasDeviceFragment;
import com.pitstop.ui.add_car.device_search.DeviceSearchFragment;
import com.pitstop.ui.add_car.vin_entry.VinEntryFragment;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.MixpanelHelper;

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
    private Fragment currentFragment;
    private MixpanelHelper mixpanelHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {

        setContentView(R.layout.activity_add_car);

        mixpanelHelper = new MixpanelHelper((GlobalApplication)getApplicationContext());

        askHasDeviceFragment = AskHasDeviceFragment.getInstance();
        deviceSearchFragment = DeviceSearchFragment.getInstance();
        vinEntryFragment = VinEntryFragment.getInstance();

        setViewAskHasDevice();

        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    public void setViewAskHasDevice() {
        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_ASK_HAS_DEVICE_VIEW);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, askHasDeviceFragment);
        currentFragment = askHasDeviceFragment;
        fragmentTransaction.commit();
    }

    @Override
    public void setViewDeviceSearch() {
        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_SEARCH_DEVICE_VIEW);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, deviceSearchFragment);
        currentFragment = deviceSearchFragment;
        fragmentTransaction.commit();
    }

    @Override
    public void setViewVinEntry(String scannerId, String scannerName) {
        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_VIN_ENTRY_VIEW);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        vinEntryFragment.onGotDeviceInfo(scannerId, scannerName);
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, vinEntryFragment);
        currentFragment = vinEntryFragment;
        fragmentTransaction.commit();
    }

    @Override
    public void setViewVinEntry() {
        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_VIN_ENTRY_VIEW);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.activity_add_car_fragment_holder, vinEntryFragment);
        currentFragment = vinEntryFragment;
        fragmentTransaction.commit();
    }

    @Override
    public void endAddCarSuccess(Car car, boolean hasDealership) {
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
        Intent data = new Intent();
        setResult(ADD_CAR_FAILED, data);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (currentFragment == vinEntryFragment || currentFragment == deviceSearchFragment){
            setViewAskHasDevice();
        }
        else{
            super.onBackPressed();
        }
    }
}
