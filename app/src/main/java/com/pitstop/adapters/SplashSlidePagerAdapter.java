package com.pitstop.adapters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.login.widget.LoginButton;
import com.pitstop.R;
import com.pitstop.utils.NetworkHelper;

import java.util.ArrayList;

/**
 * Created by David Liu on 2/7/2016.
 */
public class SplashSlidePagerAdapter extends FragmentStatePagerAdapter {

    /**
     * The number of pages (wizard steps) to show in this demo.
     */
//    private static final int NUM_PAGES = 3;
    private static final int NUM_PAGES = 2; //We removed the first page of the splash screen - Aug 30th

    public static final int PAGE_LOGIN = 1;

    ArrayList<Fragment> fragments = new ArrayList<>();

    public SplashSlidePagerAdapter(FragmentManager fm) {
        super(fm);
//        fragments.add(new SplashFragment1());
        fragments.add(new SplashFragment2());
        fragments.add(new SplashFragment3());
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }
//    public static class SplashFragment1 extends Fragment {
//        public SplashFragment1() {}
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                                 Bundle savedInstanceState) {
//            ViewGroup rootView = (ViewGroup) inflater.inflate(
//                    R.layout.splash_1, container, false);
//
////            Picasso.with(getContext()).load(R.drawable.slider1).into((ImageView)rootView.findViewById(R.id.bg));
//
//            return rootView;
//        }
//    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                          int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    public static class SplashFragment2 extends Fragment {
        public SplashFragment2() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.splash_2, container, false);
//            Picasso.with(getContext()).load(R.drawable.slider2).into((ImageView)rootView.findViewById(R.id.bg));

            return rootView;
        }
    }
    public static class SplashFragment3 extends Fragment {

        // facebook things
        private LoginButton facebookLoginButton;

        private NetworkHelper networkHelper;

        public SplashFragment3() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            ViewGroup rootView = (ViewGroup) inflater.inflate(
                    R.layout.splash_login, container, false);

            return rootView;
        }
    }
}
