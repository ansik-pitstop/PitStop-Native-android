package com.pitstop.ui;

import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.pitstop.BuildConfig;
import com.pitstop.R;

public abstract class DebugDrawerActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private View mClearPrefsButton;
    private View mClearDbButton;
    private View mVinButton;
    private EditText mVinField;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)) {
            mDrawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_debug_drawer, null);
            super.setContentView(mDrawerLayout);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        setContentView(getLayoutInflater().inflate(layoutResID, mDrawerLayout, false));
    }

    @Override
    public void setContentView(View view) {
        if (!BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)) {
            mDrawerLayout.addView(view, 0);
        } else {
            super.setContentView(view);
        }
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (!BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)) {
            DrawerLayout drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_debug_drawer, null);
            drawerLayout.addView(view, 0, params);
            super.setContentView(drawerLayout);
        } else {
            super.setContentView(view, params);
        }
    }

}
