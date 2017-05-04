package com.pitstop.ui.services;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.models.Car;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ServicesFragment extends Fragment {

    @BindView(R.id.services_viewpager)
    ViewPager mServicesPager;

    private Car dashboardCar;
    private View rootview;
    private UpcomingServicesFragment upcomingServicesFragment;
    private HistoryServiceFragment historyServicesFragment;
    private CurrentServicesFragment currentServicesFragment;


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Create tab layout
        TabLayout tabLayout = (TabLayout) getActivity().findViewById(R.id.tab_layout);
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
        mServicesPager.setAdapter(new ServicesAdapter(getFragmentManager()));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootview = inflater.inflate(R.layout.activity_services,null);
        ButterKnife.bind(this,rootview);
        return rootview;
    }

    //Update all fragments in the tabs, using the received car data
    public void onDashboardCarUpdated(Car car){
        if (currentServicesFragment != null){
            currentServicesFragment.onDashboardCarUpdated(car);
        }

        if (historyServicesFragment != null){
            historyServicesFragment.onDashboardCarUpdated(car);
        }
    }

    //Return data associated with fragment of the provided tab
    private class ServicesAdapter extends FragmentPagerAdapter{

        public ServicesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    upcomingServicesFragment = UpcomingServicesFragment.newInstance();

                    return upcomingServicesFragment;
                case 1:
                    currentServicesFragment = CurrentServicesFragment.newInstance();

                    //Provide car data to fragment if available
                    if (dashboardCar != null){
                        currentServicesFragment.onDashboardCarUpdated(dashboardCar);
                    }

                    return currentServicesFragment;
                case 2:
                    historyServicesFragment = HistoryServiceFragment.newInstance();

                    //Provide car data to fragment if available
                    if (dashboardCar != null){
                        historyServicesFragment.onDashboardCarUpdated(dashboardCar);
                    }

                    return historyServicesFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }

}
