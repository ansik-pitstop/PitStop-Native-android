package com.pitstop.adapters;

import android.content.Context;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.pitstop.R;
import com.pitstop.ui.main_activity.TabFragmentManager;
import com.pitstop.ui.services.MainServicesFragment;
import com.pitstop.ui.trip.TripsFragment;
import com.pitstop.ui.vehicle_health_report.start_report.StartReportFragment;
import com.pitstop.ui.vehicle_specs.VehicleSpecsFragment;

/**
 * Class responsible for providing fragments, and their associated data
 *  for each of the tabs.
 *
 * Created by Karol Zdebel on 5/4/2017.
 */

public class TabViewPagerAdapter extends FragmentStatePagerAdapter {

    private MainServicesFragment mainServicesFragment;
    private StartReportFragment startReportFragment;
    private VehicleSpecsFragment vehicleSpecsFragment;
    private TripsFragment tripsFragment;
    private Context context;

    public TabViewPagerAdapter(FragmentManager fm, MainServicesFragment mainServicesFragment
            , StartReportFragment startReportFragment, VehicleSpecsFragment vehicleSpecsFragment
            , TripsFragment tripsFragment
            , Context context) {
        super(fm);
        this.mainServicesFragment = mainServicesFragment;
        this.startReportFragment = startReportFragment;
        this.vehicleSpecsFragment = vehicleSpecsFragment;
        this.tripsFragment = tripsFragment;
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {

        // getItem is called to instantiate the fragment for the given page.
        switch(position){
            case TabFragmentManager.TAB_SERVICES:
                return mainServicesFragment;
            case TabFragmentManager.TAB_SCAN:
                return startReportFragment;

            case TabFragmentManager.TAB_VEHICLE_SPECS:
                return vehicleSpecsFragment;

            case TabFragmentManager.TAB_TRIPS_LIST:
                return tripsFragment;

        }
        return null;
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch(position){
            case TabFragmentManager.TAB_SCAN:
                return context.getString(R.string.scan);
            case TabFragmentManager.TAB_VEHICLE_SPECS:
                return "My Car";
            case TabFragmentManager.TAB_SERVICES:
                return context.getString(R.string.services_nav_text);
            case TabFragmentManager.TAB_TRIPS_LIST:
                return context.getString(R.string.my_trips);
        }
        return "";
    }

}
