package com.pitstop.ui.vehicle_health_report.health_report_view;

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
import com.pitstop.models.report.CarHealthItem;
import com.pitstop.models.report.EngineIssue;
import com.pitstop.models.report.Recall;
import com.pitstop.models.report.Service;

import java.util.List;

/**
 * Created by Matt on 2017-08-18.
 */

public class HeathReportIssueAdapter extends RecyclerView.Adapter<HeathReportIssueAdapter.IssueViewHolder> {

    private final int VIEW_TYPE_EMPTY = 100;

    private final List<CarHealthItem> carHealthItemList;

    private String emptyText;

    private Context context;

    private HealthReportPresenterCallback callback;

    public HeathReportIssueAdapter(@NonNull List<CarHealthItem> carHealthItems
            , String emptyText, HealthReportPresenterCallback callback, Context context) {
        this.carHealthItemList = carHealthItems;
        this.emptyText = emptyText;
        this.callback = callback;
        this.context = context;
    }

    @Override
    public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new IssueViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_issue_report, parent, false));
    }

    @Override
    public void onBindViewHolder(IssueViewHolder holder, final int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_EMPTY) {
            holder.action.setText(emptyText);
            holder.description.setText("");
            holder.image.setImageDrawable(ContextCompat.getDrawable(context,R.drawable.ic_check_circle_green_400_36dp));
        } else {
            final CarHealthItem carHealthItem = carHealthItemList.get(position);

            holder.description.setText(carHealthItem.getDescription());
            holder.description.setEllipsize(TextUtils.TruncateAt.END);
            if (carHealthItem instanceof Recall) {
                holder.action.setText(String.format("%s %s"
                        , "Recall", carHealthItem.getItem()));
                holder.image.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.ic_error_red_600_24dp));

            } else if (carHealthItem instanceof EngineIssue) {
                holder.action.setText(String.format("%s %s"
                        , "Pending Engine Code", carHealthItem.getItem()));
                if (((EngineIssue)carHealthItem).isPending()){
                    holder.image.setImageDrawable(ContextCompat
                            .getDrawable(context, R.drawable.car_engine_yellow));
                }
                else{
                    holder.action.setText(String.format("%s %s"
                            , "Stored Engine Code", carHealthItem.getItem()));
                    holder.image.setImageDrawable(ContextCompat
                            .getDrawable(context, R.drawable.car_engine_red));
                }
            } else if (carHealthItem instanceof Service){
                holder.action.setText(String.format("%s %s"
                        , ((Service)carHealthItem).getAction(), carHealthItem.getItem()));
                holder.description.setText(carHealthItem.getDescription());
                holder.image.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.ic_warning_amber_300_24dp));
            }

            holder.setOnClickListener(view
                    -> callback.issueClicked(carHealthItemList.get(position)));


        }
    }

    @Override
    public int getItemViewType(int position) {
        if (carHealthItemList.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getItemCount() {
        if (carHealthItemList.isEmpty()) return 1;
        return carHealthItemList.size();
    }

    public class IssueViewHolder extends RecyclerView.ViewHolder{
        TextView description;
        TextView action;
        ImageView image;

        public IssueViewHolder(View itemView) {
            super(itemView);
            description = itemView.findViewById(R.id.description_issue);
            action = itemView.findViewById(R.id.action_issue);
            image = itemView.findViewById(R.id.image_issue);
        }
        public void setOnClickListener(View.OnClickListener clickListener){
            itemView.setOnClickListener(clickListener);
        }
    }

}
