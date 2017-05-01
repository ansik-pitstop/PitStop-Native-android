package com.pitstop.ui.services;

import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.pitstop.R;
import com.pitstop.models.Car;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ServicesActivity extends AppCompatActivity {

    @BindView(R.id.services_viewpager)
    ViewPager mServicesPager;

    Car currentCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);
        ButterKnife.bind(this);
        currentCar = getIntent().getParcelableExtra("dashboardCar");
        final TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("Upcoming"));
        tabLayout.addTab(tabLayout.newTab().setText("Current"));
        tabLayout.addTab(tabLayout.newTab().setText("History"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mServicesPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        mServicesPager.setAdapter(new ServicesAdapter(getSupportFragmentManager()));

    }

    private class ServicesAdapter extends FragmentPagerAdapter{

        public ServicesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0: return UpcomingServicesFragment.newInstance(currentCar);
                case 1: return CurrentServicesFragment.newInstance(currentCar);
                case 2: return HistoryServiceFragment.newInstance(currentCar);
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

}
