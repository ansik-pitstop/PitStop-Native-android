package com.pitstop.ui.services.upcoming;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class UpcomingServicesFragment extends Fragment implements UpcomingServicesView {

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.timeline_recyclerview)
    RecyclerView timelineRecyclerView;

    @BindView(R.id.progress)
    View loadingView;

    @BindView(R.id.no_services)
    View noServicesView;

    @BindView(R.id.offline_view)
    View offlineView;

    @BindView(R.id.activity_timeline)
    SwipeRefreshLayout swipeRefreshLayout;

    private List<Object> timelineDisplayList;
    private TimelineAdapter timelineAdapter;

    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;

    private UpcomingServicesPresenter presenter;
    private Map<Integer, List<UpcomingService>> upcomingServices = new HashMap<>();
    private boolean hasBeenPopulated = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");

        View view = inflater.inflate(R.layout.fragment_upcoming_services, container, false);
        ButterKnife.bind(this, view);

        if (presenter == null){
            MixpanelHelper mixPanelHelper = new MixpanelHelper(
                    (GlobalApplication)getActivity().getApplicationContext());
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getActivity()))
                    .build();
            presenter = new UpcomingServicesPresenter(useCaseComponent,mixPanelHelper);
        }
        presenter.subscribe(this);

        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());
        timelineDisplayList = new ArrayList<>();
        timelineAdapter = new TimelineAdapter(timelineDisplayList);
        timelineRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        timelineRecyclerView.setNestedScrollingEnabled(true);
        timelineRecyclerView.setAdapter(timelineAdapter);
        presenter.onUpdateNeeded();

        return view;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");
        presenter.unsubscribe();
        hasBeenPopulated = false;
        super.onDestroyView();
    }

    @Override
    public void displayNoServices() {
        Log.d(TAG,"displayNoServices()");
        timelineRecyclerView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.VISIBLE);
    }

    @Override
    public void showLoading() {
        Log.d(TAG,"showLoading()");
        if (!swipeRefreshLayout.isRefreshing()) {
            loadingView.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public void hideLoading() {
        Log.d(TAG,"hideLoading()");
        if (!swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setEnabled(true);
            loadingView.setVisibility(View.GONE);
        }else{
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void displayOfflineErrorDialog() {
        Log.d(TAG,"displayOfflineErrorDialog()");
        if (offlineAlertDialog == null){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.offline_error_title);
            alertDialogBuilder
                    .setMessage(R.string.offline_error)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        dialog.dismiss();
                    });
            offlineAlertDialog = alertDialogBuilder.create();
        }

        offlineAlertDialog.show();
    }

    @Override
    public void displayUnknownErrorDialog() {
        Log.d(TAG,"displayUnknownErrorDialog()");
        if (unknownErrorDialog == null){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.unknown_error_title);
            alertDialogBuilder
                    .setMessage(R.string.unknown_error)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        dialog.dismiss();
                    });
            unknownErrorDialog = alertDialogBuilder.create();
        }

        unknownErrorDialog.show();
    }

    @Override
    public void displayOfflineView() {
        Log.d(TAG,"displayOfflineView()");
        timelineRecyclerView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.GONE);
        offlineView.setVisibility(View.VISIBLE);
        offlineView.bringToFront();
    }

    @Override
    public void displayOnlineView() {
        Log.d(TAG,"displayOnlineView()");
        timelineRecyclerView.setVisibility(View.VISIBLE);
        noServicesView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        timelineRecyclerView.bringToFront();
    }

    @Override
    public void populateUpcomingServices(Map<Integer, List<UpcomingService>> upcomingServices) {
        Log.d(TAG,"populateUpcomingServices() size: "+upcomingServices.size());
        hasBeenPopulated = true;
        timelineDisplayList.clear();
        this.upcomingServices.clear();
        this.upcomingServices.putAll(upcomingServices);

        for (Integer mileage : upcomingServices.keySet()){
            timelineDisplayList.add(String.valueOf(mileage));
            timelineDisplayList.addAll(upcomingServices.get(mileage));
        }

        timelineAdapter.notifyDataSetChanged();

    }

    @Override
    public boolean hasBeenPopulated() {
        Log.d(TAG,"hasBeenPopulated() ? "+hasBeenPopulated);
        return hasBeenPopulated;
    }

    @OnClick(R.id.offline_try_again)
    public void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        presenter.onOfflineTryAgainClicked();
    }
}
