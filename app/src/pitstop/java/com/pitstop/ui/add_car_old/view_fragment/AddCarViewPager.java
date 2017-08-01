package com.pitstop.ui.add_car_old.view_fragment;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by David on 7/11/2016.
 */
public class AddCarViewPager extends ViewPager {

    public static final int PAGE_FIRST = 0;
    public static final int PAGE_VIN = 1;
    public static final int PAGE_DEALERSHIP = 2;

    public AddCarViewPager(Context context) {
        super(context);
    }

    public AddCarViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item);
    }
}
