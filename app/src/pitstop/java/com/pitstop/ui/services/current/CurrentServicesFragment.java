package com.pitstop.ui.services.current;

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
import com.pitstop.adapters.CurrentServicesAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.main_activity.MainActivityCallback;
import com.pitstop.ui.services.ServicesDatePickerDialog;
import com.pitstop.ui.services.custom_service.CustomServiceActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.Calendar;
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

    @BindView(R.id.car_issues_list)
    protected RecyclerView carIssueListView;

    @BindView(R.id.progress)
    View loadingView;

    @BindView(R.id.service_launch_custom)
    LinearLayout customServiceButton;

    @BindView(R.id.custom_issues_list)
    RecyclerView customIssueListRecyclerView;

    @BindView(R.id.engine_list_view)
    RecyclerView engineListView;

    @BindView(R.id.potential_list_view)
    RecyclerView potentialListView;

    @BindView(R.id.recall_list_view)
    RecyclerView recallListView;

    @BindView(R.id.engine_issue_list_holder)
    LinearLayout engineIssueHolder;

    @BindView(R.id.potential_engine_issue_list)
    LinearLayout potentialEngineList;

    @BindView(R.id.recall_list_holder)
    LinearLayout recallListHolder;

    @BindView(R.id.routine_list_holder)
    LinearLayout routineListHolder;

    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.offline_view)
    View offlineView;

    @BindView(R.id.reg_view)
    View regView;

    @BindView(R.id.unknown_error_view)
    View unknownErrorView;

    /*Adapters used to convert CarIssue list into RecyclerView*/
    private CurrentServicesAdapter carIssuesAdapter;
    private CurrentServicesAdapter customIssueAdapter;
    private CurrentServicesAdapter storedEngineIssuesAdapter;
    private CurrentServicesAdapter potentialEngineIssueAdapter;
    private CurrentServicesAdapter recallAdapter;

    /*Displayed services, these lists are referenced through the adapter*/
    List<CarIssue> carIssueList = new ArrayList<>();
    List<CarIssue> customIssueList = new ArrayList<>();
    List<CarIssue> storedEngineIssueList = new ArrayList<>();
    List<CarIssue> potentialEngineIssuesList = new ArrayList<>();
    List<CarIssue> recallList = new ArrayList<>();

    private CurrentServicesPresenter presenter;
    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;
    private boolean hasBeenPopulated = false;

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

        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.onUpdateNeeded();
    }

    @OnClick(R.id.service_launch_custom)
    public void onCustomServiceButtonClicked(){
        Log.d(TAG,"onCustomServiceButtonClicked()");
        presenter.onCustomServiceButtonClicked();
    }

    @Override
    public void startCustomServiceActivity(){
        Log.d(TAG,"startCustomServiceActivity()");
        Intent intent =  new Intent(getActivity(), CustomServiceActivity.class);
        intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA,false);
        startActivityForResult(intent,RC_CUSTOM_ISSUE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,"onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_CUSTOM_ISSUE && data != null){
            CarIssue carIssue = data.getParcelableExtra(CarIssue.class.getName());
            if (carIssue != null){
                presenter.onCustomIssueCreated(carIssue);
            }
        }
    }

    @Override
    public void removeCarIssue(CarIssue issue) {
        Log.d(TAG,"removeCarIssue() carIssue: "+issue.getIssueType());
        if (issue.getIssueType().equals(CarIssue.RECALL)) {
            recallList.remove(issue);
            if (recallList.isEmpty())
                recallListHolder.setVisibility(View.GONE);
            recallAdapter.notifyDataSetChanged();
        } else if (issue.getIssueType().equals(CarIssue.DTC)) {
            storedEngineIssueList.remove(issue);
            if (storedEngineIssueList.isEmpty())
                engineIssueHolder.setVisibility(View.GONE);
            storedEngineIssuesAdapter.notifyDataSetChanged();
        } else if (issue.getIssueType().equals(CarIssue.PENDING_DTC)) {
            potentialEngineIssuesList.remove(issue);
            if (potentialEngineIssuesList.isEmpty())
                potentialListView.setVisibility(View.GONE);
            potentialEngineIssueAdapter.notifyDataSetChanged();
        } else if (issue.getIssueType().equals(CarIssue.SERVICE_USER)){
            customIssueList.remove(issue);
            if (customIssueList.isEmpty())
                customIssueListRecyclerView.setVisibility(View.GONE);
            customIssueAdapter.notifyDataSetChanged();
        } else {
            carIssueList.remove(issue);
            carIssuesAdapter.notifyDataSetChanged();
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
        Intent intent = new Intent(getActivity(), AddCarActivity.class);
        startActivityForResult(intent, MainActivity.RC_ADD_CAR);
    }

    @OnClick(R.id.offline_try_again)
    public void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        presenter.onOfflineTryAgainClicked();
    }

    @Override
    public void addCustomIssue(CarIssue issue) {
        Log.d(TAG,"addCustomIssue()");
        customIssueListRecyclerView.setVisibility(View.VISIBLE);
        customIssueList.add(issue);
        customIssueAdapter.notifyDataSetChanged();
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
        if (!swipeRefreshLayout.isRefreshing()) {
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
        }else{
            swipeRefreshLayout.setRefreshing(false);
        }
    }


    @Override
    public void displayCarIssues(List<CarIssue> carIssues) {
        Log.d(TAG,"displayCarIssues() size(): "+carIssues.size());

        hasBeenPopulated = true;
        this.carIssueList.clear();
        this.carIssueList.addAll(carIssues);

        carIssuesAdapter = new CurrentServicesAdapter(this.carIssueList,this);
        carIssueListView.setLayoutManager(new LinearLayoutManager(
                getActivity().getApplicationContext()));
        carIssueListView.setAdapter(carIssuesAdapter);

        routineListHolder.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayCustomIssues(List<CarIssue> customIssueList) {
        Log.d(TAG,"displayCustomIssues() size(): "+customIssueList.size());

        hasBeenPopulated = true;
        this.customIssueList.clear();
        this.customIssueList.addAll(customIssueList);
        customIssueAdapter = new CurrentServicesAdapter(this.customIssueList,this);
        customIssueListRecyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity().getApplicationContext()));
        customIssueListRecyclerView.setAdapter(customIssueAdapter);
        customIssueAdapter.notifyDataSetChanged();

        if(customIssueList.isEmpty()){
            customIssueListRecyclerView.setVisibility(View.GONE);
        }else{
            customIssueListRecyclerView.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void displayStoredEngineIssues(List<CarIssue> storedEngineIssues) {
        Log.d(TAG,"displayStoredEngineIssues() size(): "+storedEngineIssues.size());

        hasBeenPopulated = true;
        this.storedEngineIssueList.clear();
        this.storedEngineIssueList.addAll(storedEngineIssues);
        storedEngineIssuesAdapter = new CurrentServicesAdapter(this.storedEngineIssueList,this);
        engineListView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        engineListView.setAdapter(storedEngineIssuesAdapter);

        if(storedEngineIssues.isEmpty()){
            engineIssueHolder.setVisibility(View.GONE);
        }else{
            engineIssueHolder.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void displayPotentialEngineIssues(List<CarIssue> potentialEngineIssueList) {
        Log.d(TAG,"displayPotentialEngineIssues() size(): "+potentialEngineIssueList.size());

        hasBeenPopulated = true;
        this.potentialEngineIssuesList.clear();
        this.potentialEngineIssuesList.addAll(potentialEngineIssueList);
        potentialEngineIssueAdapter
                = new CurrentServicesAdapter(this.potentialEngineIssuesList,this);
        potentialListView.setLayoutManager(
                new LinearLayoutManager(getActivity().getApplicationContext()));
        potentialListView.setAdapter(potentialEngineIssueAdapter);

        if(potentialEngineIssueList.isEmpty()){
            potentialEngineList.setVisibility(View.GONE);
        }else{
            potentialEngineList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void displayRecalls(List<CarIssue> displayRecalls) {
        Log.d(TAG,"displayRecalls() size(): "+displayRecalls.size());

        hasBeenPopulated = true;
        this.recallList.clear();
        this.recallList.addAll(displayRecalls);
        recallAdapter = new CurrentServicesAdapter(this.recallList,this);
        recallListView.setLayoutManager(
                new LinearLayoutManager(getActivity().getApplicationContext()));
        recallListView.setAdapter(recallAdapter);

        if(displayRecalls.isEmpty()){
            recallListHolder.setVisibility(View.GONE);
        }else{
            recallListHolder.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void displayCalendar(CarIssue carIssue) {
        Log.d(TAG,"displayCalendar()");
        DatePickerDialog servicesDatePickerDialog = new ServicesDatePickerDialog(getContext()
                , Calendar.getInstance()
                , (DatePicker datePicker, int year, int month, int day) -> {

                     presenter.onServiceDoneDatePicked(carIssue,year,month,day);
        });
        servicesDatePickerDialog.setTitle(getString(R.string.service_date_picker_title));
        servicesDatePickerDialog.show();
    }

    @Override
    public void onServiceClicked(CarIssue carIssue) {
        Log.d(TAG,"onServiceClicked()");
        presenter.onServiceClicked(carIssue);

    }

    @Override
    public void startDisplayIssueActivity(CarIssue issue){
        if (getActivity() == null) return;
        ((MainActivityCallback)getActivity()).startDisplayIssueActivity(issue);
    }

    @Override
    public void onServiceDoneClicked(CarIssue carIssue) {
        Log.d(TAG,"onServiceDoneClicked()");
        presenter.onServiceMarkedAsDone(carIssue);
    }

    @Override
    public void onTentativeServiceClicked() {
        Log.d(TAG,"onTentativeServiceClicked()");
        ((MainActivityCallback)getActivity()).prepareAndStartTutorialSequence();
    }
}
