package com.pitstop.ui.services;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.CarIssue;
import com.pitstop.utils.DateTimeFormatUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Karol Zdebel on 6/1/2017.
 */
public class HistoryIssueGroupAdapter extends BaseExpandableListAdapter {

    private LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues;
    private ArrayList<String> headers;
    private LayoutInflater inflater;
    private Activity activity;


    public HistoryIssueGroupAdapter(Activity activity, LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues, ArrayList<String> headers) {
        this.activity = activity;
        this.sortedIssues = sortedIssues;
        this.headers = headers;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
            convertView = inflater.inflate(R.layout.list_item_issue_group, parent, false);
        }

        String header = headers.get(groupPosition);
        ((TextView) convertView.findViewById(R.id.issue_group_header)).setText(header.equals("") ? "No date" : header);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
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
            date.setText("Done");
        } else {
            date.setText(String.format("Done on %s", DateTimeFormatUtil.formatDateHistory(issue.getDoneAt())));
        }

        if (issue.getIssueType().equals(CarIssue.RECALL)) {
            imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_error_red_600_24dp));
        } else if (issue.getIssueType().equals(CarIssue.DTC)) {
            imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.car_engine_red));
        } else if (issue.getIssueType().equals(CarIssue.PENDING_DTC)) {
            imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.car_engine_yellow));
        } else {
            imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_warning_amber_300_24dp));
        }

        title.setText(String.format("%s %s", issue.getAction(), issue.getItem()));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
