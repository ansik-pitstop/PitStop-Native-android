package com.pitstop.ui.main_activity;

import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.pitstop.R;
import com.pitstop.adapters.TabViewPagerAdapter;
import com.pitstop.ui.Notifications.NotificationFragment;
import com.pitstop.ui.services.MainServicesFragment;
import com.pitstop.ui.services.MainServicesView;
import com.pitstop.ui.vehicle_health_report.start_report.StartReportFragment;
import com.pitstop.ui.vehicle_specs.VehicleSpecsFragment;
import com.pitstop.utils.MixpanelHelper;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.BottomBarTab;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Karol Zdebel on 6/23/2017.
 */

public class TabFragmentManager implements BadgeDisplayer{

    private final String TAG = getClass().getSimpleName();

    public static final int TAB_SERVICES = 2;
    public static final int TAB_SCAN = 1;
    public static final int TAB_VEHICLE_SPECS = 0;
    public static final int TAB_NOTIF = 3;

    public String[] TAB_NAMES;

    @BindView(R.id.main_container)
    ViewPager mViewPager;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.bottomBar)
    BottomBar bottomBar;

    private TabViewPagerAdapter tabViewPagerAdapter;
    private FragmentActivity mActivity;
    private MixpanelHelper mMixpanelHelper;

    private MainServicesFragment mainServicesFragment;
    private StartReportFragment startReportFragment;
    private VehicleSpecsFragment vehicleSpecsFragment;
    private NotificationFragment notificationFragment;

    public TabFragmentManager(FragmentActivity activity, MainServicesFragment mainServicesFragment
            , StartReportFragment startReportFragment, VehicleSpecsFragment vehicleSpecsFragment
            , NotificationFragment notificationFragment, MixpanelHelper mixpanelHelper) {

        mActivity = activity;
        this.mainServicesFragment = mainServicesFragment;
        this.startReportFragment = startReportFragment;
        this.vehicleSpecsFragment = vehicleSpecsFragment;
        this.notificationFragment = notificationFragment;
        mMixpanelHelper = mixpanelHelper;
        TAB_NAMES = new String[]{
                mActivity.getApplicationContext().getString(R.string.my_garage),
                mActivity.getApplicationContext().getString(R.string.scan),
                mActivity.getApplicationContext().getString(R.string.services_nav_text),
                mActivity.getApplicationContext().getString(R.string.notifications)
        };
    }

    public String getCurrentTabTitle(){
        return tabViewPagerAdapter.getPageTitle(mViewPager.getCurrentItem()).toString();
    }

    public void createTabs(){
        ButterKnife.bind(this,mActivity);
        tabViewPagerAdapter
                = new TabViewPagerAdapter(mActivity.getSupportFragmentManager(), mainServicesFragment
                , startReportFragment, vehicleSpecsFragment, notificationFragment, mActivity);

        mViewPager.setAdapter(tabViewPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);

        bottomBar.setOnTabSelectListener(tabId -> {
            switch(tabId){
                case R.id.tab_services:
                    mViewPager.setCurrentItem(TAB_SERVICES);
                    break;
                case R.id.tab_garage:
                    mViewPager.setCurrentItem(TAB_VEHICLE_SPECS);
                    break;
                case R.id.tab_scan:
                    mViewPager.setCurrentItem(TAB_SCAN);
                    break;
                case R.id.tab_notifications:
                    mViewPager.setCurrentItem(TAB_NOTIF);
                    break;
            }
        });

        setupSwitchActions();
        setupActionBar();
        mToolbar.setTitle(TAB_NAMES[0]);

    }

    private void setupSwitchActions(){
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch(position){
                    case TAB_SERVICES:
                        mMixpanelHelper.trackSwitchedToTab("Services");
                        break;
                    case TAB_SCAN:
                        mMixpanelHelper.trackSwitchedToTab("Health");
                        break;
                    case TAB_VEHICLE_SPECS:
                        mMixpanelHelper.trackSwitchedToTab("Vehicle specs");
                        break;
                    case TAB_NOTIF:
                        mMixpanelHelper.trackSwitchedToTab("Notifications");
                        break;
                }

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setupActionBar(){

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //Change actionbar title
                mToolbar.setTitle(TAB_NAMES[position]);
                switch(position){
                    case TAB_SERVICES:
                        bottomBar.selectTabWithId(R.id.tab_services);
                        break;
                    case TAB_SCAN:
                        bottomBar.selectTabWithId(R.id.tab_scan);
                        break;
                    case TAB_VEHICLE_SPECS:
                        bottomBar.selectTabWithId(R.id.tab_garage);
                        break;
                    case TAB_NOTIF:
                        bottomBar.selectTabWithId(R.id.tab_notifications);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public void openServices() {
        mViewPager.setCurrentItem(TAB_SERVICES);
        setCurrentServices();
    }
    public void setCurrentServices(){
        ((MainServicesFragment) tabViewPagerAdapter.getItem(TAB_SERVICES))
                .selectTab(MainServicesView.ServiceTab.CURRENT);
    }

    public void openScanTab() {
        mViewPager.setCurrentItem(TAB_SCAN);
    }

    @Override
    public void displayServicesBadgeCount(int count) {
        Log.d(TAG,"displayServicesBadgeCount() count: "+count);
        if (bottomBar != null){
            BottomBarTab tab = bottomBar.getTabWithId(R.id.tab_services);
            if (count == 0) tab.removeBadge();
            else bottomBar.getTabWithId(R.id.tab_services).setBadgeCount(count);
        }
    }

    @Override
    public void displayNotificationsBadgeCount(int count) {
        Log.d(TAG,"displayNotificationsBadgeCount() count: "+count);
        if (bottomBar != null){
            BottomBarTab tab = bottomBar.getTabWithId(R.id.tab_notifications);
            if (count == 0) tab.removeBadge();
            else bottomBar.getTabWithId(R.id.tab_notifications).setBadgeCount(count);
        }
    }
}
