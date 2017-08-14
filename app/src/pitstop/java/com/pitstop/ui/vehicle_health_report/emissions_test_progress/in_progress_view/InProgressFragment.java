package com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;

/**
 * Created by Matt on 2017-08-14.
 */

public class InProgressFragment extends Fragment implements InProgressView{
    private InProgressPresenter presenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_emissions_progress,container,false);
        presenter = new InProgressPresenter();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.subscribe(this);
    }
}
