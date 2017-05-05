package com.pitstop.ui.services;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.ui.mainFragments.CarDataFragment;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HistoryServiceFragment extends CarDataFragment implements SubServiceFragment {

    public static final String ISSUE_FROM_HISTORY = "IssueFromHistory";

    //private CustomAdapter customAdapter;

    private RecyclerView issuesList;

    @BindView(R.id.message_card)
    protected CardView messageCard;

    @BindView(R.id.issue_expandable_list)
    protected ExpandableListView issueGroup;

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    private Car dashboardCar;

    private LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues = new LinkedHashMap<>();
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

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        ButterKnife.bind(this, view);

        dashboardCar = getCurrentCar();
        updateIssueGroupView();

        return view;
    }

    private void updateIssueGroupView(){
        CarIssue[] doneIssues = dashboardCar.getDoneIssues().toArray(new CarIssue[dashboardCar.getDoneIssues().size()]);

        Arrays.sort(doneIssues, new Comparator<CarIssue>() {
            @Override
            public int compare(CarIssue lhs, CarIssue rhs) {
                return getDateToCompare(rhs.getDoneAt()) - getDateToCompare(lhs.getDoneAt());
            }
        });

        for(CarIssue issue : doneIssues) {  // sort dates into groups
            String dateHeader;
            if(issue.getDoneAt() == null || issue.getDoneAt().equals("")) {
                dateHeader = "";
            } else {
                String formattedDate = formatDate(issue.getDoneAt());
                dateHeader = formattedDate.substring(0, 3) + " " + formattedDate.substring(9, 13);
            }
            ArrayList<CarIssue> issues = sortedIssues.get(dateHeader);
            if(issues == null) {
                headers.add(dateHeader);
                issues = new ArrayList<>();
            }
            issues.add(issue);
            sortedIssues.put(dateHeader, issues);
        }

        ArrayList<CarIssue> doneIssuesList = new ArrayList<>(Arrays.asList(doneIssues));

        if(doneIssuesList.isEmpty()) {
            messageCard.setVisibility(View.VISIBLE);
        }

        issueGroup.setAdapter(new IssueGroupAdapter(sortedIssues, headers));
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

    private String formatDate(String rawDate) { // parse date that looks like "2009-07-28T20:12:29.533Z" to "Jul. 28, 2009"
        String[] splittedDate = rawDate.split("-");
        String[] months = new String[] {"null", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        splittedDate[2] = splittedDate[2].substring(0, 2);

        return months[Integer.parseInt(splittedDate[1])] + ". " + splittedDate[2] + ", " + splittedDate[0];
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

    @Override
    public void onMainServiceTabReopened() {
        dashboardCar = getCurrentCar();
        updateIssueGroupView();
    }

    private class IssueGroupAdapter extends BaseExpandableListAdapter {

        LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues;
        ArrayList<String> headers;
        LayoutInflater inflater;

        public IssueGroupAdapter(LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues, ArrayList<String> headers) {
            this.sortedIssues = sortedIssues;
            this.headers = headers;
            inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getGroupCount() {
            return sortedIssues.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return sortedIssues.get(headers.get(groupPosition)).size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return sortedIssues.get(headers.get(groupPosition));
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return sortedIssues.get(headers.get(groupPosition)).get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return groupPosition * childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = inflater.inflate(R.layout.list_item_issue_group, parent, false);
            }

            String header = headers.get(groupPosition);
            ((TextView) convertView.findViewById(R.id.issue_group_header)).setText(header.equals("") ? "No date" : header);

            return convertView;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = inflater.inflate(R.layout.list_item_issue, parent, false);
            }

            final CarIssue issue = (CarIssue) getChild(groupPosition, childPosition);

            TextView title = (TextView)convertView.findViewById(R.id.title);
            TextView desc = (TextView)convertView.findViewById(R.id.description);
            TextView date = (TextView)convertView.findViewById(R.id.date);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.image_icon);

            desc.setText(issue.getDescription());
            if(issue.getDoneAt() == null || issue.getDoneAt().equals("null")) {
                date.setText("Done");
            } else {
                date.setText(String.format("Done on %s", formatDate(issue.getDoneAt())));
            }

            if(issue.getIssueType().equals(CarIssue.RECALL)) {
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_error_red_600_24dp));
            } else if(issue.getIssueType().equals(CarIssue.DTC)) {
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.car_engine_red));
            } else if(issue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.car_engine_yellow));
            } else {
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning_amber_300_24dp));
            }

            title.setText(String.format("%s %s", issue.getAction(), issue.getItem()));

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /*mixpanelHelper.trackButtonTapped(issue.getItem(), MixpanelHelper.SERVICE_HISTORY_VIEW);
                    Intent intent = new Intent(CarHistoryActivity.this, IssueDetailsActivity.class);
                    intent.putExtra(MainActivity.CAR_EXTRA, dashboardCar);
                    intent.putExtra(MainActivity.CAR_ISSUE_EXTRA, issue);
                    intent.putExtra(ISSUE_FROM_HISTORY, true);
                    startActivity(intent);*/
                    //TODO launch issueDetails
                }
            });

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
}
