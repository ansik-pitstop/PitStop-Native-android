package com.pitstop.ui.vehicle_health_report.past_reports;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class PastReportsFragment extends Fragment implements PastReportsView {

    @BindView(R.id.reports_recycler_view)
    protected RecyclerView reportsRecyclerView;

    private PastReportsPresenter presenter;
    private PastReportsAdapter pastReportsAdapter;
    private List<VehicleHealthReport> vehicleHealthReports = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container
            , @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_past_reports,container);
        ButterKnife.bind(this,root);

        //Setup adapter
        pastReportsAdapter = new PastReportsAdapter(this, vehicleHealthReports);
        reportsRecyclerView.setAdapter(pastReportsAdapter);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //Create presenter
        if (presenter == null){
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();
            MixpanelHelper mixpanelHelper
                    = new MixpanelHelper((GlobalApplication)getActivity().getApplicationContext());
            presenter = new PastReportsPresenter(useCaseComponent,mixpanelHelper);
        }
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.populateUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.unsubscribe();
    }

    @Override
    public void displayHealthReports(List<VehicleHealthReport> vehicleHealthReports) {
        this.vehicleHealthReports.addAll(vehicleHealthReports);
        pastReportsAdapter.notifyDataSetChanged();
    }
}
