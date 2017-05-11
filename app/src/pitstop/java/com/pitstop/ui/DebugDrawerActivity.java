package com.pitstop.ui;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.utils.NetworkHelper;

import butterknife.ButterKnife;

public abstract class DebugDrawerActivity extends AppCompatActivity {

    private NetworkHelper mNetworkHelper;

    private DrawerLayout mDrawerLayout;
    private View mClearPrefsButton;
    private View mClearDbButton;
    private View mVinButton;
    private EditText mVinField;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)) {
            mNetworkHelper = new NetworkHelper(getApplicationContext());

            mDrawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_debug_drawer, null);
            super.setContentView(mDrawerLayout);

            mClearPrefsButton = findViewById(R.id.debugClearPrefs); // Only default prefs for now
            mClearPrefsButton.setOnClickListener(v -> {
                PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
                Toast.makeText(this, "Preferences Cleared", Toast.LENGTH_SHORT).show();
            });

            mClearDbButton = findViewById(R.id.debugClearDB);
            mClearDbButton.setOnClickListener(v -> {
                LocalDatabaseHelper databaseHelper = LocalDatabaseHelper.getInstance(this);
                SQLiteDatabase db = databaseHelper.getWritableDatabase();
                databaseHelper.onUpgrade(db, 0, 0);
                db.close();
                Toast.makeText(this, "Database Cleared", Toast.LENGTH_SHORT).show();
            });

            mVinField = ButterKnife.findById(mDrawerLayout, R.id.debugVinField);
            mVinButton = findViewById(R.id.debugRandomVin);
            mVinButton.setOnClickListener(v -> mNetworkHelper.getRandomVin(
                    (response, requestError) -> {
                        mVinField.setText(requestError == null ? response : "error: " + requestError.getMessage());
                    })
            );
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
