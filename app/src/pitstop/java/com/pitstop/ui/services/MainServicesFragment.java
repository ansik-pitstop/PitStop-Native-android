package com.pitstop.ui.services;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.ui.MainActivity;
import com.pitstop.ui.mainFragments.MainFragmentCallback;

public class MainServicesFragment extends Fragment implements MainFragmentCallback{

    //Fragments being navigated
    private UpcomingServicesFragment upcomingServicesFragment;
    private HistoryServiceFragment historyServicesFragment;
    private CurrentServicesFragment currentServicesFragment;

    private static Car dashboardCar;

    public static final int SUB_SERVICE_COUNT = 3;
    private int attachedSubServiceCounter = 0;

    private MainActivity mainActivity;
    private SubServiceViewPager mServicesPager;

    public static MainServicesFragment newInstance() {
        MainServicesFragment fragment = new MainServicesFragment();
        return fragment;
    }

    public static void setDashboardCar(Car c){
        dashboardCar = c;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mainActivity = (MainActivity)getActivity();
        mServicesPager = (SubServiceViewPager)getActivity().findViewById(R.id.services_viewpager);

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
        mServicesPager.setAdapter(new ServicesAdapter(getChildFragmentManager()));
    }

    //Called when the fragment is set to visible or invisible by ViewPager
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        //Notify child fragments that the tab has been re-opened
        if (isVisibleToUser && upcomingServicesFragment != null
                && historyServicesFragment != null && currentServicesFragment != null){

            upcomingServicesFragment.onMainServiceTabReopened();
            historyServicesFragment.onMainServiceTabReopened();
            currentServicesFragment.onMainServiceTabReopened();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //Allow MainActivity to notify this fragment
        MainActivity.servicesCallback = this;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootview = inflater.inflate(R.layout.activity_services,null);

        //Check if onDashboardCarUpdated() was called prior to MainServicesFragment view being created
        if (dashboardCar != null){
            onDashboardCarUpdated();
        }

        return rootview;
    }

    @Override
    public void onDashboardCarUpdated() {

        //Send car data to upcoming services fragment
        UpcomingServicesFragment.setDashboardCar(dashboardCar);
        if (upcomingServicesFragment != null){
            upcomingServicesFragment.onDashboardCarUpdated();
        }

        //Send car data to history services fragment
        HistoryServiceFragment.setDashboardCar(dashboardCar);
        if (historyServicesFragment != null){
            historyServicesFragment.onDashboardCarUpdated();
        }

        //Send car data to current services fragment
        CurrentServicesFragment.setDashboardCar(dashboardCar);
        if (currentServicesFragment != null){
            currentServicesFragment.onDashboardCarUpdated();
        }
    }

    @Override
    public void onAttachFragment(Fragment childFragment) {
        super.onAttachFragment(childFragment);

        Log.d("KAROL","attached fragment: "+childFragment.getClass().getSimpleName());
    }

    //Return data associated with fragment of the provided tab
    private class ServicesAdapter extends FragmentPagerAdapter {

        private final int FRAGMENT_UPCOMING = 0;
        private final int FRAGMENT_CURRENT = 1;
        private final int FRAGMENT_HISTORY = 2;
        private final int FRAGMENT_COUNT = 3;

        public ServicesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            //Return respective fragment and set the variable inside outer class for later callback reference
            switch (position){
                case FRAGMENT_UPCOMING:
                    upcomingServicesFragment = UpcomingServicesFragment.newInstance();
                    return upcomingServicesFragment;
                case FRAGMENT_CURRENT:
                    currentServicesFragment = CurrentServicesFragment.newInstance();
                    return  currentServicesFragment;
                case FRAGMENT_HISTORY:
                    historyServicesFragment = HistoryServiceFragment.newInstance();
                    return historyServicesFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return FRAGMENT_COUNT;
        }
    }

}
