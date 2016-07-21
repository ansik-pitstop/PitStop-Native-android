package com.pitstop.AddCarProcesses;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.pitstop.R;
import com.pitstop.adapters.AddCarViewPagerAdapter;
import com.pitstop.utils.AddCarViewPager;

/**
 * Created by David on 7/20/2016.
 */
public class AddCarActivity extends FragmentActivity {

    public enum fragment_type{
        CHOOSE_HAVE_DONGLE,
        LOAD_VIN,
        INPUT_SCAN_VIN,
        CONFIRM,
        CHOOSE_DEALERSHIP
    }

    AddCarViewPager mPager;
    private AddCarViewPagerAdapter mPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_add_car_fragmented);

        //setup view pager
        mPager = (AddCarViewPager) findViewById(R.id.add_car_view_pager);
        mPagerAdapter = new AddCarViewPagerAdapter(getSupportFragmentManager());
        mPagerAdapter.addFragment(new AddCar1Fragment(), "STEP 1/3");
        mPagerAdapter.addFragment(new AddCar1Fragment(), "STEP 2/3");
        mPager.setAdapter(mPagerAdapter);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    public void noDongleClicked(View view) {
    }
}
