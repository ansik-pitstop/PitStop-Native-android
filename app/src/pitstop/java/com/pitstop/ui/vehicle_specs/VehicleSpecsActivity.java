package com.pitstop.ui.vehicle_specs;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.pitstop.R;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsActivity extends AppCompatActivity {

    FragmentManager fragmentManager;
    VehicleSpecsFragment specsFragment;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vehicle_specs_activity);
        fragmentManager = getFragmentManager();
        specsFragment = new VehicleSpecsFragment();
        setSpecsFragment();

    }

    @Override
    protected void onResume() {
        super.onResume();
        //presenter.subscribe(this);
    }

    public void setSpecsFragment() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.vehicle_specs_fragment_holder,specsFragment);
        fragmentTransaction.commit();
    }



}
