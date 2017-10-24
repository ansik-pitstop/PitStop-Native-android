package com.pitstop.ui.services.upcoming;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.service.UpcomingService;

import java.util.ArrayList;

/**
 * Created by ishan on 2017-10-11.
 */

public class UpcomingServicesAdapter extends RecyclerView.Adapter<UpcomingServicesAdapter.IssueViewHolder> {

    private ArrayList<UpcomingService> servicesList = new ArrayList<>();
    private int mileage;
    UpcomingServicesView upcomingServicesView;

    public UpcomingServicesAdapter(ArrayList<UpcomingService> list, UpcomingServicesView servicesView){
        this.servicesList =list;
        this.upcomingServicesView = servicesView;
    }


    @Override
    public IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.issue_timeline_list_item, parent, false);
        view.setOnClickListener(view1 -> upcomingServicesView.onUpcomingServiceClicked(servicesList, getItemViewType(viewType)));
        IssueViewHolder issueViewHolder = new IssueViewHolder(view);
        return issueViewHolder;
    }

    @Override
    public void onBindViewHolder(IssueViewHolder holder, int position) {
        holder.bind(servicesList.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return position;
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
