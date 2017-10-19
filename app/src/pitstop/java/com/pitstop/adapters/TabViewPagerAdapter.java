package com.pitstop.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.pitstop.ui.Notifications.NotificationFragment;
import com.pitstop.ui.dashboard.DashboardFragment;
import com.pitstop.ui.main_activity.TabFragmentManager;
import com.pitstop.ui.my_garage.MyGarageFragment;

import com.pitstop.ui.services.MainServicesFragment;
import com.pitstop.ui.vehicle_health_report.start_report.StartReportFragment;

/**
 * Class responsible for providing fragments, and their associated data
 *  for each of the tabs.
 *
 * Created by Karol Zdebel on 5/4/2017.
 */

public class TabViewPagerAdapter extends FragmentStatePagerAdapter {

    DashboardFragment dashboardFragment;
    MainServicesFragment mainServicesFragment;
    StartReportFragment startReportFragment;
    MyGarageFragment myGarageFragment;
    NotificationFragment notificationFragment;

    public TabViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {

        // getItem is called to instantiate the fragment for the given page.
        switch(position){
            case TabFragmentManager.TAB_DASHBOARD:
                if (dashboardFragment == null) {
                    dashboardFragment = new DashboardFragment();
                }
                return dashboardFragment;
            case TabFragmentManager.TAB_SERVICES:
                if (mainServicesFragment == null){
                    mainServicesFragment = new MainServicesFragment();
                }
                return mainServicesFragment;
            case TabFragmentManager.TAB_SCAN:
                if (startReportFragment == null){
                    startReportFragment = new StartReportFragment();
                }
                return startReportFragment;

            case TabFragmentManager.TAB_GARAGE:
                if (myGarageFragment == null){
                    myGarageFragment = MyGarageFragment.newInstance();
                }
                return myGarageFragment;

            case TabFragmentManager.TAB_NOTIF:
                if (notificationFragment == null){
                    notificationFragment = NotificationFragment.newInstance();
                }
                return notificationFragment;
        }
        return null;
    }

    @Override
    public int getCount() {
        // Show 4 total pages.
        return 5;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch(position){
            case TabFragmentManager.TAB_DASHBOARD:
                return "Dashboard";
            case TabFragmentManager.TAB_NOTIF:
                return "Notifications";
            case TabFragmentManager.TAB_SCAN:
                return "Scan";
            case TabFragmentManager.TAB_GARAGE:
                return "Garage";
            case TabFragmentManager.TAB_SERVICES:
                return "Services";
        }
        return "";
    }

}
