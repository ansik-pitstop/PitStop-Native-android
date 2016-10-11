package com.pitstop.ui.addCarFragments;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.pitstop.R;

/**
 * Created by David on 7/11/2016.
 */
public class AddCarViewPager extends ViewPager {

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
