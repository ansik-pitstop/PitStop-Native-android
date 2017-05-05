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
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.models.Car;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainServicesFragment extends Fragment {

    @BindView(R.id.services_viewpager)
    ViewPager mServicesPager;

    private List<Car> allCars;
    private View rootview;
    private LocalCarAdapter localCarStorage;

    //Fragments being navigated
    private UpcomingServicesFragment upcomingServicesFragment;
    private HistoryServiceFragment historyServicesFragment;
    private CurrentServicesFragment currentServicesFragment;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Get local car storage and set current car
        localCarStorage = new LocalCarAdapter(getContext());
        allCars = localCarStorage.getAllCars();

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

    //Called whenever the fragment is set to visible or invisible by the ViewPager
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        //Update the dashboard car if one exists
        if (isVisibleToUser && hasCurrentCar()) {

            //Update car list in case it changed
            updateCarList();

            currentServicesFragment.onDashboardCarUpdated(getCurrentCar());
            historyServicesFragment.onDashboardCarUpdated(getCurrentCar());
            upcomingServicesFragment.onDashboardCarUpdated(getCurrentCar());
        }

        //Fragment set to invisible
        else {
        }
    }

    //Get the current/dashboard car
    private Car getCurrentCar(){

        for (Car c: allCars){
            if (c.isCurrentCar()){
                return c;
            }
        }
        return null;
    }

    private void updateCarList(){
        allCars = localCarStorage.getAllCars();
    }

    //Returns whether a current car exists
    private boolean hasCurrentCar(){
        for (Car c: allCars){
            if (c.isCurrentCar()){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set to false since its true by defult and we don't want functionality being triggered
        //unpredictably
        setUserVisibleHint(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        rootview = inflater.inflate(R.layout.activity_services,null);
        ButterKnife.bind(this,rootview);
        return rootview;
    }

    //Return data associated with fragment of the provided tab
    private class ServicesAdapter extends FragmentPagerAdapter{

        private final int FRAGMENT_UPCOMING = 0;
        private final int FRAGMENT_CURRENT = 1;
        private final int FRAGMENT_HISTORY = 2;
        private final int FRAGMENT_COUNT = 3;
        private Car currentCar;

        public ServicesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            //Set the current car once so that it doesn't keep getting pulled from disc
            // also so that all fragments must receive the same car
            if (currentCar == null){
                updateCarList();
                currentCar = getCurrentCar();
            }

            //Return respective fragment
            switch (position){
                case FRAGMENT_UPCOMING:
                    upcomingServicesFragment = UpcomingServicesFragment.newInstance(currentCar);
                    return upcomingServicesFragment;
                case FRAGMENT_CURRENT:
                    currentServicesFragment = CurrentServicesFragment.newInstance(currentCar);
                    return currentServicesFragment;
                case FRAGMENT_HISTORY:
                    historyServicesFragment = HistoryServiceFragment.newInstance(currentCar);
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
