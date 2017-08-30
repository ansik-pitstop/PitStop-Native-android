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
import android.widget.Toast;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.adapters.CurrentServicesAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.interactors.other.MarkServiceDoneUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
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

    public static final String TAG = CurrentServicesFragmentOld.class.getSimpleName();
    public static final EventSource EVENT_SOURCE
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

    private List<CarIssue> carIssueList = new ArrayList<>();
    private List<CarIssue> customIssueList = new ArrayList<>();
    private List<CarIssue> engineIssueList = new ArrayList<>();
    private List<CarIssue> potentialEngineIssues = new ArrayList<>();
    private List<CarIssue> recallList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_new_services, container, false);
        ButterKnife.bind(this, view);

        //setNoUpdateOnEventTypes(ignoredEvents);
        //initUI();

        return view;
    }

    @OnClick(R.id.service_launch_custom)
    public void onCustomServiceButtonClicked(){
        //presenter.onCustomServiceButtonClicked()
        Intent intent =  new Intent(getActivity(), CustomServiceActivity.class);
        intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA,false);
        startActivity(intent);
    }


    @Override
    public void displayCarIssues(List<CarIssue> carIssues) {
        carIssuesAdapter = new CurrentServicesAdapter(car,carIssueList
                ,(MainActivityCallback)activity, getContext(),useCaseComponent.markServiceDoneUseCase()
                ,notifier);
        carIssueListView.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext()));
        carIssueListView.setAdapter(carIssuesAdapter);
        carIssuesAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if(carIssueList.isEmpty()){
                    routineListHolder.setVisibility(View.GONE);
                }else{
                    routineListHolder.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void displayCustomIssues(List<CarIssue> customIssueList) {
        customIssueAdapter = new CurrentServicesAdapter(car,customIssueList,(MainActivityCallback)activity, getContext(),useCaseComponent.markServiceDoneUseCase()
                ,notifier);
        customIssueListRecyclerView.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext()));
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

    }

    @Override
    public void displayPotentialEngineIssues(List<CarIssue> potentialEngineIssueList) {

    }

    @Override
    public void displayRecalls(List<CarIssue> displayRecalls) {

    }

    @Override
    public void onServiceClicked(CarIssue carIssue) {
        new MixpanelHelper((GlobalApplication) getContext().getApplicationContext())
                .trackButtonTapped(carIssue.getItem(), MixpanelHelper.DASHBOARD_VIEW);

        ((MainActivityCallback)getActivity()).startDisplayIssueActivity(carIssue);
    }

    @Override
    public void onServiceDoneClicked(CarIssue carIssue) {
        DatePickerDialog servicesDatePickerDialog = new ServicesDatePickerDialog(context
                , Calendar.getInstance(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {

                carIssue.setYear(year);
                carIssue.setMonth(month);
                carIssue.setDay(day);

                //When the date is set, update issue to done on that date
                markServiceDoneUseCase.execute(carIssue, new MarkServiceDoneUseCase.Callback() {
                    @Override
                    public void onServiceMarkedAsDone() {
                        Toast.makeText(context,"Successfully marked service as done"
                                ,Toast.LENGTH_LONG);
                        carIssues.remove(carIssue);
                        notifyDataSetChanged();
                        EventType event = new EventTypeImpl(EventType
                                .EVENT_SERVICES_HISTORY);
                        EventSource source = new EventSourceImpl(EventSource
                                .SOURCE_SERVICES_CURRENT);
                        notifier.notifyCarDataChanged(event,source);
                    }

                    @Override
                    public void onError(RequestError error) {
                    }
                });
            }
        });
        servicesDatePickerDialog.setTitle("Select when you completed this service.");
        servicesDatePickerDialog.show();
    }

    @Override
    public void onTentativeServiceClicked() {

    }
}
