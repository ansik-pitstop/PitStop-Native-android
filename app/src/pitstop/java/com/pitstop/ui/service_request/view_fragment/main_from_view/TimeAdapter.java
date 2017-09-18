package com.pitstop.ui.service_request.view_fragment.main_from_view;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;


import java.util.List;

/**
 * Created by Matthew on 2017-07-12.
 */

public class TimeAdapter extends RecyclerView.Adapter<TimeAdapter.TimeViewHolder> {

    private final int VIEW_TYPE_EMPTY = 100;

    private PresenterCallback callback;

    private List<String> times;

    public TimeAdapter (@NonNull List<String> times,PresenterCallback callback) {
        this.times = times;
        this.callback = callback;
    }

    @Override
    public TimeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new TimeViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_time,parent,false));
    }

    @Override
    public void onBindViewHolder(TimeViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if(viewType == VIEW_TYPE_EMPTY){
            holder.time.setText(Resources.getSystem().getString(R.string.no_available_times));
        }else{
            String timeSelect = times.get(position);
            holder.time.setText(timeSelect);
            holder.setClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onTimeClicked(timeSelect);
                }
            });
        }

    }

    @Override
    public int getItemViewType(int position) {
        if(times.isEmpty()){
            return VIEW_TYPE_EMPTY;
        }
        return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        return times.size();
    }

    public class TimeViewHolder extends RecyclerView.ViewHolder{
        TextView time;

        public TimeViewHolder(View itemView){
            super(itemView);
            time = (TextView) itemView.findViewById(R.id.service_time_cell);
        }
        public void setClickListener(final View.OnClickListener clickListener){
            itemView.setOnClickListener(clickListener);
        }
    }
}
