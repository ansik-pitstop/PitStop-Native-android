package com.pitstop.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.utils.DateTimeFormatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Karol Zdebel on 6/1/2017.
 */
public class HistoryIssueGroupAdapter extends BaseExpandableListAdapter {

    private LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues;
    private ArrayList<String> headers; //sorted by month
    private List<CarIssue> doneServices;
    private List<CarIssue> doneServicesBeforeChange = new ArrayList<>();

    public HistoryIssueGroupAdapter(List<CarIssue> doneServices) {
        this.doneServicesBeforeChange.addAll(doneServices);
        this.doneServices = doneServices;
        sortedIssues = new LinkedHashMap<>();
        headers = new ArrayList<>();
        setSortedIssues(doneServices);
    }

    @Override
    public void notifyDataSetChanged() {
        //Check if removal or addition
        if (doneServices.size() < doneServicesBeforeChange.size()){
            sortedIssues.clear();
            headers.clear();
            setSortedIssues(doneServices);
        }
        else{
            for (CarIssue d: doneServices){
                if (!doneServicesBeforeChange.contains(d)){
                    addIssue(d);
                }
            }
        }
        doneServicesBeforeChange.clear();
        doneServicesBeforeChange.addAll(doneServices);
        super.notifyDataSetChanged();
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

    private void setSortedIssues(List<CarIssue> doneServices){


        CarIssue[] doneServicesOrdered = new CarIssue[doneServices.size()];
        doneServices.toArray(doneServicesOrdered);
        Arrays.sort(doneServicesOrdered, (lhs, rhs)
                -> DateTimeFormatUtil.getHistoryDateToCompare(rhs.getDoneAt())
                - DateTimeFormatUtil.getHistoryDateToCompare(lhs.getDoneAt()));

        for (CarIssue issue: doneServicesOrdered){
            addIssue(issue);
        }
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
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_issue_group, parent, false);
        }

        String header = headers.get(groupPosition);
        ((TextView) convertView.findViewById(R.id.issue_group_header)).setText(header.equals("") ? convertView.getContext().getString(R.string.no_date) : header);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_issue, parent, false);
        }

        final CarIssue issue = (CarIssue) getChild(groupPosition, childPosition);

        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView desc = (TextView) convertView.findViewById(R.id.description);
        TextView date = (TextView) convertView.findViewById(R.id.date);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.image_icon);
        ImageView doneImageView = (ImageView) convertView.findViewById((R.id.image_done_issue));

        //Do not show done button inside history since services are already considered completed
        doneImageView.setVisibility(View.INVISIBLE);

        desc.setText(issue.getDescription());
        if (issue.getDoneAt() == null || issue.getDoneAt().equals("null")) {
            date.setText(convertView.getContext().getString(R.string.done));
        } else {
            date.setText(String.format("Done on %s", DateTimeFormatUtil.formatDateToHistoryFormat(issue.getDoneAt())));
        }

        if (issue.getIssueType().equals(CarIssue.RECALL)) {
            imageView.setImageDrawable(convertView.getContext().getResources().getDrawable(R.drawable.ic_error_red_600_24dp));
        } else if (issue.getIssueType().equals(CarIssue.DTC)) {
            imageView.setImageDrawable(convertView.getContext().getResources().getDrawable(R.drawable.car_engine_red));
        } else if (issue.getIssueType().equals(CarIssue.PENDING_DTC)) {
            imageView.setImageDrawable(convertView.getContext().getResources().getDrawable(R.drawable.car_engine_yellow));
        } else {
            imageView.setImageDrawable(convertView.getContext().getResources().getDrawable(R.drawable.ic_warning_amber_300_24dp));
        }

        title.setText(String.format("%s %s", issue.getAction(), issue.getItem()));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
