package com.pitstop.ui.services;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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



    private CurrentServicesAdapter carIssuesAdapter;

    private CurrentServicesAdapter customIssueAdapter;

    private List<CarIssue> carIssueList = new ArrayList<>();

    private List<CarIssue> customIssueList = new ArrayList<>();



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

        //Create ui from scratch
        if (!uiInitialized){
            initUI();
            return;
        }

        mLoadingSpinner.setVisibility(View.VISIBLE);
        //customLoading.setVisibility(View.VISIBLE);
        carIssueListView.setVisibility(View.INVISIBLE);
        customIssueListRecyclerView.setVisibility(View.GONE);

        useCaseComponent.getCurrentServicesUseCase().execute(new GetCurrentServicesUseCase.Callback() {
            @Override
            public void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> custom) {
                if(custom.isEmpty()){
                    customIssueListRecyclerView.setVisibility(View.GONE);
                }else{
                    customIssueListRecyclerView.setVisibility(View.VISIBLE);
                }

                carIssueList.clear();
                carIssueList.addAll(currentServices);
                carIssuesAdapter.notifyDataSetChanged();

                customIssueList.clear();
                customIssueList.addAll(custom);
                customIssueAdapter.notifyDataSetChanged();

                mLoadingSpinner.setVisibility(View.INVISIBLE);
               // customLoading.setVisibility(View.INVISIBLE);
                carIssueListView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(RequestError error) {
                mLoadingSpinner.setVisibility(View.INVISIBLE);
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
