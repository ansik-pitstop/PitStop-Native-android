package com.pitstop.ui.vehicle_health_report.emissions_test_progress;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.pitstop.R;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.emissions_report_view.EmissionsReportFragment;
import com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view.InProgressFragment;

/**
 * Created by Matt on 2017-08-14.
 */

public class EmissionsProgressActivity extends AppCompatActivity implements EmissionsProgressView,EmissionsProgressCallback {
    private EmissionsProgressPresenter presenter;

    private FragmentManager fragmentManager;

    private InProgressFragment inProgressFragment;
    private EmissionsReportFragment emissionsReportFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emissions_progress);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragmentManager = getFragmentManager();
        presenter = new EmissionsProgressPresenter(this);
        inProgressFragment = new InProgressFragment();
        emissionsReportFragment = new EmissionsReportFragment();
        inProgressFragment.setCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }

    @Override
    public void setColors() {
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(getApplicationContext(),R.color.highlight)));
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this , R.color.highlight_dark));
        }
    }

    @Override
    public void setViewProgress() {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.emissions_progress_fragment_holder,inProgressFragment);
        transaction.commit();
    }


    @Override
    public void setViewReport() {//animate this later
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.emissions_progress_fragment_holder,emissionsReportFragment);
        transaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home){
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void end() {
        finish();
    }
}
