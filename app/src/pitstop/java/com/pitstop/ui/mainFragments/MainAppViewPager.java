package com.pitstop.ui.mainFragments;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.viewpager.widget.ViewPager;

import com.pitstop.R;

/**
 * Created by David on 7/11/2016.
 */
public class
MainAppViewPager extends ViewPager {

    public static final int PAGE_NUM_MAIN_DASHBOARD = 0;
    public static final int PAGE_NUM_MAIN_TOOL = 1;

    private boolean enabled;

    public MainAppViewPager(Context context) {
        super(context);
        this.enabled = true;
    }

    public MainAppViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.enabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(enabled) {
            return super.onTouchEvent(ev);
        } else {
            return false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.enabled && getCurrentItem() == 1) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    public void setPagingEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
