package com.pitstop.ui.services.upcoming;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.issue_detail.IssueDetailsActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class UpcomingServicesFragment extends Fragment implements UpcomingServicesView {

    public static final String UPCOMING_SERVICE_KEY = "upcomingService" ;
    public static final String SOURCE = "source";
    public static final String UPCOMING_SERVICE_SOURCE ="fromUpcomingService" ;
    public static final String UPCOMING_SERVICE_POSITION = "position" ;

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.no_car)
    View noCarView;
/*

    @BindView(R.id.upcoming_service_rel_layout)
    RelativeLayout relativeLayout;
*/

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

    @BindView(R.id.unknown_error_view)
    View unknownErrorView;


    private TimelineAdapter timelineAdapter;

    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;

    private UpcomingServicesPresenter presenter;
    private LinkedHashMap<Integer, List<UpcomingService>> upcomingServices = new LinkedHashMap<>();
    private List<Integer> listOfMileages =  new ArrayList<>();
    private boolean hasBeenPopulated = false;
    private boolean isRefreshing = false;

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

        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        timelineAdapter = new TimelineAdapter(upcomingServices,listOfMileages, this);
        timelineRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        timelineRecyclerView.setNestedScrollingEnabled(true);
        timelineRecyclerView.setAdapter(timelineAdapter);

        RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager manager = ((LinearLayoutManager)recyclerView.getLayoutManager());
                boolean enabled =manager.findFirstCompletelyVisibleItemPosition() == 0;
                swipeRefreshLayout.setEnabled(enabled);
            }
        };

        timelineRecyclerView.setOnScrollListener(scrollListener);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        presenter.subscribe(this);
        presenter.onUpdateNeeded();

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
        noCarView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.VISIBLE);
        noServicesView.bringToFront();
    }

    @Override
    public void showLoading() {
        Log.d(TAG,"showLoading()");
        if (!swipeRefreshLayout.isRefreshing()) {
            timelineRecyclerView.setVisibility(View.GONE);
            unknownErrorView.setVisibility(View.GONE);
            offlineView.setVisibility(View.GONE);
            loadingView.setVisibility(View.VISIBLE);
            loadingView.bringToFront();
            swipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public void hideLoading() {
        Log.d(TAG,"hideLoading()");
        if (!swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setEnabled(true);
            loadingView.setVisibility(View.GONE);
            timelineRecyclerView.bringToFront();
        }else{
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void hideRefreshing() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public boolean isRefreshing() {
        return swipeRefreshLayout.isRefreshing();
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
    public void displayUnknownErrorView() {
        Log.d(TAG,"displayUnknownErrorView()");
        offlineView.setVisibility(View.GONE);
        timelineRecyclerView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.VISIBLE);
        unknownErrorView.bringToFront();
    }

    @Override
    public void displayOfflineView() {
        Log.d(TAG,"displayOfflineView()");
        timelineRecyclerView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.VISIBLE);
        offlineView.bringToFront();
    }

    @Override
    public void displayOnlineView() {
        Log.d(TAG,"displayOnlineView()");

        noCarView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        timelineRecyclerView.setVisibility(View.VISIBLE);
        timelineRecyclerView.bringToFront();
    }

    @OnClick(R.id.addCarButton)
    public void onAddCarButtonClicked(){
        Log.d(TAG,"onAddCarButtonClicked()");
        presenter.onAddCarButtonClicked();
    }

    @OnClick(R.id.unknown_error_try_again)
    public void onUnknownErrorTryAgainClicked(){
        Log.d(TAG,"onUnknownErrorTryAgainClicked()");
        presenter.onUnknownErrorTryAgainClicked();
    }

    @Override
    public void displayNoCarView() {
        Log.d(TAG,"displayNoCarView()");
        offlineView.setVisibility(View.GONE);
        timelineRecyclerView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        noCarView.setVisibility(View.VISIBLE);
        noCarView.bringToFront();
    }

    @Override
    public void startAddCarActivity() {
        Log.d(TAG,"startAddCarActivity()");
        Intent intent = new Intent(getActivity(), AddCarActivity.class);
        startActivityForResult(intent, MainActivity.RC_ADD_CAR);
    }

    @Override
    public void populateUpcomingServices(Map<Integer, List<UpcomingService>> upcomingServices) {
        Log.d(TAG,"populateUpcomingServices() size: "+upcomingServices.size());
        hasBeenPopulated = true;
        this.upcomingServices.clear();
        this.upcomingServices.putAll(upcomingServices);
        this.listOfMileages.clear();
        Set<Integer> mileageSet = upcomingServices.keySet();
        for (int i: mileageSet){
            this.listOfMileages.add(i);
        }
        timelineAdapter.notifyDataSetChanged();

    }

    @Override
    public void onUpcomingServiceClicked(ArrayList<UpcomingService> services, int positionClicked) {
        Log.d(TAG, "onUpcomingServiceClicked()");
        presenter.onUpcomingServiceClicked(services, positionClicked);
    }

    @Override
    public void openIssueDetailsActivity(ArrayList<UpcomingService> services, int position) {
        Log.d(TAG, "openIssueDetailsActivity()");
        Intent intent = new Intent(getActivity(), IssueDetailsActivity.class);
        intent.putParcelableArrayListExtra(UPCOMING_SERVICE_KEY, services);
        intent.putExtra(UPCOMING_SERVICE_POSITION, position);
        intent.putExtra(IssueDetailsActivity.SOURCE, UPCOMING_SERVICE_SOURCE);
        startActivity(intent);

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
