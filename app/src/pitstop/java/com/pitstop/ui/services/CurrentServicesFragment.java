package com.pitstop.ui.services;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.adapters.CurrentServicesAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetCurrentServicesUseCase;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;
import com.pitstop.ui.mainFragments.CarDataChangedNotifier;
import com.pitstop.ui.mainFragments.CarDataFragment;
import com.pitstop.ui.main_activity.MainActivityCallback;
import com.pitstop.ui.services.custom_service.CustomServiceActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.pitstop.EventBus.EventType.EVENT_SERVICES_HISTORY;

/**
 * A simple {@link Fragment} subclass.
 */
public class CurrentServicesFragment extends CarDataFragment {

    public static final String TAG = CurrentServicesFragment.class.getSimpleName();
    public static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_SERVICES_CURRENT);

    @BindView(R.id.car_issues_list)
    protected RecyclerView carIssueListView;

    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingSpinner;

   /* @BindView(R.id.custom_loading)
    ProgressBar customLoading;*/

    @BindView(R.id.service_launch_custom)
    LinearLayout customSeerviceButton;

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

    private boolean isUpdating = false;

    private final EventType[] ignoredEvents = {
            new EventTypeImpl(EVENT_SERVICES_HISTORY),
    };

    private boolean uiInitialized = false;

    private UseCaseComponent useCaseComponent;

    public static CurrentServicesFragment newInstance(){
        CurrentServicesFragment fragment = new CurrentServicesFragment();
        return fragment;
    }

    public CurrentServicesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext().getApplicationContext()))
                .build();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_new_services, container, false);
        ButterKnife.bind(this, view);
        customSeerviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =  new Intent(getActivity(), CustomServiceActivity.class);
                intent.putExtra(CustomServiceActivity.HISTORICAL_EXTRA,false);
                startActivity(intent);
            }
        });

        setNoUpdateOnEventTypes(ignoredEvents);
        initUI();

        return view;
    }

    @Override
    public void updateUI(){
        Log.d(TAG,"Update UI Called()");

        if (isUpdating) return;

        //Create ui from scratch
        if (!uiInitialized){
            initUI();
            return;
        }
        isUpdating = true;

        if (!swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setEnabled(false);
            mLoadingSpinner.setVisibility(View.VISIBLE);
        }

        useCaseComponent.getCurrentServicesUseCase().execute(new GetCurrentServicesUseCase.Callback() {
            @Override
            public void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> custom) {
                engineIssueList.clear();
                potentialEngineIssues.clear();
                recallList.clear();
                carIssueList.clear();
                customIssueList.clear();

                for(CarIssue c:currentServices){
                    if(c.getIssueType().equals(CarIssue.DTC)){
                        engineIssueList.add(c);
                    }else if(c.getIssueType().equals(CarIssue.PENDING_DTC)){
                        potentialEngineIssues.add(c);
                    }else if(c.getIssueType().equals(CarIssue.RECALL)){
                        recallList.add(c);
                    }else{
                        carIssueList.add(c);
                    }
                }
                customIssueList.addAll(custom);

                carIssuesAdapter.notifyDataSetChanged();
                customIssueAdapter.notifyDataSetChanged();
                engineIssueAdapter.notifyDataSetChanged();
                potentialEngineIssueAdapter.notifyDataSetChanged();
                recallAdapter.notifyDataSetChanged();

                if (!swipeRefreshLayout.isRefreshing()){
                    mLoadingSpinner.setVisibility(View.INVISIBLE);
                    swipeRefreshLayout.setEnabled(true);
                }else{
                    swipeRefreshLayout.setRefreshing(false);
                }
                isUpdating = false;
            }

            @Override
            public void onError(RequestError error) {
                isUpdating = false;
                if (!swipeRefreshLayout.isRefreshing()){
                    mLoadingSpinner.setVisibility(View.INVISIBLE);
                    swipeRefreshLayout.setEnabled(true);
                }else{
                    swipeRefreshLayout.setRefreshing(false);
                }
                //customLoading.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    //Call whenever you want to completely new UI objects
    private void initUI(){
        Log.d(TAG,"initUI() called.");
        final Activity activity = this.getActivity();
        final CarDataChangedNotifier notifier = this;

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (isUpdating) swipeRefreshLayout.setRefreshing(false);
                else updateUI();
            }
        });

        useCaseComponent.getUserCarUseCase().execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {

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


                engineIssueAdapter = new CurrentServicesAdapter(car,engineIssueList
                        ,(MainActivityCallback)activity, getContext(),useCaseComponent.markServiceDoneUseCase()
                        ,notifier);
                engineListView.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext()));
                engineListView.setAdapter(engineIssueAdapter);
                engineIssueAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
                        if(engineIssueList.isEmpty()){
                            engineIssueHolder.setVisibility(View.GONE);
                        }else{
                            engineIssueHolder.setVisibility(View.VISIBLE);
                        }
                    }
                });

                potentialEngineIssueAdapter = new CurrentServicesAdapter(car,potentialEngineIssues
                        ,(MainActivityCallback)activity, getContext(),useCaseComponent.markServiceDoneUseCase()
                        ,notifier);
                potentialListView.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext()));
                potentialListView.setAdapter(potentialEngineIssueAdapter);
                potentialEngineIssueAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
                        if(potentialEngineIssues.isEmpty()){
                            potentialEngineList.setVisibility(View.GONE);
                        }else{
                            potentialEngineList.setVisibility(View.VISIBLE);
                        }
                    }
                });


                recallAdapter = new CurrentServicesAdapter(car,recallList
                        ,(MainActivityCallback)activity, getContext(),useCaseComponent.markServiceDoneUseCase()
                        ,notifier);
                recallListView.setLayoutManager(new LinearLayoutManager(activity.getApplicationContext()));
                recallListView.setAdapter(recallAdapter);
                recallAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
                        if(recallList.isEmpty()){
                            recallListHolder.setVisibility(View.GONE);
                        }else{
                            recallListHolder.setVisibility(View.VISIBLE);
                        }
                    }
                });


                uiInitialized = true;
                updateUI();
            }

            @Override
            public void onNoCarSet() {
                uiInitialized = false;
            }

            @Override
            public void onError(RequestError error) {
                uiInitialized = false;
            }
        });

    }
}
