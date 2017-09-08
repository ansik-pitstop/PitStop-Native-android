package com.pitstop.ui.services.history;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ExpandableListView;

import com.pitstop.R;
import com.pitstop.adapters.HistoryIssueGroupAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.services.custom_service.CustomServiceActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HistoryServicesFragment extends Fragment implements HistoryServicesView {

    private final String TAG = getClass().getSimpleName();
    private final int RC_CUSTOM_ISSUE = 190;

    @BindView(R.id.no_car)
    View noCarView;

    @BindView(R.id.progress)
    View loadingView;

    @BindView(R.id.message_card)
    protected View messageCard;

    @BindView(R.id.issue_expandable_list)
    protected ExpandableListView issueGroup;

    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.offline_view)
    View offlineView;

    @BindView(R.id.reg_view)
    View regView;

    @BindView(R.id.unknown_error_view)
    View unknownErrorView;

    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;

    private HistoryIssueGroupAdapter issueGroupAdapter;
    private List<CarIssue> doneServices;


    private HistoryServicesPresenter presenter;
    private boolean hasBeenPopulated = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");

        View view = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, view);

        if (presenter == null){
            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication) getActivity().getApplicationContext());

            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getActivity()))
                    .build();
            presenter = new HistoryServicesPresenter(mixpanelHelper, useCaseComponent);
        }

        swipeRefreshLayout.setOnRefreshListener(()
                -> presenter.onRefresh());

        doneServices = new ArrayList<>();
        issueGroupAdapter = new HistoryIssueGroupAdapter(doneServices);
        issueGroup.setAdapter(issueGroupAdapter);

        //Allow scrolling inside nested refresh view
        issueGroup.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                boolean allow = false;

                if(visibleItemCount>0) {
                    long packedPosition = issueGroup.getExpandableListPosition(firstVisibleItem);
                    int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
                    int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
                    allow = groupPosition==0 && childPosition==-1 && issueGroup.getChildAt(0).getTop()==0;
                }

                swipeRefreshLayout.setEnabled(allow);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG,"onViewCreated()");

        presenter.subscribe(this);
        presenter.onUpdateNeeded();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,"onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_CUSTOM_ISSUE && data != null){
            CarIssue carIssue = data.getParcelableExtra(CarIssue.class.getName());
            if (carIssue != null){
                presenter.onCustomServiceCreated(carIssue);
            }
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");
        super.onDestroyView();
        hasBeenPopulated = false;
        presenter.unsubscribe();
    }

    @Override
    public void showLoading() {
        Log.d(TAG,"showLoading()");
        if (!swipeRefreshLayout.isRefreshing()) {
            loadingView.setVisibility(View.VISIBLE);
            regView.setVisibility(View.GONE);
            loadingView.bringToFront();
            swipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public void hideLoading() {
        Log.d(TAG,"hideLoading()");
        if (!swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setEnabled(true);
            regView.setVisibility(View.VISIBLE);
            loadingView.setVisibility(View.GONE);
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
        regView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayOfflineView() {
        Log.d(TAG,"displayOfflineView()");
        offlineView.setVisibility(View.VISIBLE);
        noCarView.setVisibility(View.GONE);
        regView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
    }

    @Override
    public void displayOnlineView() {
        Log.d(TAG,"displayOnlineView()");
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        regView.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayNoCarView() {
        Log.d(TAG,"displayNoCarView()");
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        regView.setVisibility(View.GONE);
        noCarView.setVisibility(View.VISIBLE);
    }

    @Override
    public void startAddCarActivity() {
        Log.d(TAG,"startAddCarActivity()");
        Intent intent = new Intent(getActivity(), AddCarActivity.class);
        startActivityForResult(intent, MainActivity.RC_ADD_CAR);
    }

    @OnClick(R.id.addCarButton)
    public void onAddCarButtonClicked(){
        Log.d(TAG,"onAddCarButtonClicked()");
        presenter.onAddCarButtonClicked();
    }

    @OnClick(R.id.service_launch_custom)
    public void onCustomServiceButtonClicked(){
        Log.d(TAG,"onCustomServiceButtonClicked()");
        presenter.onCustomServiceButtonClicked();
    }

    @OnClick(R.id.offline_try_again)
    public void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        presenter.onOfflineTryAgainClicked();
    }

    @OnClick(R.id.unknown_error_try_again)
    public void onUnknownErrorTryAgainClicked(){
        Log.d(TAG,"onUnknownErrorTryAgainClicked()");
        presenter.onUnknownErrorTryAgainClicked();
    }

    @Override
    public void populateDoneServices(List<CarIssue> doneServices) {
        Log.d(TAG,"populateDoneServices()");

        hasBeenPopulated = true;
        messageCard.setVisibility(View.GONE);
        issueGroup.setVisibility(View.VISIBLE);

        this.doneServices.clear();
        this.doneServices.addAll(doneServices);

        issueGroupAdapter.notifyDataSetChanged();
    }

    @Override
    public void populateEmptyServices(){
        Log.d(TAG,"populateEmptyServices()");

        hasBeenPopulated = true;
        messageCard.setVisibility(View.VISIBLE);
        issueGroup.setVisibility(View.GONE);
        this.doneServices.clear();
        issueGroupAdapter.notifyDataSetChanged();
    }

    @Override
    public void startCustomServiceActivity() {
        Log.d(TAG,"startCustomServiceActivity()");
        Intent intent = new Intent(getActivity(), CustomServiceActivity.class);
        intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA,true);
        startActivityForResult(intent,RC_CUSTOM_ISSUE);
    }

    @Override
    public void addDoneService(CarIssue doneService) {
        Log.d(TAG,"addDoneService()");
        messageCard.setVisibility(View.GONE);
        issueGroup.setVisibility(View.VISIBLE);
        this.doneServices.add(doneService);
        issueGroupAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean hasBeenPopulated() {
        Log.d(TAG,"hasBeenPopulated() ? "+hasBeenPopulated);
        return hasBeenPopulated;
    }
}
