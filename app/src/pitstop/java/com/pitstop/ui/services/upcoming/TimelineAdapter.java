package com.pitstop.ui.services.upcoming;

import android.content.res.Resources;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.service.UpcomingService;

import java.util.List;

/**
 * Created by Karol Zdebel on 8/31/2017.
 */
public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int MILEAGE = 0;
    private static final int ISSUE = 1;

    private List<Object> mTimelineDisplayList;

    public TimelineAdapter(List<Object> mTimelineDisplayList) {
        this.mTimelineDisplayList = mTimelineDisplayList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            case MILEAGE:
                layoutId = R.layout.mileage_timeline_list_item;
                return new MileageViewHolder(inflateLayout(parent, layoutId));
            case ISSUE:
                layoutId = R.layout.issue_timeline_list_item;
                return new IssueViewHolder(inflateLayout(parent, layoutId));
            default:
                return null;
        }
    }

    private View inflateLayout(ViewGroup parent, @LayoutRes int layoutResId) {
        return LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MileageViewHolder) {
            ((MileageViewHolder) holder).bind((String) mTimelineDisplayList.get(position));
        } else if (holder instanceof IssueViewHolder) {
            ((IssueViewHolder) holder).bind((UpcomingService) mTimelineDisplayList.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return mTimelineDisplayList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mTimelineDisplayList.get(position) instanceof String)
            return MILEAGE;
        else
            return ISSUE;
    }

    private class MileageViewHolder extends RecyclerView.ViewHolder{

        TextView mileageTextView;

        public MileageViewHolder(View itemView) {
            super(itemView);
            mileageTextView = (TextView) itemView;
        }

        public void bind(String s) {
            String mileage = s + " " + Resources.getSystem().getString(R.string.kilometers_unit);
            mileageTextView.setText(mileage);
        }
    }

    private class IssueViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        TextView mTitleTextView;
        UpcomingService upcomingService;

        public IssueViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mTitleTextView = (TextView) itemView.findViewById(R.id.title);
        }

        public void bind(UpcomingService upcomingService) {
            this.upcomingService = upcomingService;
            mTitleTextView.setText(upcomingService.getAction() + " " + upcomingService.getItem());
        }

        @Override
        public void onClick(View v) {

        }
    }
}

