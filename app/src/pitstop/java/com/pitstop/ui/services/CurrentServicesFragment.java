package com.pitstop.ui.services;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.adapters.CurrentServicesAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetCurrentServicesUseCase;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.interactors.MarkServiceDoneUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.mainFragments.CarDataChangedNotifier;
import com.pitstop.ui.mainFragments.CarDataFragment;
import com.pitstop.ui.main_activity.MainActivityCallback;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.pitstop.EventBus.EventType.EVENT_MILEAGE;
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

    private CurrentServicesAdapter carIssuesAdapter;

    private List<CarIssue> carIssueList = new ArrayList<>();

    private final EventType[] ignoredEvents = {
            new EventTypeImpl(EVENT_SERVICES_HISTORY),
            new EventTypeImpl(EVENT_MILEAGE)
    };

    private boolean uiInitialized = false;

    @Inject
    GetUserCarUseCase getUserCarUseCase;

    @Inject
    GetCurrentServicesUseCase getCurrentServices;

    @Inject
    MarkServiceDoneUseCase markServiceDoneUseCase;

    public static CurrentServicesFragment newInstance(){
        CurrentServicesFragment fragment = new CurrentServicesFragment();
        return fragment;
    }

    public CurrentServicesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext().getApplicationContext()))
                .build();

        component.injectUseCases(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_new_services, container, false);
        ButterKnife.bind(this, view);
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
        carIssueListView.setVisibility(View.INVISIBLE);

        getCurrentServices.execute(new GetCurrentServicesUseCase.Callback() {
            @Override
            public void onGotCurrentServices(List<CarIssue> currentServices) {
                carIssueList.clear();
                carIssueList.addAll(currentServices);
                carIssuesAdapter.notifyDataSetChanged();

                mLoadingSpinner.setVisibility(View.INVISIBLE);
                carIssueListView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError() {
                mLoadingSpinner.setVisibility(View.INVISIBLE);
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
        getUserCarUseCase.execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                carIssuesAdapter = new CurrentServicesAdapter(car,carIssueList
                        ,(MainActivityCallback)activity, getContext(),markServiceDoneUseCase
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
            public void onError() {
                uiInitialized = false;
            }
        });

    }
}
