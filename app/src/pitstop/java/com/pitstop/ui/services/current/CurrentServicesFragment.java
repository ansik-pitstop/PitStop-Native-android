package com.pitstop.ui.services.current;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.R;
import com.pitstop.adapters.CurrentServicesAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.main_activity.MainActivityCallback;
import com.pitstop.ui.services.ServicesDatePickerDialog;
import com.pitstop.ui.services.custom_service.CustomServiceActivity;
import com.pitstop.utils.MixpanelHelper;

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
    public final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_SERVICES_CURRENT);

    @BindView(R.id.car_issues_list)
    protected RecyclerView carIssueListView;

    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingSpinner;

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

    private CurrentServicesAdapter carIssuesAdapter;
    private CurrentServicesAdapter customIssueAdapter;
    private CurrentServicesAdapter engineIssueAdapter;
    private CurrentServicesAdapter potentialEngineIssueAdapter;
    private CurrentServicesAdapter recallAdapter;

    private CurrentServicesPresenter presenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_new_services, container, false);
        ButterKnife.bind(this, view);

        //setNoUpdateOnEventTypes(ignoredEvents);
        //initUI();
        if (presenter == null){
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent
                    .builder().contextModule(new ContextModule(getContext())).build();
            presenter = new CurrentServicesPresenter(useCaseComponent);
        }
        presenter.subscribe(this);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            presenter.onUpdateNeeded();
        });

        return view;
    }

    @OnClick(R.id.service_launch_custom)
    public void onCustomServiceButtonClicked(){
        presenter.onCustomServiceButtonClicked();
    }

    @Override
    public void startCustomServiceActivity(){
        Intent intent =  new Intent(getActivity(), CustomServiceActivity.class);
        intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA,false);
        startActivity(intent);
    }

    @Override
    public void removeCarIssue(CarIssue issue) {
        if (issue.getIssueType().equals(CarIssue.RECALL)) {
            recallAdapter.removeIssue(issue);

        } else if (issue.getIssueType().equals(CarIssue.DTC)) {
            engineIssueAdapter.removeIssue(issue);

        } else if (issue.getIssueType().equals(CarIssue.PENDING_DTC)) {
            potentialEngineIssueAdapter.removeIssue(issue);
        } else {
            customIssueAdapter.removeIssue(issue);
        }
    }

    @Override
    public void onDestroyView() {
        presenter.unsubscribe();
        super.onDestroyView();
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void hideLoading() {

    }


    @Override
    public void displayCarIssues(List<CarIssue> carIssues) {
        carIssuesAdapter = new CurrentServicesAdapter(carIssues,this);
        carIssueListView.setLayoutManager(new LinearLayoutManager(
                getActivity().getApplicationContext()));
        carIssueListView.setAdapter(carIssuesAdapter);
        carIssuesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(carIssues.isEmpty()){
                    routineListHolder.setVisibility(View.GONE);
                }else{
                    routineListHolder.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void displayCustomIssues(List<CarIssue> customIssueList) {
        customIssueAdapter = new CurrentServicesAdapter(customIssueList,this);
        customIssueListRecyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity().getApplicationContext()));
        customIssueListRecyclerView.setAdapter(customIssueAdapter);
        customIssueAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(customIssueList.isEmpty()){
                    customIssueListRecyclerView.setVisibility(View.GONE);
                }else{
                    customIssueListRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void displayStoredEngineIssues(List<CarIssue> storedEngineIssues) {
        engineIssueAdapter = new CurrentServicesAdapter(storedEngineIssues,this);
        engineListView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        engineListView.setAdapter(engineIssueAdapter);
        engineIssueAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(storedEngineIssues.isEmpty()){
                    engineIssueHolder.setVisibility(View.GONE);
                }else{
                    engineIssueHolder.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void displayPotentialEngineIssues(List<CarIssue> potentialEngineIssueList) {
        potentialEngineIssueAdapter
                = new CurrentServicesAdapter(potentialEngineIssueList,this);
        potentialListView.setLayoutManager(
                new LinearLayoutManager(getActivity().getApplicationContext()));
        potentialListView.setAdapter(potentialEngineIssueAdapter);
        potentialEngineIssueAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(potentialEngineIssueList.isEmpty()){
                    potentialEngineList.setVisibility(View.GONE);
                }else{
                    potentialEngineList.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void displayRecalls(List<CarIssue> displayRecalls) {
        recallAdapter = new CurrentServicesAdapter(displayRecalls,this);
        recallListView.setLayoutManager(
                new LinearLayoutManager(getActivity().getApplicationContext()));
        recallListView.setAdapter(recallAdapter);
        recallAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(displayRecalls.isEmpty()){
                    recallListHolder.setVisibility(View.GONE);
                }else{
                    recallListHolder.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void displayCalendar(CarIssue carIssue) {
        DatePickerDialog servicesDatePickerDialog = new ServicesDatePickerDialog(getContext()
                , Calendar.getInstance()
                , (DatePicker datePicker, int year, int month, int day) -> {

                     presenter.onServiceDoneDatePicked(carIssue,year,month,day);
        });
        servicesDatePickerDialog.setTitle("Select when you completed this service.");
        servicesDatePickerDialog.show();
    }

    @Override
    public void onServiceClicked(CarIssue carIssue) {
        new MixpanelHelper((GlobalApplication) getContext().getApplicationContext())
                .trackButtonTapped(carIssue.getItem(), MixpanelHelper.DASHBOARD_VIEW);
        ((MainActivityCallback)getActivity()).startDisplayIssueActivity(carIssue);
    }

    @Override
    public void onServiceDoneClicked(CarIssue carIssue) {
        presenter.onServiceMarkedAsDone(carIssue);
    }

    @Override
    public void onTentativeServiceClicked() {
        ((MainActivityCallback)getActivity()).prepareAndStartTutorialSequence();
    }
}
