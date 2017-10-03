package com.pitstop.ui.vehicle_health_report.past_reports;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.report.FullReport;
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

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.reports_recycler_view)
    protected RecyclerView reportsRecyclerView;

    @BindView(R.id.no_reports_view)
    protected View noReportsView;

    @BindView(R.id.loading_view)
    protected View loadingView;

    private PastReportsPresenter presenter;
    private PastReportsAdapter pastReportsAdapter;
    private List<FullReport> reports = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container
            , @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View root = inflater.inflate(R.layout.fragment_past_reports,container,false);
        ButterKnife.bind(this,root);

        //Setup adapter
        pastReportsAdapter = new PastReportsAdapter(this, reports);
        reportsRecyclerView.setAdapter(pastReportsAdapter);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        getActivity().setTitle("Past Reports");

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.d(TAG,"onCreateOptionsMenu()");
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_past_reports,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG,"onOptionsItemSelected()");
        switch(item.getItemId()){
            case R.id.action_sort_date_newest:
                presenter.onSortNewestDateClicked();
                return true;
            case R.id.action_sort_date_oldest:
                presenter.onSortOldestDateClicked();
                return true;
            case R.id.action_sort_engine_issue:
                presenter.onSortEngineIssuesClicked();
                return true;
            case R.id.action_sort_services:
                presenter.onSortServicesClicked();
                return true;
            case R.id.action_sort_recalls:
                presenter.onSortRecallsClicked();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.populateUI();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");
        super.onDestroyView();
        presenter.unsubscribe();
    }

    @Override
    public void displayReports(List<FullReport> reports) {
        Log.d(TAG,"displayReports() reports: "+reports);
        reportsRecyclerView.setVisibility(View.VISIBLE);
        noReportsView.setVisibility(View.GONE);
        this.reports.clear();
        this.reports.addAll(reports);
        pastReportsAdapter.notifyDataSetChanged();
    }

    @Override
    public void displayReport(FullReport report) {
        Log.d(TAG,"displayReport() report: "+report);
        try{
            ((PastReportsViewSwitcher)getActivity()).setReportView(report);
        }catch(ClassCastException e){
            e.printStackTrace();
        }

    }

    @Override
    public void notifyReportDataChange() {
        pastReportsAdapter.notifyDataSetChanged();
    }

    @Override
    public void displayNoHealthReports() {
        noReportsView.setVisibility(View.VISIBLE);
        reportsRecyclerView.setVisibility(View.GONE);
    }

    @Override
    public void onReportClicked(VehicleHealthReport vehicleHealthReport) {
        Log.d(TAG,"onReportClicked() report: "+vehicleHealthReport);
        presenter.onReportClicked(vehicleHealthReport);
    }

    @Override
    public void displayError() {
        Log.d(TAG,"displayError()");
        new AlertDialog.Builder(getActivity())
            .setTitle("Error")
            .setMessage("Error loading reports, please check connection")
            .setCancelable(false)
            .setPositiveButton("Ok", (dialog, id) -> getActivity().finish())
            .create()
            .show();
    }

    @Override
    public void displayLoading(boolean display) {
        Log.d(TAG,"displayLoading() display: "+display);
        if (display){
            loadingView.setVisibility(View.VISIBLE);
            loadingView.bringToFront();
        }else{
            loadingView.setVisibility(View.GONE);
        }
    }

    @Override
    public List<FullReport> getDisplayedReports() {
        return reports;
    }
}
