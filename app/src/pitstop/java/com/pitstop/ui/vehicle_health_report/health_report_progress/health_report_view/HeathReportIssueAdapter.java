package com.pitstop.ui.vehicle_health_report.health_report_progress.health_report_view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.models.report.Recall;

import java.util.List;

/**
 * Created by Matt on 2017-08-18.
 */

public class HeathReportIssueAdapter extends RecyclerView.Adapter<HeathReportIssueAdapter.IssueViewHolder> {

    private final int VIEW_TYPE_EMPTY = 100;

    private final List<Object> issues;

    private String emptyText;

    private Context context;

    private HealthReportPresenterCallback callback;

    public HeathReportIssueAdapter(@NonNull List<Object> issues, String emptyText,HealthReportPresenterCallback callback, Context context) {
        this.issues = issues;
        this.emptyText = emptyText;
        this.callback = callback;
        this.context = context;
    }

    @Override
    public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new IssueViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_issue_report, parent, false));
    }

    @Override
    public void onBindViewHolder(IssueViewHolder holder, final int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_EMPTY) {
            holder.action.setText(emptyText);
            holder.description.setText("");
            holder.image.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_check_circle_green_400_36dp));
        } else {
            final CarIssue carIssue = issues.get(position);

            holder.description.setText(carIssue.getDescription());
            holder.description.setEllipsize(TextUtils.TruncateAt.END);
            if (issues.get(0) != null && issues.get(0) instanceof Recall) {
                holder.image.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.ic_error_red_600_24dp));

            } else if (issues.get(0) != null && issues.get(0) instanceof E) {
                holder.image.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.car_engine_red));

            } else if (carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                holder.image.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.car_engine_yellow));
            } else {
                holder.description.setText(carIssue.getDescription());
                holder.image.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.ic_warning_amber_300_24dp));
            }

            holder.action.setText(String.format("%s %s", carIssue.getAction(), carIssue.getItem()));
            holder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    callback.issueClicked(issues.get(position));
                }
            });


        }
    }

    @Override
    public int getItemViewType(int position) {
        if (issues.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getItemCount() {
        if (issues.isEmpty()) return 1;
        return issues.size();
    }

    public class IssueViewHolder extends RecyclerView.ViewHolder{
        TextView description;
        TextView action;
        ImageView image;

        public IssueViewHolder(View itemView) {
            super(itemView);
            description = (TextView) itemView.findViewById(R.id.description_issue);
            action = (TextView) itemView.findViewById(R.id.action_issue);
            image = (ImageView) itemView.findViewById(R.id.image_issue);
        }
        public void setOnClickListener(View.OnClickListener clickListener){
            itemView.setOnClickListener(clickListener);
        }
    }

}
