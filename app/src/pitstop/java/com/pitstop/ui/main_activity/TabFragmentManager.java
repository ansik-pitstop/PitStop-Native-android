package com.pitstop.ui.main_activity;

import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;

import com.pitstop.R;
import com.pitstop.adapters.TabViewPagerAdapter;
import com.pitstop.ui.services.MainServicesFragment;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Karol Zdebel on 6/23/2017.
 */

public class TabFragmentManager {

    public static final int TAB_DASHBOARD = 0;
    public static final int TAB_SERVICES = 1;
    public static final int TAB_SCAN = 2;
    public static final int TAB_GARAGE = 3;
    public static final int TAB_NOTIF = 4;

    public static final String[] TAB_NAMES = {"Dashboard","Services","Vehicle Health Report","Garage", "Notifications"};

    @BindView(R.id.main_container)
    ViewPager mViewPager;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

    @BindView(R.id.appbar)
    BottomNavigationView bottomNavigationView;

    private TabViewPagerAdapter tabViewPagerAdapter;
    private FragmentActivity mActivity;
    private MixpanelHelper mMixpanelHelper;

    public TabFragmentManager(FragmentActivity activity, MixpanelHelper mixpanelHelper) {

        mActivity = activity;
        mMixpanelHelper = mixpanelHelper;
    }



    public void createTabs(){
        ButterKnife.bind(this,mActivity);
        tabViewPagerAdapter
                = new TabViewPagerAdapter(mActivity.getSupportFragmentManager());

        mViewPager.setAdapter(tabViewPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);

        setupSwitchActions();
        setupActionBar();
        setupBottomNavBar();

    }

    private void setupSwitchActions(){
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch(position){
                    case TAB_DASHBOARD:
                        mMixpanelHelper.trackSwitchedToTab("Dashboard");
                        break;
                    case TAB_SERVICES:
                        mMixpanelHelper.trackSwitchedToTab("Services");
                        break;
                    case TAB_SCAN:
                        mMixpanelHelper.trackSwitchedToTab("Health");
                        break;
                    case TAB_GARAGE:
                        mMixpanelHelper.trackSwitchedToTab("My Garage");
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
                    case TAB_DASHBOARD:
                        bottomNavigationView.setSelectedItemId(R.id.action_dashboard);
                        break;
                    case TAB_SERVICES:
                        bottomNavigationView.setSelectedItemId(R.id.action_services);
                        break;
                    case TAB_SCAN:
                        bottomNavigationView.setSelectedItemId(R.id.action_scan);
                        break;
                    case TAB_GARAGE:
                        bottomNavigationView.setSelectedItemId(R.id.action_garage);
                        break;
                    case TAB_NOTIF:
                        bottomNavigationView.setSelectedItemId(R.id.action_notifications);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setupBottomNavBar(){
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch(item.getItemId()){
                case R.id.action_dashboard:
                    mViewPager.setCurrentItem(TAB_DASHBOARD);
                    return true;
                case R.id.action_services:
                    mViewPager.setCurrentItem(TAB_SERVICES);
                    return true;
                case R.id.action_scan:
                    mViewPager.setCurrentItem(TAB_SCAN);
                    return true;
                case R.id.action_garage:
                    mViewPager.setCurrentItem(TAB_GARAGE);
                    return true;
                case R.id.action_notifications:
                    mViewPager.setCurrentItem(TAB_NOTIF);
                    return true;
                default:
                    return false;
            }
        });
    }

    public void openServices() {
        mViewPager.setCurrentItem(TAB_SERVICES);
        setCurrentServices();
    }
    public void setCurrentServices(){
        ((MainServicesFragment) tabViewPagerAdapter.getItem(1)).setCurrent();
    }

    public void openScanTab() {
        mViewPager.setCurrentItem(TAB_SCAN);
    }
}
