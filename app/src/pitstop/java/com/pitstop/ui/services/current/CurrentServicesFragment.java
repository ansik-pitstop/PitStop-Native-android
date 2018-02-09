package com.pitstop.ui.services.current;

import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.widget.DatePicker;
import android.widget.LinearLayout;

import com.pitstop.R;
import com.pitstop.adapters.ServicesAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.main_activity.BadgeDisplayer;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.main_activity.MainActivityCallback;
import com.pitstop.ui.services.ServiceErrorDisplayer;
import com.pitstop.ui.services.ServicesDatePickerDialog;
import com.pitstop.ui.services.custom_service.CustomServiceActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Karol Zdebel on 8/30/2017.
 */

public class CurrentServicesFragment extends Fragment implements CurrentServicesView
        , IssueHolderListener{

    public final String TAG = getClass().getSimpleName();

    private final int RC_CUSTOM_ISSUE = 1;

    @BindView(R.id.no_car)
    View noCarView;

    @BindView(R.id.routine_services_recycler_view)
    protected RecyclerView routineServicesRecyclerView;

    @BindView(R.id.progress)
    View loadingView;

    @BindView(R.id.my_services_recycler_view)
    RecyclerView myServicesRecyclerView;

    @BindView(R.id.stored_engine_issues_recycler_view)
    RecyclerView storedEngineIssuesRecyclerView;

    @BindView(R.id.potential_engine_issues_recycler_view)
    RecyclerView potentialEngineIssuesRecyclerView;

    @BindView(R.id.recalls_recycler_view)
    RecyclerView recallsRecyclerView;

    @BindView(R.id.stored_engine_issues_holder)
    LinearLayout storedEngineIssuesHolder;

    @BindView(R.id.potential_engine_issues_holder)
    LinearLayout potentialEngineIssuesHolder;

    @BindView(R.id.recalls_holder)
    LinearLayout recallsHolder;

    @BindView(R.id.routine_serivces_holder)
    LinearLayout routineServicesHolder;

    @BindView(R.id.offline_view)
    View offlineView;

    @BindView(R.id.reg_view)
    View regView;

    @BindView(R.id.unknown_error_view)
    View unknownErrorView;

    @BindView(R.id.my_services_holder)
    View myServicesHolder;

    @BindView(R.id.no_services_card)
    View noServicesCard;

    @BindView(R.id.move_history)
    View moveToHistoryView;

    @BindView(R.id.move_history_hidden)
    View moveToHistoryHiddenView;

    /*Adapters used to convert CarIssue list into RecyclerView*/
    private ServicesAdapter routineServicesAdapter;
    private ServicesAdapter myServicesAdapter;
    private ServicesAdapter storedEngineIssuesAdapter;
    private ServicesAdapter potentialEngineIssueAdapter;
    private ServicesAdapter recallAdapter;

    private CurrentServicesPresenter presenter;
    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;
    private boolean hasBeenPopulated = false;
    private SwipeRefreshLayout parentSwipeRefreshLayout;
    private ServiceErrorDisplayer serviceErrorDisplayer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view =  inflater.inflate(R.layout.fragment_new_services, container, false);
        ButterKnife.bind(this, view);

        if (presenter == null){
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent
                    .builder().contextModule(new ContextModule(getContext())).build();
            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication)getContext().getApplicationContext());
            presenter = new CurrentServicesPresenter(useCaseComponent,mixpanelHelper);
        }

        routineServicesRecyclerView.setNestedScrollingEnabled(false);
        myServicesRecyclerView.setNestedScrollingEnabled(false);
        potentialEngineIssuesRecyclerView.setNestedScrollingEnabled(false);
        recallsRecyclerView.setNestedScrollingEnabled(false);
        storedEngineIssuesRecyclerView.setNestedScrollingEnabled(false);

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
        if (presenter != null)
            presenter.onRefresh();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.onUpdateNeeded();
    }

    @OnClick(R.id.create_service)
    public void onCustomServiceButtonClicked(){
        Log.d(TAG,"onCustomServiceButtonClicked()");
        presenter.onCustomServiceButtonClicked();
    }

    @OnClick(R.id.move_history)
    public void onMoveToHistoryClicked(){
        Log.d(TAG,"onMoveToHistoryClicked()");
        presenter.onMoveToHistoryClicked();
    }

    @Override
    public void startCustomServiceActivity(){
        Log.d(TAG,"startCustomServiceActivity()");
        Intent intent =  new Intent(getActivity(), CustomServiceActivity.class);
        intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA,false);
        startActivityForResult(intent,RC_CUSTOM_ISSUE);
    }

    @Override
    public void notifyIssueDataChanged() {
        Log.d(TAG,"notifyIssueDataChanged()");
        recallAdapter.notifyDataSetChanged();
        potentialEngineIssueAdapter.notifyDataSetChanged();
        storedEngineIssuesAdapter.notifyDataSetChanged();
        myServicesAdapter.notifyDataSetChanged();
        routineServicesAdapter.notifyDataSetChanged();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,"onActivityResult()");

        if (requestCode == RC_CUSTOM_ISSUE && data != null){
            CarIssue carIssue = data.getParcelableExtra(CarIssue.class.getName());
            if (carIssue != null){
                presenter.onCustomIssueCreated(carIssue);
            }
        }
        else{
            super.onActivityResult(requestCode, resultCode, data);
        }
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
        Log.d(TAG,"displayUnknownErrorView()");
        offlineView.setVisibility(View.GONE);
        regView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.VISIBLE);
        unknownErrorView.bringToFront();
    }

    @Override
    public void displayOfflineView() {
        Log.d(TAG,"displayOfflineView()");
        offlineView.setVisibility(View.VISIBLE);
        regView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        offlineView.bringToFront();
    }

    @Override
    public void displayOnlineView() {
        Log.d(TAG,"displayOnlineView()");
        offlineView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        regView.setVisibility(View.VISIBLE);
        regView.bringToFront();
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
        regView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        noCarView.setVisibility(View.VISIBLE);
    }

    @Override
    public void startAddCarActivity() {
        Log.d(TAG,"startAddCarActivity()");
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), AddCarActivity.class);
        getActivity().startActivityForResult(intent, MainActivity.RC_ADD_CAR);
    }

    @OnClick(R.id.offline_try_again)
    public void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        presenter.onOfflineTryAgainClicked();
    }

    @Override
    public void displayNoServices(boolean visible) {
        if (visible)
            noServicesCard.setVisibility(View.VISIBLE);
        else
            noServicesCard.setVisibility(View.GONE);
    }

    @Override
    public void showMyServicesView(boolean show) {
        if (show)
            myServicesHolder.setVisibility(View.VISIBLE);
        else
            myServicesHolder.setVisibility(View.GONE);
    }

    @Override
    public void showRoutineServicesView(boolean show) {
        if (show)
            routineServicesHolder.setVisibility(View.VISIBLE);
        else
            routineServicesHolder.setVisibility(View.GONE);
    }

    @Override
    public void showPotentialEngineIssuesView(boolean show) {
        if (show)
            potentialEngineIssuesHolder.setVisibility(View.VISIBLE);
        else
            potentialEngineIssuesHolder.setVisibility(View.GONE);
    }

    @Override
    public void showStoredEngineIssuesView(boolean show) {
        if (show)
            storedEngineIssuesHolder.setVisibility(View.VISIBLE);
        else
            storedEngineIssuesHolder.setVisibility(View.GONE);
    }

    @Override
    public void showRecallsView(boolean show) {
        if (show)
            recallsHolder.setVisibility(View.VISIBLE);
        else
            recallsHolder.setVisibility(View.GONE);
    }

    @Override
    public void showMoveToHistory(boolean show) {
        if (show){
            moveToHistoryView.setVisibility(View.VISIBLE);
            moveToHistoryHiddenView.setVisibility(View.GONE);
        }
        else{
            moveToHistoryView.setVisibility(View.GONE);
            moveToHistoryHiddenView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public boolean hasBeenPopulated() {
        Log.d(TAG,"hasBeenPopulated()");
        return hasBeenPopulated;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");
        presenter.unsubscribe();
        hasBeenPopulated = false;
        super.onDestroyView();
    }

    @Override
    public void showLoading() {
        Log.d(TAG,"showLoading()");
        if (parentSwipeRefreshLayout != null && !parentSwipeRefreshLayout.isRefreshing()) {
            loadingView.setVisibility(View.VISIBLE);
            loadingView.bringToFront();
            parentSwipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public void hideLoading() {
        Log.d(TAG,"hideLoading()");
        if (parentSwipeRefreshLayout != null && !parentSwipeRefreshLayout.isRefreshing()){
            parentSwipeRefreshLayout.setEnabled(true);
            loadingView.setVisibility(View.GONE);
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
    public void displayRoutineServices(List<CarIssue> routineServicesList
            , LinkedHashMap<CarIssue,Boolean> selectionMap) {
        Log.d(TAG,"displayRoutineServices() size(): "+ routineServicesList.size());

        hasBeenPopulated = true;

        if (routineServicesAdapter == null){
            routineServicesAdapter = new ServicesAdapter(routineServicesList, selectionMap
                    ,this);
            routineServicesRecyclerView.setLayoutManager(new LinearLayoutManager(
                    getActivity().getApplicationContext()));
            routineServicesRecyclerView.setAdapter(routineServicesAdapter);
        }else{
            routineServicesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void displayMyServices(List<CarIssue> myServicesList
            , LinkedHashMap<CarIssue,Boolean> selectionMap) {
        Log.d(TAG,"displayMyServices() size(): "+ myServicesList.size());

        hasBeenPopulated = true;
        if (myServicesAdapter == null){
            myServicesAdapter = new ServicesAdapter(myServicesList, selectionMap
                    , this);
            myServicesRecyclerView.setLayoutManager(
                    new LinearLayoutManager(getActivity().getApplicationContext()));
            myServicesRecyclerView.setAdapter(myServicesAdapter);
        }
        else{
            myServicesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void displayStoredEngineIssues(List<CarIssue> storedEngineIssuesList
            , LinkedHashMap<CarIssue,Boolean> selectionMap) {
        Log.d(TAG,"displayStoredEngineIssues() size(): "+ storedEngineIssuesList.size());

        hasBeenPopulated = true;
        if (storedEngineIssuesAdapter == null){
            storedEngineIssuesAdapter = new ServicesAdapter(storedEngineIssuesList, selectionMap
                    , this);
            storedEngineIssuesRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
            storedEngineIssuesRecyclerView.setAdapter(storedEngineIssuesAdapter);
        }else{
            storedEngineIssuesAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void displayPotentialEngineIssues(List<CarIssue> potentialEngineIssueList
            , LinkedHashMap<CarIssue,Boolean> selectionMap) {
        Log.d(TAG,"displayPotentialEngineIssues() size(): "+potentialEngineIssueList.size());

        hasBeenPopulated = true;
        if (potentialEngineIssueAdapter == null){
            potentialEngineIssueAdapter
                    = new ServicesAdapter(potentialEngineIssueList, selectionMap
                    ,this);
            potentialEngineIssuesRecyclerView.setLayoutManager(
                    new LinearLayoutManager(getActivity().getApplicationContext()));
            potentialEngineIssuesRecyclerView.setAdapter(potentialEngineIssueAdapter);
        }else{
            potentialEngineIssueAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void displayRecalls(List<CarIssue> displayRecallsList
            , LinkedHashMap<CarIssue,Boolean> selectionMap) {
        Log.d(TAG,"displayRecalls() size(): "+ displayRecallsList.size());

        hasBeenPopulated = true;
        if (recallAdapter == null){
            recallAdapter = new ServicesAdapter(displayRecallsList, selectionMap
                    ,this);
            recallsRecyclerView.setLayoutManager(
                    new LinearLayoutManager(getActivity().getApplicationContext()));
            recallsRecyclerView.setAdapter(recallAdapter);
        }else{
            recallAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void displayBadge(int count) {
        Log.d(TAG,"displayBadge() count: "+count);
        Activity activity = getActivity();
        try{
            if (activity != null){
                BadgeDisplayer badgeDisplayer = (BadgeDisplayer)activity;
                badgeDisplayer.displayServicesBadgeCount(count);
            }
        }catch(ClassCastException e){
            e.printStackTrace();
        }

    }

    @Override
    public void displayCalendar() {
        Log.d(TAG,"displayCalendar()");
        DatePickerDialog servicesDatePickerDialog = new ServicesDatePickerDialog(getContext()
                , Calendar.getInstance()
                , (DatePicker datePicker, int year, int month, int day)
                    -> presenter.onServiceDoneDatePicked(year,month,day));
        servicesDatePickerDialog.setTitle(getString(R.string.service_date_picker_title));
        servicesDatePickerDialog.show();
    }

    @Override
    public void onServiceClicked(List<CarIssue> carIssues, int position) {
        Log.d(TAG,"onServiceClicked()");
        presenter.onServiceClicked(carIssues, position);

    }

    @Override
    public void startDisplayIssueActivity(List<CarIssue> issues, int position){
        if (getActivity() == null) return;
        ((MainActivityCallback)getActivity()).startDisplayIssueActivity(issues, position);
    }

    @Override
    public void onServiceSelected(CarIssue carIssue) {
        Log.d(TAG,"onServiceSelected()");
        presenter.onServiceSelected(carIssue);
    }
}
