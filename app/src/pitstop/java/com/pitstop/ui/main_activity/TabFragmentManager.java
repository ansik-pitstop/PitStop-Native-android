package com.pitstop.ui.main_activity;

import android.support.design.widget.TabLayout;
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
    public static final int TAB_NOTIF = 3;

    public static final String[] TAB_NAMES = {"Dashboard","Services","Vehicle Health Report ","Notifications"};

    @BindView(R.id.main_tablayout)
    TabLayout mTabLayout;

    @BindView(R.id.main_container)
    ViewPager mViewPager;

    @BindView(R.id.toolbar)
    Toolbar mToolbar;

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
        mViewPager.setOffscreenPageLimit(3);

        setupSwitchActions();
        setupActionBar();
        setupTabIcons();
        setupTabTappable();

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

                        break;
                    case TAB_SERVICES:
                        mMixpanelHelper.trackSwitchedToTab("Services");
                        break;
                    case TAB_SCAN:
                        mMixpanelHelper.trackSwitchedToTab("Health");
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
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    private void setupTabIcons(){
        mTabLayout.setupWithViewPager(mViewPager);

        int[] tabIcons = {R.drawable.ic_dashboard,R.drawable.history
                ,R.drawable.scan_icon,R.drawable.ic_notifications_white_24dp};

        for (int i=0;i<tabIcons.length;i++){
            try{
                mTabLayout.getTabAt(i).setIcon(tabIcons[i]);
            }catch(java.lang.NullPointerException e){

            }
        }
    }

    private void setupTabTappable(){
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){

                    case TAB_DASHBOARD:
                        //Go to dashboard fragment
                        mViewPager.setCurrentItem(TAB_DASHBOARD);
                        break;

                    case TAB_SERVICES:
                        //Go to services fragment
                        mViewPager.setCurrentItem(TAB_SERVICES);
                        break;

                    case TAB_SCAN:
                        //Go to scan fragment
                        mViewPager.setCurrentItem(TAB_SCAN);
                        break;

                    case TAB_NOTIF:
                        //Go to notifications fragment
                        mViewPager.setCurrentItem(TAB_NOTIF);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //do nothing
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

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
