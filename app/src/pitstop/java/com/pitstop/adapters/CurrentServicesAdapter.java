package com.pitstop.adapters;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.ui.MainActivity;
import com.pitstop.ui.issue_detail.IssueDetailsActivity;
import com.pitstop.ui.services.CurrentServicesFragment;
import com.pitstop.utils.MixpanelHelper;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */
public class CurrentServicesAdapter extends RecyclerView.Adapter<CurrentServicesAdapter.ViewHolder> {

    private WeakReference<Activity> activityReference;

    private List<CarIssue> carIssues;
    private Car dashboardCar;
    static final int VIEW_TYPE_EMPTY = 100;
    static final int VIEW_TYPE_TENTATIVE = 101;

    public CurrentServicesAdapter(List<CarIssue> carIssues, Activity activity, Car dashboardCar) {
        this.carIssues = carIssues;
        this.dashboardCar = dashboardCar;
        Log.d(CurrentServicesFragment.TAG, "Car issue list size: " + this.carIssues.size());
        activityReference = new WeakReference<>(activity);
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
        //Log.i(TAG,"On bind view holder");
        if (activityReference.get() == null) return;
        final Activity activity = activityReference.get();

        int viewType = getItemViewType(position);

        holder.date.setVisibility(View.GONE);

        if (viewType == VIEW_TYPE_EMPTY) {
            holder.description.setMaxLines(2);
            holder.description.setText("You have no pending Engine Code, Recalls or Services");
            holder.title.setText("Congrats!");
            holder.imageView.setImageDrawable(
                    ContextCompat.getDrawable(activity, R.drawable.ic_check_circle_green_400_36dp));
        } else if (viewType == VIEW_TYPE_TENTATIVE) {
            holder.description.setMaxLines(2);
            holder.description.setText("Tap to start");
            holder.title.setText("Book your first tentative service");
            holder.imageView.setImageDrawable(
                    ContextCompat.getDrawable(activity, R.drawable.ic_announcement_blue_600_24dp));
            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // removeTutorial();
                    ((MainActivity) activity).prepareAndStartTutorialSequence();
                }
            });
        } else {
            final CarIssue carIssue = carIssues.get(position);

            holder.description.setText(carIssue.getDescription());
            holder.description.setEllipsize(TextUtils.TruncateAt.END);
            if (carIssue.getIssueType().equals(CarIssue.RECALL)) {
                holder.imageView.setImageDrawable(ContextCompat
                        .getDrawable(activity, R.drawable.ic_error_red_600_24dp));

            } else if (carIssue.getIssueType().equals(CarIssue.DTC)) {
                holder.imageView.setImageDrawable(ContextCompat
                        .getDrawable(activity, R.drawable.car_engine_red));

            } else if (carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                holder.imageView.setImageDrawable(ContextCompat
                        .getDrawable(activity, R.drawable.car_engine_yellow));
            } else {
                holder.description.setText(carIssue.getDescription());
                holder.imageView.setImageDrawable(ContextCompat
                        .getDrawable(activity, R.drawable.ic_warning_amber_300_24dp));
            }

            holder.title.setText(String.format("%s %s", carIssue.getAction(), carIssue.getItem()));

            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MixpanelHelper((GlobalApplication) activity.getApplicationContext())
                            .trackButtonTapped(carIssues.get(position).getItem(), MixpanelHelper.DASHBOARD_VIEW);

                    Intent intent = new Intent(activity, IssueDetailsActivity.class);
                    intent.putExtra(MainActivity.CAR_EXTRA, dashboardCar);
                    intent.putExtra(MainActivity.CAR_ISSUE_EXTRA, carIssue);
                    activity.startActivityForResult(intent, MainActivity.RC_DISPLAY_ISSUE);
                }
            });
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
        public View date; // Not used here so it is set to GONE

        public ViewHolder(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            description = (TextView) v.findViewById(R.id.description);
            imageView = (ImageView) v.findViewById(R.id.image_icon);
            container = v.findViewById(R.id.list_car_item);
            date = v.findViewById(R.id.date);
        }
    }
}
