package com.pitstop.ui.services;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.ui.main_activity.MainActivity;
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
    private TabLayout tabLayout;

    private boolean didLoadCustomDesign = false;

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
        tabLayout = (TabLayout) getActivity().findViewById(R.id.tab_layout);
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

        //Check whether dashboard car was set prior to tablayout being set up
        if (dashboardCar != null && !didLoadCustomDesign){
            loadDealershipCustomDesign();
            didLoadCustomDesign = true;
        }
        else{
            didLoadCustomDesign = false;
        }

        mServicesPager.setAdapter(new ServicesAdapter(getChildFragmentManager()));
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

    private void loadDealershipCustomDesign(){
        //Update tab design to the current dealerships custom design if applicable
        if (dashboardCar.getDealership() != null){
            if (BuildConfig.DEBUG && (dashboardCar.getDealership().getId() == 4
                    || dashboardCar.getDealership().getId() == 18)){

                bindMercedesDealerUI();
            }else if (!BuildConfig.DEBUG && dashboardCar.getDealership().getId() == 14) {
                bindMercedesDealerUI();
            }
            else{
                bindDefaultDealerUI();
            }
        }
    }

    private void bindDefaultDealerUI(){
        //Get the themes default primary color
        TypedValue defaultColor = new TypedValue();
        mainActivity.getTheme().resolveAttribute(android.R.attr.colorPrimary, defaultColor, true);

        //Set other changed UI elements back to original color
        tabLayout.setBackgroundColor(defaultColor.data);
    }

    private void bindMercedesDealerUI(){
        tabLayout.setBackgroundColor(Color.BLACK);
    }

    @Override
    public void onDashboardCarUpdated() {

        //Update design for custom dealers
        if (getView() != null){
            loadDealershipCustomDesign();
            didLoadCustomDesign = true;
        }
        else{
            didLoadCustomDesign = false;
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
