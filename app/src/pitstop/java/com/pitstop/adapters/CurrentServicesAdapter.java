package com.pitstop.adapters;

import android.app.DatePickerDialog;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.interactors.MarkServiceDoneUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.ui.mainFragments.CarDataChangedNotifier;
import com.pitstop.ui.main_activity.MainActivityCallback;
import com.pitstop.ui.services.ServicesDatePickerDialog;
import com.pitstop.utils.MixpanelHelper;

import java.util.Calendar;
import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */
public class CurrentServicesAdapter extends RecyclerView.Adapter<CurrentServicesAdapter.ViewHolder> {

    private Context context;

    private MainActivityCallback mainActivityCallback;
    private Car dashboardCar;
    private List<CarIssue> carIssues;
    static final int VIEW_TYPE_EMPTY = 100;
    static final int VIEW_TYPE_TENTATIVE = 101;
    private MarkServiceDoneUseCase markServiceDoneUseCase;
    private CarDataChangedNotifier notifier;

    public CurrentServicesAdapter(Car dashboardCar, List<CarIssue> carIssues
                , MainActivityCallback tutorialCallback,Context context
            , MarkServiceDoneUseCase markServiceDoneUseCase, CarDataChangedNotifier notifier) {
        this.dashboardCar = dashboardCar;
        this.carIssues = carIssues;
        this.context = context;
        this.notifier = notifier;
        this.markServiceDoneUseCase = markServiceDoneUseCase;
        this.mainActivityCallback = tutorialCallback;
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
        if (context == null) return;

        int viewType = getItemViewType(position);

        holder.date.setVisibility(View.GONE);

        if (viewType == VIEW_TYPE_EMPTY) {
            holder.description.setMaxLines(2);
            holder.doneImageView.setVisibility(View.INVISIBLE);
            holder.description.setText("You have no pending Engine Code, Recalls or Services");
            holder.title.setText("Congrats!");
            holder.imageView.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_check_circle_green_400_36dp));
        } else if (viewType == VIEW_TYPE_TENTATIVE) {
            holder.description.setMaxLines(2);
            holder.description.setText("Tap to start");
            holder.title.setText("Book your first tentative service");
            holder.imageView.setImageDrawable(
                    ContextCompat.getDrawable(context, R.drawable.ic_announcement_blue_600_24dp));
            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // removeTutorial();
                    mainActivityCallback.prepareAndStartTutorialSequence();
                }
            });
        } else {
            final CarIssue carIssue = carIssues.get(position);

            holder.description.setText(carIssue.getDescription());
            holder.description.setEllipsize(TextUtils.TruncateAt.END);
            if (carIssue.getIssueType().equals(CarIssue.RECALL)) {
                holder.imageView.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.ic_error_red_600_24dp));

            } else if (carIssue.getIssueType().equals(CarIssue.DTC)) {
                holder.imageView.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.car_engine_red));

            } else if (carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                holder.imageView.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.car_engine_yellow));
            } else {
                holder.description.setText(carIssue.getDescription());
                holder.imageView.setImageDrawable(ContextCompat
                        .getDrawable(context, R.drawable.ic_warning_amber_300_24dp));
            }

            holder.title.setText(String.format("%s %s", carIssue.getAction(), carIssue.getItem()));

            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new MixpanelHelper((GlobalApplication) context.getApplicationContext())
                            .trackButtonTapped(carIssues.get(position).getItem(), MixpanelHelper.DASHBOARD_VIEW);

                    mainActivityCallback.startDisplayIssueActivity(dashboardCar,carIssue);
                }
            });

            //Get the done image view
            holder.doneImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //When clicked show date picker
                    DatePickerDialog servicesDatePickerDialog = new ServicesDatePickerDialog(context
                            , Calendar.getInstance(), new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker datePicker, int year, int month, int day) {

                            carIssue.setYear(year);
                            carIssue.setMonth(month);
                            carIssue.setDay(day);

                            //When the date is set, update issue to done on that date
                            markServiceDoneUseCase.execute(carIssue, new MarkServiceDoneUseCase.Callback() {
                                @Override
                                public void onServiceMarkedAsDone() {
                                    Toast.makeText(context,"Successfully marked service as done"
                                            ,Toast.LENGTH_LONG);
                                    carIssues.remove(carIssue);
                                    notifyDataSetChanged();
                                    EventType event = new EventTypeImpl(EventType
                                            .EVENT_SERVICES_HISTORY);
                                    EventSource source = new EventSourceImpl(EventSource
                                            .SOURCE_SERVICES_CURRENT);
                                    notifier.notifyCarDataChanged(event,source);
                                }

                                @Override
                                public void onError() {
                                }
                            });
                        }
                    });
                    servicesDatePickerDialog.setTitle("Select when you completed this service.");
                    servicesDatePickerDialog.show();
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
        public ImageView doneImageView;

        public ViewHolder(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            description = (TextView) v.findViewById(R.id.description);
            imageView = (ImageView) v.findViewById(R.id.image_icon);
            doneImageView = (ImageView) v.findViewById(R.id.image_done_issue);
            container = v.findViewById(R.id.list_car_item);
            date = v.findViewById(R.id.date);
        }
    }
}
