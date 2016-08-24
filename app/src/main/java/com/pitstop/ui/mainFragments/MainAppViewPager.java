package com.pitstop.ui.mainFragments;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.pitstop.R;

/**
 * Created by David on 7/11/2016.
 */
public class MainAppViewPager extends ViewPager {
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
