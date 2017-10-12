package com.pitstop.ui.services.upcoming;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.adapters.NotificationAdapter;
import com.pitstop.models.service.UpcomingService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ishan on 2017-10-11.
 */

public class UpcomingServicesAdapter extends RecyclerView.Adapter<UpcomingServicesAdapter.IssueViewHolder> {

    private List<UpcomingService> servicesList = new ArrayList<>();
    private int mileage;

    public UpcomingServicesAdapter(List<UpcomingService> list){
        this.servicesList =list;
    }


    @Override
    public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.issue_timeline_list_item, parent, false);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        IssueViewHolder issueViewHolder = new IssueViewHolder(view);
        return issueViewHolder;
    }

    @Override
    public void onBindViewHolder(IssueViewHolder holder, int position) {
        holder.bind(servicesList.get(position));
    }

    @Override
    public int getItemCount() {
        return servicesList.size();
    }

    public class IssueViewHolder extends RecyclerView.ViewHolder{


        TextView mTitleTextView;
        UpcomingService upcomingService;

        public IssueViewHolder(View itemView) {
            super(itemView);
            mTitleTextView = (TextView) itemView.findViewById(R.id.title);
        }

        public void bind(UpcomingService upcomingService) {
            this.upcomingService = upcomingService;
            mTitleTextView.setText(upcomingService.getAction() + " " + upcomingService.getItem());
        }
    }
}
