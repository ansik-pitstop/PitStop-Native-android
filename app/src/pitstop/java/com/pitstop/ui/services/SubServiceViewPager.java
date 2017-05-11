package com.pitstop.ui.services;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

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
