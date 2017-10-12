package com.pitstop.ui.services.upcoming;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.service.UpcomingService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Karol Zdebel on 8/31/2017.
 */
public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = TimelineAdapter.class.getSimpleName();

    private List<Integer> mileageList;
    private LinkedHashMap<Integer, List<UpcomingService>> upcomingServices;

    public TimelineAdapter(LinkedHashMap<Integer, List<UpcomingService>> upcomingservices, List<Integer> MileageList) {
        this.mileageList = MileageList;
        this.upcomingServices = upcomingservices;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mileage_timeline_list_item, parent, false);
        MileageIssuesHolder mileageIssuesHolder = new MileageIssuesHolder(view, parent.getContext());
        return mileageIssuesHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
       int mileage = getItemViewType(position);
        ((MileageIssuesHolder)holder).bind(upcomingServices.get(mileage), mileage);
    }

    @Override
    public int getItemCount() {
        return upcomingServices.size();
    }

    @Override
    public int getItemViewType(int position) {
        // returns the mileage corresponding to the position
        return mileageList.get(position);
    }

    public class MileageIssuesHolder extends RecyclerView.ViewHolder{

        TextView mileageTV;
        private List<UpcomingService> services;
        private int mileage;
        RecyclerView mRecyclerView;
        UpcomingServicesAdapter adapter;
        private Context context;
        public MileageIssuesHolder(View itemView, Context con) {
            super(itemView);
            this.context = con;
            mileageTV = itemView.findViewById(R.id.mileage_timeline_list_item_text_view);
            mRecyclerView = itemView.findViewById(R.id.mileage_specific_issues_recycler_view);
        }

        public void bind(List<UpcomingService> serviceList, int Mileage){
            this.services = serviceList;
            this.mileage = Mileage;
            mileageTV.setText(Integer.toString(this.mileage) + " KM");
            adapter = new UpcomingServicesAdapter(this.services);
            mRecyclerView.setAdapter(adapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this.context));

        }
    }
}

