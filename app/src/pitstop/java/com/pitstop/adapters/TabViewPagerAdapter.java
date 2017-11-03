package com.pitstop.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.pitstop.R;
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
    Context context;

    public TabViewPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
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
                return context.getString(R.string.dashboard);
            case TabFragmentManager.TAB_NOTIF:
                return context.getString(R.string.notifications);
            case TabFragmentManager.TAB_SCAN:
                context.getString(R.string.scan);
            case TabFragmentManager.TAB_GARAGE:
                return "Garage";
            case TabFragmentManager.TAB_SERVICES:
                return context.getString(R.string.services_nav_text);
        }
        return "";
    }

}
