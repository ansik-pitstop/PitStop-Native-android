package com.pitstop.ui.services;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.UserAdapter;
import com.pitstop.interactors.GetDoneServicesUseCase;
import com.pitstop.interactors.GetDoneServicesUseCaseImpl;
import com.pitstop.models.CarIssue;
import com.pitstop.utils.DateTimeFormatUtil;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistoryServiceFragment extends Fragment {

    public static final String ISSUE_FROM_HISTORY = "IssueFromHistory";

    private RecyclerView issuesList;

    @BindView(R.id.message_card)
    protected CardView messageCard;

    @BindView(R.id.issue_expandable_list)
    protected ExpandableListView issueGroup;

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private UserAdapter userAdapter;
    private NetworkHelper networkHelper;
    private LocalCarIssueAdapter localCarIssueAdapter;

    private HistoryIssueGroupAdapter issueGroupAdapter;

    private LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues;
    ArrayList<String> headers = new ArrayList<>();

    public HistoryServiceFragment() {
        // Required empty public constructor
    }

    public static HistoryServiceFragment newInstance() {
        HistoryServiceFragment fragment = new HistoryServiceFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        application = (GlobalApplication) getActivity().getApplicationContext();
        mixpanelHelper = new MixpanelHelper((GlobalApplication) getActivity().getApplicationContext());
        networkHelper = new NetworkHelper(application);
        userAdapter = new UserAdapter(application);
        localCarIssueAdapter = new LocalCarIssueAdapter(application);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, view);

        initUI();

        //This must be called so that UI elements are set for SubService
        super.onCreateView(inflater,container,savedInstanceState);

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && getView() != null){
            initUI();
        }
    }

    private void addIssue(CarIssue issue){

        String dateHeader;
        if(issue.getDoneAt() == null || issue.getDoneAt().equals("")) {
            dateHeader = "";
        } else {
            String formattedDate = DateTimeFormatUtil.formatDateHistory(issue.getDoneAt());
            dateHeader = formattedDate.substring(0, 3) + " " + formattedDate.substring(9, 13);
        }

        ArrayList<CarIssue> issues = sortedIssues.get(dateHeader);

        //Check if header already exists
        if(issues == null) {
            headers.add(dateHeader);
            issues = new ArrayList<>();
            issues.add(issue);
        }
        else {
            //Add issue to appropriate position within list, in order of date
            int issueSize = issues.size();
            for (int i = 0; i < issueSize; i++) {
                if (!(getDateToCompare(issues.get(i).getDoneAt())
                        - getDateToCompare(issue.getDoneAt()) <= 0)) {
                    issues.add(i, issue);
                }
                if (i == issueSize -1){
                    issues.add(issue);
                }
            }
        }

        sortedIssues.put(dateHeader, issues);
    }

    private void initUI(){
        sortedIssues = new LinkedHashMap<>();
        issueGroupAdapter = new HistoryIssueGroupAdapter(getActivity(),sortedIssues,headers);
        issueGroup.setAdapter(issueGroupAdapter);

        updateUI();
    }

    private void updateUI(){

        sortedIssues.clear();

        GetDoneServicesUseCase getDoneServicesUseCase
                = new GetDoneServicesUseCaseImpl(userAdapter,localCarIssueAdapter,networkHelper);
        getDoneServicesUseCase.execute(new GetDoneServicesUseCase.Callback() {
            @Override
            public void onGotDoneServices(List<CarIssue> doneServices) {
                if(doneServices.isEmpty()) {
                    messageCard.setVisibility(View.VISIBLE);
                }

                for (CarIssue issue: doneServices){
                    addIssue(issue);
                }
                issueGroupAdapter.notifyDataSetChanged();

            }

            @Override
            public void onError() {

            }
        });

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mixpanelHelper.trackViewAppeared(MixpanelHelper.SERVICE_HISTORY_VIEW);
    }

    @Override
    public void onPause() {
        super.onPause();
        application.getMixpanelAPI().flush();
    }

    private int getDateToCompare(String rawDate) {
        if(rawDate == null || rawDate.isEmpty() || rawDate.equals("null")) {
            return 0;
        }

        String[] splittedDate = rawDate.split("-");
        splittedDate[2] = splittedDate[2].substring(0, 2);

        return Integer.parseInt(splittedDate[2])
                + Integer.parseInt(splittedDate[1]) * 30
                + Integer.parseInt(splittedDate[0]) * 365;
    }

    public void onServiceDone(CarIssue issue){

        //Set to invisible since it is about to be no longer empty
        if(sortedIssues.isEmpty()) {
            messageCard.setVisibility(View.INVISIBLE);
        }

        addIssue(issue);
        issueGroupAdapter.notifyDataSetChanged();
        Log.d("HistoryServiceFragment","onServiceDone() called.");
    }

}
