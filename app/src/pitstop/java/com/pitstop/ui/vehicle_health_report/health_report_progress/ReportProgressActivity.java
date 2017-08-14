package com.pitstop.ui.vehicle_health_report.health_report_progress;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.pitstop.R;

/**
 * Created by Matt on 2017-08-14.
 */

public class ReportProgressActivity extends AppCompatActivity implements ReportProgressView,ReportProgressCallback {

    private ReportProgressPresenter presenter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_progress);
        presenter = new ReportProgressPresenter(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }
}
