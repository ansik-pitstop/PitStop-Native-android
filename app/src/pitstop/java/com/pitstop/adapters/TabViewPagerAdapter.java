package com.pitstop.adapters;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.ui.MainActivity;
import com.pitstop.ui.mainFragments.MainDashboardFragment;
import com.pitstop.ui.services.ServicesFragment;

/**
 * Class responsible for providing fragments, and their associated data
 *  for each of the tabs.
 *
 * Created by Karol Zdebel on 5/4/2017.
 */

public class TabViewPagerAdapter extends FragmentPagerAdapter {

    private MainActivity mainActivity;

    private MainDashboardFragment mainDashboardFragment; //Main dashboard
    private ServicesFragment servicesFragment; //Services
    //Implement ScanFragment and Notifications fragments here

    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_sample_tab, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    public TabViewPagerAdapter(FragmentManager fm, MainActivity activity) {
        super(fm);
        mainActivity = activity;
    }

    @Override
    public Fragment getItem(int position) {

        // getItem is called to instantiate the fragment for the given page.
        switch(position){
            case MainActivity.TAB_DASHBOARD:
                return new MainDashboardFragment();

            case MainActivity.TAB_SERVICES:

                //Get the current car and use it as argument for spawning services activity
                Car currentCar = null;
                for (Car c: MainActivity.carList){
                    if (c.isCurrentCar()){
                        currentCar = c;
                        break;
                    }
                }

                ServicesFragment servicesFragment = new ServicesFragment();
                mainActivity.setServicesFragment(servicesFragment);
                return servicesFragment;
        }
        return PlaceholderFragment.newInstance(0);
    }

    @Override
    public int getCount() {
        // Show 4 total pages.
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch(position){
            case MainActivity.TAB_DASHBOARD:
                return "Dashboard";
            case MainActivity.TAB_NOTIF:
                return "Notifications";
            case MainActivity.TAB_SCAN:
                return "Scan";
            case MainActivity.TAB_SERVICES:
                return "Services";
        }
        return "";
    }

    public MainDashboardFragment getMainDashboardFragment() {
        return mainDashboardFragment;
    }

    public ServicesFragment getServicesFragment() {
        return servicesFragment;
    }

}
