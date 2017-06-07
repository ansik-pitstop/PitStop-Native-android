package com.pitstop.ui.services;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.pitstop.R;
import com.pitstop.adapters.HistoryIssueGroupAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.UserAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetDoneServicesUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.utils.DateTimeFormatUtil;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;

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
    private List<CarIssue> addedIssues;

    private HistoryIssueGroupAdapter issueGroupAdapter;

    private LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues;
    ArrayList<String> headers;

    @Inject
    GetDoneServicesUseCase getDoneServicesUseCase;

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

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        component.injectUseCases(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, view);
        initUI();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        initUI();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && getView() != null){
            updateUI();
        }
    }

    private void addIssue(CarIssue issue){

        String dateHeader;
        if(issue.getDoneAt() == null || issue.getDoneAt().equals("")) {
            dateHeader = "";
        } else {
            String formattedDate = DateTimeFormatUtil.formatDateToHistoryFormat(issue.getDoneAt());
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
                if (!(DateTimeFormatUtil.getHistoryDateToCompare(issues.get(i).getDoneAt())
                        - DateTimeFormatUtil.getHistoryDateToCompare(issue.getDoneAt()) <= 0)) {
                    issues.add(i, issue);
                    break;
                }
                if (i == issueSize -1){
                    issues.add(issue);
                }
            }
        }

        sortedIssues.put(dateHeader, issues);
    }

    private void sortHeaders() {
        Collections.sort(headers, new Comparator<String>() {
            @Override
            public int compare(String left, String right) {
                Double leftYearPrecise = DateTimeFormatUtil.historyFormatToDouble(left);
                Double rightYearPrecise = DateTimeFormatUtil.historyFormatToDouble(right);
                if (rightYearPrecise < leftYearPrecise){
                    return -1;
                }
                else{
                    return 1;
                }
            }
        });
    }

    private void initUI(){
        addedIssues = new ArrayList<>();
        headers = new ArrayList<>();
        sortedIssues = new LinkedHashMap<>();
        issueGroupAdapter = new HistoryIssueGroupAdapter(getActivity(),sortedIssues,headers);
        issueGroup.setAdapter(issueGroupAdapter);

        updateUI();
    }

    private void updateUI(){

        getDoneServicesUseCase.execute(new GetDoneServicesUseCase.Callback() {
            @Override
            public void onGotDoneServices(List<CarIssue> doneServices) {
                if(doneServices.isEmpty()) {
                    messageCard.setVisibility(View.VISIBLE);
                }
                List<CarIssue> toAdd = new ArrayList<CarIssue>();
                for (CarIssue issue: doneServices){
                    if (addedIssues.indexOf(issue) < 0){
                        toAdd.add(issue);
                        addIssue(issue);
                    }
                }
                if (!toAdd.isEmpty()){
                    addedIssues.addAll(toAdd);
                    sortHeaders();
                    issueGroupAdapter.notifyDataSetChanged();
                }

                if(messageCard.getVisibility() == View.VISIBLE && !addedIssues.isEmpty()) {
                    messageCard.setVisibility(View.INVISIBLE);
                }

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

}
