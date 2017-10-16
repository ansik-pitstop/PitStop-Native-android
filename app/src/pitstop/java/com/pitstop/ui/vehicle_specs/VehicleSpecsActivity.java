package com.pitstop.ui.vehicle_specs;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.pitstop.R;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.custom_shops.CustomShopActivity;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsActivity extends AppCompatActivity {
    public static final String TAG  = VehicleSpecsActivity.class.getSimpleName();

    private FragmentManager fragmentManager;
    private VehicleSpecsFragment specsFragment;
    private Bundle bundle = new Bundle();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_specs);
        bundle = getIntent().getExtras();
        fragmentManager = getFragmentManager();
        specsFragment = new VehicleSpecsFragment();
        specsFragment.setArguments(bundle);
        setSpecsFragment();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void setSpecsFragment() {
        Log.d(TAG , "setSpecsFragment()");
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.vehicle_specs_fragment_holder,specsFragment);
        fragmentTransaction.commit();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        onBackPressed();
        return true;
    }

}
