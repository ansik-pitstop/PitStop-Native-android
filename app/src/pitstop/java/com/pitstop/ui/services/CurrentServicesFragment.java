package com.pitstop.ui.services;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
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
public class CurrentServicesFragment extends SubServiceFragment {

    public static final String TAG = CurrentServicesFragment.class.getSimpleName();

    @BindView(R.id.car_issues_list)
    protected RecyclerView carIssueListView;
    private CustomAdapter carIssuesAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private LocalCarIssueAdapter carIssueLocalStore;
    private LocalCarAdapter localCarStore;
    private NetworkHelper networkHelper;
    private List<CarIssue> carIssueList = new ArrayList<>();

    private static MainServicesCallback mainServicesCallback;

    public static CurrentServicesFragment newInstance(MainServicesCallback callback){
        CurrentServicesFragment fragment = new CurrentServicesFragment();
        mainServicesCallback = callback;
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

        //This must be called so that UI elements are set for SubService
        super.onCreateView(inflater,container,savedInstanceState);

        return view;
    }

    @Override
    public void setUI(){
        carIssuesAdapter = new CustomAdapter(dashboardCar, carIssueList, this.getActivity());
        carIssueListView.setLayoutManager(new LinearLayoutManager(getContext()));
        carIssueListView.setAdapter(carIssuesAdapter);
        populateCarIssuesAdapter();
    }

    private void populateCarIssuesAdapter() {
        // Try local store
//        Log.i(TAG, "DashboardCar id: (Try local store) "+ dashboardCar.getId());
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

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder>
        implements DatePickerDialog.OnDateSetListener{

        private WeakReference<Activity> activityReference;

        private View view;
        private DatePickerDialog datePickerDialog;

        private CarIssue currentlySelectedIssue;
        private ViewHolder currentlySelectedHolder;

        private LocalCarAdapter localCarStore;

        private Car dashboardCar;
        private List<CarIssue> carIssues;
        static final int VIEW_TYPE_EMPTY = 100;
        static final int VIEW_TYPE_TENTATIVE = 101;

        public CustomAdapter(Car dashboardCar, List<CarIssue> carIssues, Activity activity) {
            this.dashboardCar = dashboardCar;
            this.carIssues = carIssues;
            Log.d(TAG, "Car issue list size: " + this.carIssues.size());
            localCarStore = new LocalCarAdapter(activity);
            activityReference = new WeakReference<>(activity);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_issue, parent, false);

            return new ViewHolder(view);
        }

        private void initIssueDoneImageView(CarIssue issue, ViewHolder holder){
            //Set listener for closing issue
            final CustomAdapter thisInstance = this;
            holder.doneImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG,"Close issue clicked");
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setMessage("Are you sure you want to move this issue to history?")
                            .setTitle("Issue Done")
                            .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {

                                    currentlySelectedHolder = holder;
                                    currentlySelectedIssue = issue;

                                    //Begin date selection
                                    Calendar cal = Calendar.getInstance();
                                    datePickerDialog = new DatePickerDialog(getContext()
                                            ,thisInstance,cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH));
                                    datePickerDialog.setTitle("Please select the date when service was completed.");
                                    //datePickerDialog.updateDate(cal.get(Calendar.YEAR),cal.get(Calendar.MONTH),cal.get(Calendar.DAY_OF_MONTH));
                                    datePickerDialog.show();

                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    //Do nothing, close dialog
                                    dialog.dismiss();
                                }
                            });

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });
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
                holder.doneImageView.setVisibility(View.INVISIBLE);
            } else if (viewType == VIEW_TYPE_TENTATIVE) {
                holder.description.setMaxLines(2);
                holder.description.setText("Tap to start");
                holder.title.setText("Book your first tentative service");
                holder.imageView.setImageDrawable(
                        ContextCompat.getDrawable(activity, R.drawable.ic_announcement_blue_600_24dp));
                holder.doneImageView.setVisibility(View.INVISIBLE);
                holder.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // removeTutorial();
                        ((MainActivity)activity).prepareAndStartTutorialSequence();
                    }
                });
            } else {
                final CarIssue carIssue = carIssues.get(position);

                initIssueDoneImageView(carIssue,holder);

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

        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year,month,day);

            currentlySelectedIssue.setDoneAt(DateTimeFormatUtil.formatToISO8601(calendar)); //Update this with a proper date
            currentlySelectedIssue.setStatus(CarIssue.ISSUE_DONE);

            //Remove issue
            carIssueList.remove(currentlySelectedHolder.getAdapterPosition());
            notifyDataSetChanged();

            //Update backend and local storage
            int mileage = (int)(localCarStore.getCar(currentlySelectedIssue.getCarId())).getTotalMileage();
            int daysToday = (int)TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().getTimeInMillis());
            int serviceDay = (int)TimeUnit.MILLISECONDS.toDays(calendar.getTimeInMillis());
            int daysAgo = daysToday - serviceDay;

            networkHelper.serviceDone(currentlySelectedIssue.getCarId()
                    ,currentlySelectedIssue.getId(),daysAgo,mileage,null);
            carIssueLocalStore.updateCarIssue(currentlySelectedIssue);

            mainServicesCallback.onServiceDone(currentlySelectedIssue);
        }

        public  class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView title;
            public TextView description;
            public ImageView imageView;
            public ImageView doneImageView;
            public View container;
            public View date; // Not used here so it is set to GONE

            public ViewHolder(View v) {
                super(v);
                title = (TextView) v.findViewById(R.id.title);
                description = (TextView) v.findViewById(R.id.description);
                imageView = (ImageView) v.findViewById(R.id.image_icon);
                container = v.findViewById(R.id.list_car_item);
                date = v.findViewById(R.id.date);
                doneImageView = (ImageView)v.findViewById(R.id.image_done_issue);
            }
        }
    }

}
