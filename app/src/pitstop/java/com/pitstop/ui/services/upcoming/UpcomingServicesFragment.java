package com.pitstop.ui.services.upcoming;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.issue_detail.IssueDetailsActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.services.ServiceErrorDisplayer;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
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

    @BindView(R.id.upcoming_service_rel_layout)
    RelativeLayout relativeLayout;

    @BindView(R.id.timeline_recyclerview)
    RecyclerView timelineRecyclerView;

    @BindView(R.id.progress)
    View loadingView;

    @BindView(R.id.no_services)
    View noServicesView;

    @BindView(R.id.offline_view)
    View offlineView;

    @BindView(R.id.unknown_error_view)
    View unknownErrorView;

    private SwipeRefreshLayout parentSwipeRefreshLayout;

    private TimelineAdapter timelineAdapter;

    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;

    private UpcomingServicesPresenter presenter;
    private LinkedHashMap<Integer, List<UpcomingService>> upcomingServices = new LinkedHashMap<>();
    private List<Integer> listOfMileages =  new ArrayList<>();
    private boolean hasBeenPopulated = false;
    private boolean isRefreshing = false;
    private ServiceErrorDisplayer serviceErrorDisplayer;

    private LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT);

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

        timelineAdapter = new TimelineAdapter(upcomingServices,listOfMileages, this);
        timelineRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        timelineRecyclerView.setNestedScrollingEnabled(false);
        timelineRecyclerView.setAdapter(timelineAdapter);

        return view;
    }

    public void setParentSwipeRefreshLayout(SwipeRefreshLayout swipeRefreshLayout){
        Log.d(TAG,"setParentSwipeRefreshLayout()");
        this.parentSwipeRefreshLayout = swipeRefreshLayout;
    }

    public void setErrorMessageDisplayer(ServiceErrorDisplayer serviceErrorDisplayer){
        Log.d(TAG,"setErrorMessageDisplayer()");
        this.serviceErrorDisplayer = serviceErrorDisplayer;
    }

    public void onRefresh(){
        Log.d(TAG,"onRefresh()");
        if (presenter != null) presenter.onRefresh();
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
        params.gravity = Gravity.CENTER_VERTICAL;
        relativeLayout.setLayoutParams(params);
        Log.d(TAG,"displayNoServices()");
        timelineRecyclerView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.VISIBLE);
        relativeLayout.bringToFront();
        noServicesView.bringToFront();
    }

    @Override
    public void showLoading() {
        Log.d(TAG,"showLoading()");
        if (parentSwipeRefreshLayout != null && !parentSwipeRefreshLayout.isRefreshing()) {
            params.gravity = Gravity.CENTER_VERTICAL;
            relativeLayout.setLayoutParams(params);
            timelineRecyclerView.setVisibility(View.GONE);
            unknownErrorView.setVisibility(View.GONE);
            offlineView.setVisibility(View.GONE);
            noCarView.setVisibility(View.GONE);
            noServicesView.setVisibility(View.GONE);
            loadingView.setVisibility(View.VISIBLE);
            relativeLayout.bringToFront();
            loadingView.bringToFront();
            parentSwipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public void hideLoading() {
        Log.d(TAG,"hideLoading()");
        if (parentSwipeRefreshLayout != null && !parentSwipeRefreshLayout.isRefreshing()){
            params.gravity = Gravity.CENTER_VERTICAL;
            relativeLayout.setLayoutParams(params);
            parentSwipeRefreshLayout.setEnabled(true);
            noCarView.setVisibility(View.GONE);
            noServicesView.setVisibility(View.GONE);
            loadingView.setVisibility(View.GONE);
            relativeLayout.bringToFront();
            timelineRecyclerView.bringToFront();
        }else if (parentSwipeRefreshLayout != null){
            parentSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void hideRefreshing() {
        if (parentSwipeRefreshLayout != null) parentSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public boolean isRefreshing() {
        return parentSwipeRefreshLayout == null? null: parentSwipeRefreshLayout.isRefreshing();
    }

    @Override
    public void displayOfflineErrorDialog() {
        Log.d(TAG,"displayOfflineErrorDialog()");
        if (serviceErrorDisplayer != null)
            serviceErrorDisplayer.displayServiceErrorDialog(R.string.offline_error);

//        if (offlineAlertDialog == null){
//            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
//            alertDialogBuilder.setTitle(R.string.offline_error_title);
//            alertDialogBuilder
//                    .setMessage(R.string.offline_error)
//                    .setCancelable(true)
//                    .setPositiveButton(R.string.ok, (dialog, id) -> {
//                        dialog.dismiss();
//                    });
//            offlineAlertDialog = alertDialogBuilder.create();
//        }
//
//        offlineAlertDialog.show();

    }

    @Override
    public void displayToast(int error) {
        Toast.makeText(getContext(),error,Toast.LENGTH_LONG).show();
    }

    @Override
    public void displayUnknownErrorDialog() {
        Log.d(TAG,"displayUnknownErrorDialog()");
        if (serviceErrorDisplayer != null)
            serviceErrorDisplayer.displayServiceErrorDialog(R.string.unknown_error);

//        if (unknownErrorDialog == null){
//            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
//            alertDialogBuilder.setTitle(R.string.unknown_error_title);
//            alertDialogBuilder
//                    .setMessage(R.string.unknown_error)
//                    .setCancelable(true)
//                    .setPositiveButton(R.string.ok, (dialog, id) -> {
//                        dialog.dismiss();
//                    });
//            unknownErrorDialog = alertDialogBuilder.create();
//        }
//
//        unknownErrorDialog.show();
    }

    @Override
    public void displayUnknownErrorView() {
        params.gravity = Gravity.CENTER_VERTICAL;
        relativeLayout.setLayoutParams(params);
        Log.d(TAG,"displayUnknownErrorView()");
        offlineView.setVisibility(View.GONE);
        timelineRecyclerView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.VISIBLE);
        relativeLayout.bringToFront();
        unknownErrorView.bringToFront();
    }

    @Override
    public void displayOfflineView() {
        Log.d(TAG,"displayOfflineView()");
        params.gravity = Gravity.CENTER_VERTICAL;
        relativeLayout.setLayoutParams(params);
        timelineRecyclerView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.VISIBLE);
        relativeLayout.bringToFront();
        offlineView.bringToFront();
    }

    @Override
    public void displayOnlineView() {
        Log.d(TAG,"displayOnlineView()");
        relativeLayout.setGravity(Gravity.NO_GRAVITY);
        params.gravity = Gravity.NO_GRAVITY;
        relativeLayout.setLayoutParams(params);
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
        params.gravity = Gravity.CENTER_VERTICAL;
        relativeLayout.setLayoutParams(params);
        timelineRecyclerView.setVisibility(View.GONE);
        noServicesView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noCarView.setVisibility(View.VISIBLE);
        relativeLayout.bringToFront();
        noCarView.bringToFront();
    }

    @Override
    public void startAddCarActivity() {
        Log.d(TAG,"startAddCarActivity()");
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), AddCarActivity.class);
        getActivity().startActivityForResult(intent, MainActivity.RC_ADD_CAR);
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
