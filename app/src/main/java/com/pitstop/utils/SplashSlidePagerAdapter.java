package com.pitstop.utils;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;

import java.util.ArrayList;

/**
 * Created by David Liu on 2/7/2016.
 */
public class SplashSlidePagerAdapter extends FragmentStatePagerAdapter {

    /**
     * The number of pages (wizard steps) to show in this demo.
     */
    private static final int NUM_PAGES = 4;

    ArrayList<Fragment> fragments = new ArrayList<>();

    public SplashSlidePagerAdapter(FragmentManager fm) {
        super(fm);
        fragments.add(new SplashFragment1());
        fragments.add(new SplashFragment2());
        fragments.add(new SplashFragment3());
        fragments.add(new SplashFragment4());
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }


    public class SplashFragment1 extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.splash_1, container, false);

            return rootView;
        }
    }
    public class SplashFragment2 extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.splash_2, container, false);

            return rootView;
        }
    }
    public class SplashFragment3 extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.splash_3, container, false);

            return rootView;
        }
    }
    public class SplashFragment4 extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.splash_login, container, false);

            return rootView;
        }
    }
}
