package com.pitstop.ui.services.upcoming;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.service.UpcomingService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Karol Zdebel on 8/31/2017.
 */
public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = TimelineAdapter.class.getSimpleName();

    private List<Integer> mileageList;
    private LinkedHashMap<Integer, List<UpcomingService>> upcomingServices;
    UpcomingServicesView upcomingServicesView;

    public TimelineAdapter(LinkedHashMap<Integer, List<UpcomingService>> upcomingservices, List<Integer> MileageList,
                            UpcomingServicesView servicesView) {
        this.mileageList = MileageList;
        this.upcomingServices = upcomingservices;
        this.upcomingServicesView = servicesView;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mileage_timeline_list_item, parent, false);
        MileageIssuesHolder mileageIssuesHolder = new MileageIssuesHolder(view, parent.getContext(), this.upcomingServicesView);
        return mileageIssuesHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
       int mileage = getItemViewType(position);
       ArrayList<UpcomingService> arrayList  = new ArrayList<UpcomingService>();
       for (UpcomingService service: upcomingServices.get(mileage)){
           arrayList.add(service);
       }
        ((MileageIssuesHolder)holder).bind(arrayList, mileage);
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
        private ArrayList<UpcomingService> services;
        private int mileage;
        RecyclerView mRecyclerView;
        UpcomingServicesAdapter adapter;
        private Context context;
        UpcomingServicesView upcomingServicesView;
        public MileageIssuesHolder(View itemView, Context con, UpcomingServicesView servicesView) {
            super(itemView);
            this.context = con;
            mileageTV = itemView.findViewById(R.id.mileage_timeline_list_item_text_view);
            mRecyclerView = itemView.findViewById(R.id.mileage_specific_issues_recycler_view);
            mRecyclerView.setNestedScrollingEnabled(false);
            this.upcomingServicesView = servicesView;

        }

        public void bind(ArrayList<UpcomingService> serviceList, int Mileage){
            this.services = serviceList;
            this.mileage = Mileage;
            mileageTV.setText(Integer.toString(this.mileage) + " KM");
            adapter = new UpcomingServicesAdapter(this.services, this.upcomingServicesView );
            mRecyclerView.setAdapter(adapter);
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this.context));

        }
    }
}

