package com.pitstop.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pitstop.R;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.services.current.IssueHolderListener;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */
public class ServicesAdapter extends RecyclerView.Adapter<ServicesAdapter.ViewHolder> {

    private IssueHolderListener issueHolderListener;
    private List<CarIssue> carIssues;
    private LinkedHashMap<CarIssue,Boolean> selectionMap;
    static final int VIEW_TYPE_EMPTY = 100;
    static final int VIEW_TYPE_TENTATIVE = 101;

    public ServicesAdapter(List<CarIssue> carIssues, LinkedHashMap<CarIssue,Boolean> selectionMap
            , IssueHolderListener issueHolderListener) {
        this.carIssues = carIssues;
        this.selectionMap = selectionMap;
        this.issueHolderListener = issueHolderListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_issue, parent, false);
        return new ViewHolder(v);
    }

    public CarIssue getItem(int position) {
        return carIssues.get(position);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        holder.date.setVisibility(View.GONE);
        holder.checkBox.setVisibility(View.VISIBLE);

        final CarIssue carIssue = carIssues.get(position);

        if (selectionMap.keySet().contains(carIssue))
            holder.checkBox.setChecked(selectionMap.get(carIssue));
        holder.description.setText(carIssue.getDescription());
        holder.description.setEllipsize(TextUtils.TruncateAt.END);
        if (carIssue.getIssueType().equals(CarIssue.RECALL)) {
            holder.imageView.setImageDrawable(ContextCompat
                    .getDrawable(holder.container.getContext(), R.drawable.recall_yellow3x));

        } else if (carIssue.getIssueType().equals(CarIssue.DTC)) {
            holder.imageView.setImageDrawable(ContextCompat
                    .getDrawable(holder.container.getContext(), R.drawable.car_engine_red));

        } else if (carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
            holder.imageView.setImageDrawable(ContextCompat
                    .getDrawable(holder.container.getContext(), R.drawable.car_engine_yellow));
        } else {
            holder.description.setText(carIssue.getDescription());
            holder.imageView.setImageDrawable(ContextCompat
                    .getDrawable(holder.container.getContext(), R.drawable.ic_warning_amber_300_24dp));
        }

        holder.title.setText(String.format("%s",carIssue.getItem()));

        holder.container.setOnClickListener((View view)
                -> issueHolderListener.onServiceClicked(carIssues, position));

        //Get the done image view
        holder.checkBox.setOnClickListener((View view) -> {
            issueHolderListener.onServiceSelected(carIssue);
        });
    }

    @Override
    public int getItemViewType(int position) {
        if (carIssues.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        } else if (carIssues.get(position).getIssueType().equals(CarIssue.TENTATIVE)) {
            return VIEW_TYPE_TENTATIVE;
        }
        return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        return carIssues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView title;
        public TextView description;
        public ImageView imageView;
        public View container;
        public View date; // Not used here so it is set to GONE
        public CheckBox checkBox;

        public ViewHolder(View v) {
            super(v);
            title = v.findViewById(R.id.title);
            description = v.findViewById(R.id.description);
            imageView = v.findViewById(R.id.image_icon);
            checkBox = v.findViewById(R.id.checkbox);
            container = v.findViewById(R.id.list_car_item);
            date = v.findViewById(R.id.date);
        }
    }
}
