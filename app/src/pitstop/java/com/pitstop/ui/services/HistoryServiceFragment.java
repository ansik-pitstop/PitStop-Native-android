package com.pitstop.ui.services;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.adapters.HistoryIssueGroupAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetDoneServicesUseCase;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.mainFragments.CarDataFragment;
import com.pitstop.utils.DateTimeFormatUtil;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistoryServiceFragment extends CarDataFragment {

    public static final String ISSUE_FROM_HISTORY = "IssueFromHistory";
    public static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_SERVICES_HISTORY);

    private RecyclerView issuesList;

    @BindView(R.id.loading_spinner)
    ProgressBar mLoadingSpinner;

    @BindView(R.id.message_card)
    protected CardView messageCard;

    @BindView(R.id.issue_expandable_list)
    protected ExpandableListView issueGroup;

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;
    private final EventType[] ignoredEvents = {new EventTypeImpl(EventType.EVENT_MILEAGE)};

    private HistoryIssueGroupAdapter issueGroupAdapter;

    @Inject
    GetDoneServicesUseCase getDoneServicesUseCase;

    @Inject
    GetUserCarUseCase getUserCarUseCase;

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
        setNoUpdateOnEventTypes(ignoredEvents);
        updateUI();
        return view;
    }

    private void addIssue(LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues
            ,List<String> headers, CarIssue issue){

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
                        - DateTimeFormatUtil.getHistoryDateToCompare(issue.getDoneAt()) >= 0)) {
                    issues.add(i, issue);
                    break;
                }
                if (i == issueSize -1){
                    issues.add(issue);
                    break;
                }
            }
        }

        sortedIssues.put(dateHeader, issues);
    }

    @Override
    public void updateUI(){
        mLoadingSpinner.setVisibility(View.VISIBLE);

        getDoneServicesUseCase.execute(new GetDoneServicesUseCase.Callback() {
            @Override
            public void onGotDoneServices(List<CarIssue> doneServices) {
                if(doneServices.isEmpty()) {
                    messageCard.setVisibility(View.VISIBLE);
                }

                LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues = new LinkedHashMap<>();
                ArrayList<String> headers = new ArrayList<>();

                CarIssue[] doneServicesOrdered = new CarIssue[doneServices.size()];
                doneServices.toArray(doneServicesOrdered);
                Arrays.sort(doneServicesOrdered, new Comparator<CarIssue>() {
                    @Override
                    public int compare(CarIssue lhs, CarIssue rhs) {
                        return DateTimeFormatUtil.getHistoryDateToCompare(rhs.getDoneAt())
                                - DateTimeFormatUtil.getHistoryDateToCompare(lhs.getDoneAt());
                    }
                });

                for (CarIssue issue: doneServicesOrdered){
                    addIssue(sortedIssues,headers,issue);
                }

                if(messageCard.getVisibility() == View.VISIBLE && !doneServices.isEmpty()) {
                    messageCard.setVisibility(View.INVISIBLE);
                }

                issueGroupAdapter = new HistoryIssueGroupAdapter(getActivity(),sortedIssues,headers);
                issueGroup.setAdapter(issueGroupAdapter);

                mLoadingSpinner.setVisibility(View.INVISIBLE);

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
