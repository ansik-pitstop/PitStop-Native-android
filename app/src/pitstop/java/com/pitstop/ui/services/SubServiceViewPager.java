package com.pitstop.ui.services;

import android.content.Context;
import android.util.AttributeSet;

import androidx.viewpager.widget.ViewPager;

/**
 * Created by Karol Zdebel on 5/10/2017.
 */

public class SubServiceViewPager extends ViewPager {


    public SubServiceViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return false;
    }
}
