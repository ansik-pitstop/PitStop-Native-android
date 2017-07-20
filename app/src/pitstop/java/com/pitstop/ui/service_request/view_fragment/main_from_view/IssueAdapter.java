package com.pitstop.ui.service_request.view_fragment.main_from_view;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.add_car.AddCarContract;

import java.util.List;

/**
 * Created by Matthew on 2017-07-14.
 */

public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder> {

        private List<CarIssue> issues;
        private Context context;
        private boolean truncate;
        private PresenterCallback presenter;



        public IssueAdapter(List<CarIssue> issues ,boolean truncate ,PresenterCallback presenter,Context context) {
            this.issues = issues;
            this.truncate = truncate;
            this.presenter = presenter;
            this.context = context;
        }

        @Override
        public IssueAdapter.IssueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_add_preset_issue_item, parent, false);
            return new IssueAdapter.IssueViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final IssueAdapter.IssueViewHolder holder, final int position) {
            if (context == null) return;
            final CarIssue presetIssue = issues.get(position);

            holder.description.setText(presetIssue.getDescription());
            if(truncate){
                holder.description.setMaxLines(1);
            }
            holder.title.setText(String.format("%s %s", presetIssue.getAction(), presetIssue.getItem()));

            switch (presetIssue.getId()) {
                case 1:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_flat_tire_severe));
                    break;
                case 2:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_replace_orange_48px));
                    break;
                case 3:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_replace_yellow_48px));
                    break;
                case 4:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_tow_truck_severe));
                    break;
                case 5:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.preset_service_medium));
                    break;
                default:
                    holder.imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.preset_service_medium));
                    break;
            }
            if(!truncate){
                holder.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        presenter.onIssueClicked(presetIssue);
                    }
                });
            }else{
                holder.removeButton.setVisibility(View.VISIBLE);
                holder.removeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        presenter.onRemoveClicked(presetIssue);
                    }
                });

            }
        }

        @Override
        public int getItemCount() {
            return issues.size();
        }

        public class IssueViewHolder extends RecyclerView.ViewHolder {
            public TextView title;
            public TextView description;
            public ImageView imageView;
            public ImageView removeButton;
            public View container;

            public IssueViewHolder(View itemView) {
                super(itemView);
                title = (TextView) itemView.findViewById(R.id.title);
                description = (TextView) itemView.findViewById(R.id.description);
                imageView = (ImageView) itemView.findViewById(R.id.image_icon);
                removeButton = (ImageView) itemView.findViewById(R.id.remove_service_button);
                container = itemView.findViewById(R.id.list_car_item);
            }
            public void setOnClickListener(View.OnClickListener onClickListener){
                itemView.setOnClickListener(onClickListener);
            }
        }


}
