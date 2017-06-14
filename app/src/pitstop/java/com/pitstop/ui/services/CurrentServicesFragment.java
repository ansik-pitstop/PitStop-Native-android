package com.pitstop.ui.services;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.pitstop.EventBus.CarDataChangedEvent;
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

/**
 * A simple {@link Fragment} subclass.
 */
public class CurrentServicesFragment extends CarDataFragment {

    public static final String TAG = CurrentServicesFragment.class.getSimpleName();

    @BindView(R.id.car_issues_list)
    protected RecyclerView carIssueListView;

    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingSpinner;

    private CurrentServicesAdapter carIssuesAdapter;

    private List<CarIssue> carIssueList = new ArrayList<>();

    private final String[] ignoredEvents = {EVENT_SERVICES_HISTORY,EVENT_MILEAGE};

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
    public void onCarDataChangedEvent(CarDataChangedEvent event){

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
                Toast.makeText(getActivity(),
                        "Error retrieving information", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Call whenever you want to completely new UI objects
    private void initUI(){
        if (getActivity() == null){ return; }
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
                updateUI();
            }

            @Override
            public void onError() {
                Toast.makeText(getActivity(),
                        "Error retrieving information", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
