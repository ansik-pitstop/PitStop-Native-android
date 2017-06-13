package com.pitstop.ui.service_request.view_fragment;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.issue.CarIssue;

import java.util.Iterator;
import java.util.List;

public class ServiceIssueAdapter extends RecyclerView.Adapter<ServiceIssueAdapter.ServiceIssueViewHolder> {

    private static final String TAG = ServiceIssueAdapter.class.getSimpleName();

    private final int VIEW_TYPE_EMPTY = 100;
    private final int VIEW_TYPE_CUSTOM = 101;

    private final List<CarIssue> mCarIssues;
    private final Context mContext;

    public ServiceIssueAdapter(Context context, @NonNull List<CarIssue> issues) {
        mCarIssues = issues;
        mContext = context;
    }

    public void setPickedCustomIssues(List<CarIssue> pickedIssues){
        Log.d(TAG, "SetPickedCustomIssues: " + pickedIssues.size());
        clearCustomIssues();
        mCarIssues.addAll(pickedIssues);
        notifyDataSetChanged();
    }

    private void clearCustomIssues(){
        Iterator<CarIssue> i = mCarIssues.iterator();
        while (i.hasNext()){
            CarIssue issue = i.next();
            if (issue.getIssueType().equals(CarIssue.TYPE_PRESET)){
                i.remove();
            }
        }
    }

    @Override
    public ServiceIssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ServiceIssueViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_service_issue, parent, false));
    }

    @Override
    public void onBindViewHolder(ServiceIssueViewHolder holder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_EMPTY) {
            holder.title.setText("No issues");
            holder.icon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_check_circle_green_400_36dp));
        } else {
            final CarIssue issue = mCarIssues.get(position);
            if (issue.getIssueType().equals(CarIssue.RECALL)) {
                holder.icon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_error_red_600_24dp));
            } else if (issue.getIssueType().equals(CarIssue.DTC)) {
                holder.icon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.car_engine_red));
            } else if (issue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                holder.icon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.car_engine_yellow));
            } else {
                holder.icon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_warning_amber_300_24dp));
            }

            holder.title.setText(String.format("%s %s", issue.getAction(), issue.getItem()));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mCarIssues.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getItemCount() {
        if (mCarIssues.isEmpty()) return 1;
        return mCarIssues.size();
    }

    public class ServiceIssueViewHolder extends RecyclerView.ViewHolder{
        TextView title;
        ImageView icon;
        public ServiceIssueViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.issue_title);
            icon = (ImageView) itemView.findViewById(R.id.issue_icon);
        }
    }
}
