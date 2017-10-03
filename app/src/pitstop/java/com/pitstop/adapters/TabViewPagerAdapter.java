package com.pitstop.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.pitstop.R;
import com.pitstop.ui.NotificationsFragment;
import com.pitstop.ui.dashboard.DashboardFragment;
import com.pitstop.ui.main_activity.TabFragmentManager;
import com.pitstop.ui.scan_car.ScanCarFragment;
import com.pitstop.ui.services.MainServicesFragment;

/**
 * Class responsible for providing fragments, and their associated data
 *  for each of the tabs.
 *
 * Created by Karol Zdebel on 5/4/2017.
 */

public class TabViewPagerAdapter extends FragmentStatePagerAdapter {
    private Context context;
    public TabViewPagerAdapter(Context ctx, FragmentManager fm) {
        super(fm);
    }
    public void setContext(Context ctx){
        this.context = ctx;
    }

    @Override
    public Fragment getItem(int position) {


        // getItem is called to instantiate the fragment for the given page.
        switch(position){
            case TabFragmentManager.TAB_DASHBOARD:
                return new DashboardFragment();

            case TabFragmentManager.TAB_SERVICES:
                return MainServicesFragment.newInstance();

            case TabFragmentManager.TAB_SCAN:
                return ScanCarFragment.newInstance();

            case TabFragmentManager.TAB_NOTIF:
                return NotificationsFragment.newInstance();
        }

        return null;
    }

    @Override
    public int getCount() {
        // Show 4 total pages.
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch(position){
            case TabFragmentManager.TAB_DASHBOARD:
                return context.getString(R.string.dashboard);
            case TabFragmentManager.TAB_NOTIF:
                return context.getString(R.string.notifications);
            case TabFragmentManager.TAB_SCAN:
                return context.getString(R.string.scan);
            case TabFragmentManager.TAB_SERVICES:
                return context.getString(R.string.services_nav_text);
        }
        return "";
    }

}
