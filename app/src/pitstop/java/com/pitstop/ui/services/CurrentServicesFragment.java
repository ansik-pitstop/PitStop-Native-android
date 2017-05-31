package com.pitstop.ui.services;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.adapters.CurrentServicesAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.UserAdapter;
import com.pitstop.interactors.GetCurrentServicesUseCase;
import com.pitstop.interactors.GetCurrentServicesUseCaseImpl;
import com.pitstop.models.CarIssue;
import com.pitstop.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class CurrentServicesFragment extends Fragment{

    public static final String TAG = CurrentServicesFragment.class.getSimpleName();

    @BindView(R.id.car_issues_list)
    protected RecyclerView carIssueListView;
    private CurrentServicesAdapter carIssuesAdapter;

    private UserAdapter userAdapter;
    private LocalCarIssueAdapter carIssueLocalStore;
    private NetworkHelper networkHelper;
    private List<CarIssue> carIssueList = new ArrayList<>();

    public static CurrentServicesFragment newInstance(){
        CurrentServicesFragment fragment = new CurrentServicesFragment();
        return fragment;
    }

    public CurrentServicesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkHelper = new NetworkHelper(getActivity().getApplicationContext());
        carIssueLocalStore = new LocalCarIssueAdapter(getActivity().getApplicationContext());
        userAdapter = new UserAdapter(getActivity().getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_new_services, container, false);
        ButterKnife.bind(this, view);
        initUI();
        updateUI();

        return view;
    }

    private void initUI(){
        carIssuesAdapter = new CurrentServicesAdapter(carIssueList, this.getActivity());
        carIssueListView.setLayoutManager(new LinearLayoutManager(getContext()));
        carIssueListView.setAdapter(carIssuesAdapter);
    }

    private void updateUI(){
        GetCurrentServicesUseCase getCurrentServices
                = new GetCurrentServicesUseCaseImpl(userAdapter,carIssueLocalStore,networkHelper);

        getCurrentServices.execute(new GetCurrentServicesUseCase.Callback() {
            @Override
            public void onGotCurrentServices(List<CarIssue> currentServices) {
                carIssueList.clear();
                carIssueList.addAll(currentServices);
                carIssuesAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError() {
                Toast.makeText(getActivity(),
                        "Error retrieving car details", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
