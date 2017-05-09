package com.pitstop.ui.services;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.MainActivity;
import com.pitstop.ui.issue_detail.IssueDetailsActivity;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class CurrentServicesFragment extends Fragment {

    public static final String TAG = CurrentServicesFragment.class.getSimpleName();

    @BindView(R.id.car_issues_list)
    protected RecyclerView carIssueListView;
    private CustomAdapter carIssuesAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private Car dashboardCar;
    private LocalCarIssueAdapter carIssueLocalStore;
    private LocalCarAdapter carLocalStore;
    private LocalCarAdapter localCarStore;
    private NetworkHelper networkHelper;
    private List<CarIssue> carIssueList = new ArrayList<>();

    public static CurrentServicesFragment newInstance(Car currentCar){
        CurrentServicesFragment fragment = new CurrentServicesFragment();
        Bundle args = new Bundle();
        args.putParcelable("dashboardCar", currentCar);
        fragment.setArguments(args);
        return fragment;
    }


    public CurrentServicesFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkHelper = new NetworkHelper(getActivity().getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_new_services, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        carLocalStore = new LocalCarAdapter(getActivity());
        //dashboardCar = carLocalStore.getCar(PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt(MainDashboardFragment.pfCurrentCar, -1));
        dashboardCar =  getArguments() != null ? (Car)getArguments().getParcelable("dashboardCar"):null;
        carIssuesAdapter = new CustomAdapter(dashboardCar, carIssueList, this.getActivity());
        carIssueListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        carIssueListView.setAdapter(carIssuesAdapter);
        populateCarIssuesAdapter();

    }

    private void populateCarIssuesAdapter() {
        // Try local store
        Log.i(TAG, "DashboardCar id: (Try local store) "+ dashboardCar.getId());
        if(carIssueLocalStore == null) {
            carIssueLocalStore = new LocalCarIssueAdapter(getActivity());
        }
        List<CarIssue> carIssues = carIssueLocalStore.getAllCarIssues(dashboardCar.getId());
        if (carIssues.isEmpty() && (dashboardCar.getNumberOfServices() > 0
                || dashboardCar.getNumberOfRecalls() > 0)) {
            Log.i(TAG, "No car issues in local store");

            networkHelper.getCarsById(dashboardCar.getId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        try {
                            dashboardCar.setIssues(CarIssue.createCarIssues(
                                    new JSONObject(response).getJSONArray("issues"), dashboardCar.getId()));
                            carIssueList.clear();
                            carIssueList.addAll(dashboardCar.getActiveIssues());
                            carIssuesAdapter.notifyDataSetChanged();
                           // setIssuesCount();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            if (getActivity() != null) {
                                Toast.makeText(getActivity(),
                                        "Error retrieving car details", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.e(TAG, "Load issues error: " + requestError.getMessage());
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(),
                                    "Error retrieving car details", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        } else {
            Log.i(TAG, "Trying local store for carIssues");
            Log.i(TAG, "Number of active issues: " + dashboardCar.getActiveIssues().size());
            dashboardCar.setIssues(carIssues);
            carIssueList.clear();
            carIssueList.addAll(dashboardCar.getActiveIssues());
            carIssuesAdapter.notifyDataSetChanged();
        }
        //carIssuesAdapter.updateTutorial();
    }

    private class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        private WeakReference<Activity> activityReference;

        private Car dashboardCar;
        private List<CarIssue> carIssues;
        static final int VIEW_TYPE_EMPTY = 100;
        static final int VIEW_TYPE_TENTATIVE = 101;

        public CustomAdapter(Car dashboardCar, List<CarIssue> carIssues, Activity activity) {
            this.dashboardCar = dashboardCar;
            this.carIssues = carIssues;
            Log.d(TAG, "Car issue list size: " + this.carIssues.size());
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
                        ((MainActivity)activity).prepareAndStartTutorialSequence();
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
                        new MixpanelHelper((GlobalApplication)activity.getApplicationContext())
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

        public  class ViewHolder extends RecyclerView.ViewHolder {
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

/*    @OnClick(R.id.dashboard_request_service_btn)
    protected void onServiceRequestButtonClicked(){
        ((MainActivity)getActivity()).requestMultiService(null);
    }*/

        /*private void setSwipeDeleteListener(RecyclerView recyclerView) {
        SwipeableRecyclerViewTouchListener swipeTouchListener =
                new SwipeableRecyclerViewTouchListener(recyclerView,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {

                            @Override
                            public boolean canSwipe(int position) {
                                return carIssuesAdapter.getItemViewType(position) != CustomAdapter.VIEW_TYPE_EMPTY
                                        && carIssuesAdapter.getItemViewType(position) != CustomAdapter.VIEW_TYPE_TENTATIVE;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(final RecyclerView recyclerView,
                                                               final int[] reverseSortedPositions) {

                                final Calendar calendar = Calendar.getInstance();
                                calendar.setTimeInMillis(System.currentTimeMillis());
                                final int currentYear = calendar.get(Calendar.YEAR);
                                final int currentMonth = calendar.get(Calendar.MONTH);
                                final int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

                                final int i = reverseSortedPositions[0];

                                final CarIssue issue = carIssuesAdapter.getItem(i);

                                //Swipe to start deleting(completing) the selected issue
                                mixpanelHelper.trackButtonTapped("Done " + issue.getAction() + " " + issue.getItem(), MixpanelHelper.DASHBOARD_VIEW);


                                DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                                        new DatePickerDialog.OnDateSetListener() {
                                            @Override
                                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                                if (year > currentYear || (year == currentYear
                                                        && (monthOfYear > currentMonth
                                                        || (monthOfYear == currentMonth && dayOfMonth > currentDay)))) {
                                                    Toast.makeText(getActivity(), "Please choose a date that has passed", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    long currentTime = calendar.getTimeInMillis();

                                                    calendar.set(year, monthOfYear, dayOfMonth);

                                                    int daysAgo = (int) TimeUnit.MILLISECONDS.toDays(currentTime - calendar.getTimeInMillis());
                                                    String timeCompleted;

                                                    if (daysAgo < 13) { // approximate categorization of the time service was completed
                                                        timeCompleted = "Recently";
                                                    } else if (daysAgo < 28) {
                                                        timeCompleted = "2 Weeks Ago";
                                                    } else if (daysAgo < 56) {
                                                        timeCompleted = "1 Month Ago";
                                                    } else if (daysAgo < 170) {
                                                        timeCompleted = "2 to 3 Months Ago";
                                                    } else {
                                                        timeCompleted = "6 to 12 Months Ago";
                                                    }

                                                    mixpanelHelper.trackButtonTapped("Completed Service: " + (issue.getAction() == null ? "" : (issue.getAction() + " ")) + issue.getItem()
                                                            + " " + timeCompleted, MixpanelHelper.DASHBOARD_VIEW);
                                                    networkHelper.serviceDone(dashboardCar.getId(), issue.getId(),
                                                            daysAgo, dashboardCar.getTotalMileage(), new RequestCallback() {
                                                                @Override
                                                                public void done(String response, RequestError requestError) {
                                                                    if (requestError == null) {
                                                                        Toast.makeText(getActivity(), "Issue cleared", Toast.LENGTH_SHORT).show();
                                                                        carIssueList.remove(i);
                                                                        carIssuesAdapter.notifyDataSetChanged();
                                                                        ((MainActivity) getActivity()).refreshFromServer();
                                                                    }
                                                                }
                                                            });
                                                }
                                            }
                                        },
                                        currentYear,
                                        currentMonth,
                                        currentDay
                                );

                                final View titleView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_custom_title_primary_dark, null);
                                ((TextView) titleView.findViewById(R.id.custom_title_text)).setText(R.string.dialog_clear_issue_title);

                                datePicker.setCustomTitle(titleView);

                                //Cancel the service completion
                                datePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        Toast.makeText(getActivity(), "Cancelled", Toast.LENGTH_SHORT).show();
                                        mixpanelHelper.trackButtonTapped("Nevermind, Did Not Complete Service: "
                                                + issue.getAction() + " " + issue.getItem(), MixpanelHelper.DASHBOARD_VIEW);

                                    }
                                });


                                datePicker.show();
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView
                                    , int[] reverseSortedPositions) {
                                onDismissedBySwipeLeft(recyclerView, reverseSortedPositions);
                            }
                        });

        recyclerView.addOnItemTouchListener(swipeTouchListener);
    }*/

}
