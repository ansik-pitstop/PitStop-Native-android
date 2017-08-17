package com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;

/**
 * Created by Matt on 2017-08-17.
 */

public class HealthReportFragment extends Fragment implements HealthReportView {

    private HealthReportPresenter presenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_health_report,container,false);
        presenter = new HealthReportPresenter();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.unsubscribe();
    }
}
