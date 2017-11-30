package com.pitstop.ui.services;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCurrentCarDealershipUseCase;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.ui.services.current.CurrentServicesFragment;
import com.pitstop.ui.services.history.HistoryServicesFragment;
import com.pitstop.ui.services.upcoming.UpcomingServicesFragment;

import org.jetbrains.annotations.NotNull;

public class MainServicesFragment extends Fragment{

    private final String TAG = MainServicesFragment.class.getSimpleName();

    private SubServiceViewPager mServicesPager;
    private TabLayout tabLayout;

    private UseCaseComponent useCaseComponent;

    public static MainServicesFragment newInstance() {
        MainServicesFragment fragment = new MainServicesFragment();
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mServicesPager = (SubServiceViewPager)getActivity().findViewById(R.id.services_viewpager);
        mServicesPager.setOffscreenPageLimit(2);

        //Create tab layout
        tabLayout = (TabLayout) getActivity().findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.upcoming_services)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.current_services)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.history_services)));
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

        mServicesPager.setAdapter(new ServicesAdapter(getChildFragmentManager()));
        tabLayout.getTabAt(1).select();

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootview = inflater.inflate(R.layout.fragment_services,null);
        mServicesPager = (SubServiceViewPager)getActivity().findViewById(R.id.services_viewpager);

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext().getApplicationContext()))
                .build();

        loadDealershipCustomDesign();
        return rootview;
    }

    private void loadDealershipCustomDesign(){
        useCaseComponent.getGetCurrentDealershipUseCase().execute(new GetCurrentCarDealershipUseCase.Callback() {
            @Override
            public void onGotDealership(@NotNull Dealership dealership) {
                if (dealership != null){
                    if (BuildConfig.DEBUG
                            && (dealership.getId() == 4
                            || dealership.getId() == 18)){

                        bindMercedesDealerUI();

                    }else if (!BuildConfig.DEBUG
                            && dealership.getId() == 14) {

                        bindMercedesDealerUI();

                    }
                    else{
                        bindDefaultDealerUI();
                    }
                }
            }

            @Override
            public void onNoCarExists() {

            }

            @Override
            public void onError(@NotNull RequestError error) {

            }
        });
    }

    private void bindDefaultDealerUI(){
        //Get the themes default primary color
        TypedValue defaultColor = new TypedValue();
        if(getActivity() == null){return;}
        getActivity().getTheme().resolveAttribute(android.R.attr.colorPrimary, defaultColor, true);

        //Set other changed UI elements back to original color
        tabLayout.setBackgroundColor(defaultColor.data);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() == null) return;
        loadDealershipCustomDesign();
    }

    private void bindMercedesDealerUI(){
        tabLayout.setBackgroundColor(Color.BLACK);
    }

    //Return data associated with fragment of the provided tab
    class ServicesAdapter extends FragmentPagerAdapter {

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
                    return new UpcomingServicesFragment();
                case FRAGMENT_CURRENT:
                    return new CurrentServicesFragment();
                case FRAGMENT_HISTORY:
                    return new HistoryServicesFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return FRAGMENT_COUNT;
        }
    }

    public void setCurrent(){
        if (mServicesPager == null){
            return;
        }
        tabLayout.getTabAt(1).select();
        //mServicesPager.setCurrentItem(1);
    }

}
