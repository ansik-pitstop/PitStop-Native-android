package com.pitstop.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.services.current.IssueHolderListener;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */
public class CurrentServicesAdapter extends RecyclerView.Adapter<CurrentServicesAdapter.ViewHolder> {

    private IssueHolderListener issueHolderListener;
    private List<CarIssue> carIssues;
    static final int VIEW_TYPE_EMPTY = 100;
    static final int VIEW_TYPE_TENTATIVE = 101;

    public CurrentServicesAdapter(List<CarIssue> carIssues
            , IssueHolderListener issueHolderListener) {
        this.carIssues = carIssues;
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

        int viewType = getItemViewType(position);

        holder.date.setVisibility(View.GONE);

        if (viewType == VIEW_TYPE_EMPTY) {
            holder.description.setMaxLines(2);
            holder.description.setText("You have no pending Engine Code, Recalls or Routine Services");
            holder.title.setText("Congrats!");
            holder.checkBox.setVisibility(View.INVISIBLE);
            holder.imageView.setImageDrawable(
                    ContextCompat.getDrawable(holder.container.getContext()
                            , R.drawable.ic_check_circle_green_400_36dp));
        } else if (viewType == VIEW_TYPE_TENTATIVE) {
            holder.checkBox.setVisibility(View.INVISIBLE);
            holder.description.setMaxLines(2);
            holder.description.setText("Tap to start");
            holder.title.setText("Book your first tentative service");
            holder.imageView.setImageDrawable(
                    ContextCompat.getDrawable(holder.container.getContext(), R.drawable.ic_announcement_blue_600_24dp));
            holder.container.setOnClickListener(v -> issueHolderListener.onTentativeServiceClicked());
        } else {
            final CarIssue carIssue = carIssues.get(position);

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

            holder.title.setText(String.format("%s %s", carIssue.getAction(), carIssue.getItem()));

            holder.container.setOnClickListener((View view) -> {
                issueHolderListener.onServiceClicked(carIssues, position);

            });

            //Get the done image view
            holder.checkBox.setOnClickListener((View view)
                    -> issueHolderListener.onServiceDoneClicked(carIssue));
        }
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
        if (carIssues.isEmpty()) {
            return 1;
        }
        return carIssues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView title;
        public TextView description;
        public ImageView imageView;
        public View container;
        public CardView card;
        public View date; // Not used here so it is set to GONE
        public CheckBox checkBox;

        public ViewHolder(View v) {
            super(v);
            card = v.findViewById(R.id.list_car_item);
            title = v.findViewById(R.id.title);
            description = v.findViewById(R.id.description);
            imageView = v.findViewById(R.id.image_icon);
            checkBox = v.findViewById(R.id.checkBox);
            container = v.findViewById(R.id.list_car_item);
            date = v.findViewById(R.id.date);
        }
    }
}
