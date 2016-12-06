package com.pitstop.ui.mainFragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.MainActivity;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.models.Dealership;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.issue_detail.IssueDetailsActivity;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainDashboardFragment extends Fragment implements MainActivity.MainDashboardCallback {

    public static String TAG = MainDashboardFragment.class.getSimpleName();

    public final static String pfName = "com.pitstop.login.name";
    public final static String pfCodeForObjectID = "com.pitstop.login.objectID";
    public final static String pfCurrentCar = "ccom.pitstop.currentcar";
    public final static String pfShopName = "com.pitstop.shop.name";
    public final static String pfCodeForShopObjectID = "com.pitstop.shop.objectID";

    public final static int MSG_UPDATE_CONNECTED_CAR = 1076;

    // Views
    private View rootview;
    private RecyclerView carIssueListView;
    private CustomAdapter carIssuesAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ImageView connectedCarIndicator;
    private ImageView serviceCountBackground;
    private LinearLayout dealershipLayout;
    private TextView dealershipAddress;
    private TextView dealershipPhone;
    private RelativeLayout addressLayout, phoneNumberLayout;
    private Toolbar toolbar;
    private TextView serviceCountText;
    private TextView carName, dealershipName;
    private RelativeLayout carScan;
    private Button requestServiceButton;
    private boolean dialogShowing = false;

    // Models
    private Car dashboardCar;
    private List<CarIssue> carIssueList = new ArrayList<>();

    // Database accesses
    private LocalCarAdapter carLocalStore;
    private LocalCarIssueAdapter carIssueLocalStore;
    private LocalShopAdapter shopLocalStore;
    private LocalScannerAdapter scannerLocalStore;

    private GlobalApplication application;
    private SharedPreferences sharedPreferences;

    // Utils / Helper
    private NetworkHelper networkHelper;
    private MixpanelHelper mixpanelHelper;

    private boolean askForCar = true; // do not ask for car if user presses cancel

    /**
     * Monitor app connection to device, so that ui can be updated
     * appropriately.
     */
    public Runnable carConnectedRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Scan for cars in MainDashboardFragment");
            handler.sendEmptyMessage(MSG_UPDATE_CONNECTED_CAR);
        }
    };

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (getActivity() == null) {
                return;
            }
            final BluetoothAutoConnectService autoConnectService = ((MainActivity) getActivity()).getBluetoothConnectService();

            switch (msg.what) {
                case MSG_UPDATE_CONNECTED_CAR:
                    Log.d(TAG, "Msg0, BluetoothAutoConnectState: " + autoConnectService.getState());
                    if (autoConnectService != null
                            && autoConnectService.getState() == IBluetoothCommunicator.CONNECTED
                            && dashboardCar != null
                            && dashboardCar.getScannerId() != null
                            && dashboardCar.getScannerId()
                            .equals(autoConnectService.getCurrentDeviceId())) {

                        updateConnectedCarIndicator(true);
                    } else {
                        updateConnectedCarIndicator(false);
                    }
                    // See if we are connected every 2 seconds
                    handler.postDelayed(carConnectedRunnable, 2000);
                    break;
            }

        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_main_dashboard, null);
        setUpUIReferences();
        if (dashboardCar != null) {
            carName.setText(dashboardCar.getYear() + " "
                    + dashboardCar.getMake() + " "
                    + dashboardCar.getModel());
            setIssuesCount();
            setCarDetailsUI();
        }
        return rootview;
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(carConnectedRunnable, 1000);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(carConnectedRunnable);
        application.getMixpanelAPI().flush();
        super.onPause();
    }

    @Override
    public void onAttach(Context context) {
        //Log.w(TAG, "onAttach");
        super.onAttach(context);

        application = (GlobalApplication) getActivity().getApplicationContext();
        networkHelper = new NetworkHelper(application);
        mixpanelHelper = new MixpanelHelper(application);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        carIssueList = ((MainActivity) getActivity()).getCarIssueList();
        carIssuesAdapter = new CustomAdapter(carIssueList);

        // Local db adapters
        carLocalStore = new LocalCarAdapter(getActivity());
        carIssueLocalStore = new LocalCarIssueAdapter(getActivity());
        shopLocalStore = new LocalShopAdapter(getActivity());
        scannerLocalStore = new LocalScannerAdapter(getActivity());

        MainActivity.callback = this;
    }

    private void setUpUIReferences() {
        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        carIssueListView = (RecyclerView) rootview.findViewById(R.id.car_issues_list);
        carIssueListView.setLayoutManager(new LinearLayoutManager(getContext()));
        carIssueListView.setHasFixedSize(true);
        carIssuesAdapter = new CustomAdapter(carIssueList);
        carIssueListView.setAdapter(carIssuesAdapter);

        setSwipeDeleteListener(carIssueListView);

        carName = (TextView) rootview.findViewById(R.id.car_name);
        serviceCountText = (TextView) rootview.findViewById(R.id.service_count_text);
        dealershipName = (TextView) rootview.findViewById(R.id.dealership_name);
        dealershipAddress = (TextView) rootview.findViewById(R.id.dealership_address);
        dealershipPhone = (TextView) rootview.findViewById(R.id.dealership_phone);
        serviceCountBackground = (ImageView) rootview.findViewById(R.id.service_count_background);
        dealershipLayout = (LinearLayout) rootview.findViewById(R.id.dealership_info_layout);

        carScan = (RelativeLayout) rootview.findViewById(R.id.dashboard_car_scan_btn);
        addressLayout = (RelativeLayout) rootview.findViewById(R.id.address_layout);

        phoneNumberLayout = (RelativeLayout) rootview.findViewById(R.id.phone_layout);

        connectedCarIndicator = (ImageView) rootview.findViewById(R.id.car_connected_indicator_layout);

        requestServiceButton = (Button) rootview.findViewById(R.id.dashboard_request_service_btn);
    }

    private void updateConnectedCarIndicator(boolean isConnected) {
        if (isConnected) {
            connectedCarIndicator.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.severity_low_indicator));
        } else {
            connectedCarIndicator.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_indicator_stroke));
        }
    }

    /**
     * Detect Swipes on each list item
     *
     * @param //carIssueListView
     */
    private void setSwipeDeleteListener(RecyclerView recyclerView) {
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

                                final String[] timeCompleted = new String[1];

                                final int[] daysAgo = new int[1];

                                //Swipe to start deleting(completing) the selected issue
                                try {
                                    mixpanelHelper.trackButtonTapped("Done " + issue.getAction() + " " + issue.getItem(), MixpanelHelper.DASHBOARD_VIEW);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

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

                                                    daysAgo[0] = (int) TimeUnit.MILLISECONDS.toDays(currentTime - calendar.getTimeInMillis());

                                                    if (daysAgo[0] < 13) { // approximate categorization of the time service was completed
                                                        timeCompleted[0] = "Recently";
                                                    } else if (daysAgo[0] < 28) {
                                                        timeCompleted[0] = "2 Weeks Ago";
                                                    } else if (daysAgo[0] < 56) {
                                                        timeCompleted[0] = "1 Month Ago";
                                                    } else if (daysAgo[0] < 170) {
                                                        timeCompleted[0] = "2 to 3 Months Ago";
                                                    } else {
                                                        timeCompleted[0] = "6 to 12 Months Ago";
                                                    }
                                                }
                                            }
                                        },
                                        currentYear,
                                        currentMonth,
                                        currentDay
                                );

                                try {
                                    datePicker.getWindow().setWindowAnimations(AnimatedDialogBuilder.ANIMATION_GROW);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                final View titleView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_custom_title_primary_dark, null);
                                ((TextView) titleView.findViewById(R.id.custom_title_text)).setText(R.string.dialog_clear_issue_title);

                                datePicker.setCustomTitle(titleView);

                                datePicker.setButton(DialogInterface.BUTTON_POSITIVE, "Confirm", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            mixpanelHelper.trackButtonTapped("Completed Service: " +
                                                    (issue.getAction() == null ? "" : (issue.getAction() + " ")) +
                                                    issue.getItem() + " " + timeCompleted[0], MixpanelHelper.DASHBOARD_VIEW);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        networkHelper.serviceDone(dashboardCar.getId(), issue.getId(),
                                                daysAgo[0], dashboardCar.getTotalMileage(), new RequestCallback() {
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
                                });

                                datePicker.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(getActivity(), "Cancelled", Toast.LENGTH_SHORT).show();
                                        try {
                                            mixpanelHelper.trackButtonTapped("Nevermind, Did Not Complete Service: "
                                                    + issue.getAction() + " " + issue.getItem(), MixpanelHelper.DASHBOARD_VIEW);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });

                                //Cancel the service completion
                                datePicker.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        Toast.makeText(getActivity(), "Cancelled", Toast.LENGTH_SHORT).show();
                                        try {
                                            mixpanelHelper.trackButtonTapped("Nevermind, Did Not Complete Service: "
                                                    + issue.getAction() + " " + issue.getItem(), MixpanelHelper.DASHBOARD_VIEW);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
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
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void setIssuesCount() { // sets the number of active issues to display
        int total = dashboardCar.getActiveIssues().size();

        serviceCountText.setText(String.valueOf(total));

        Drawable background = serviceCountBackground.getDrawable();
        GradientDrawable gradientDrawable = (GradientDrawable) background;

        if (total > 0) {
            gradientDrawable.setColor(Color.rgb(203, 77, 69));
        } else {
            gradientDrawable.setColor(Color.rgb(93, 172, 129));
        }
    }

    private void setDealership() {
        Dealership shop = dashboardCar.getDealership();
        if (shop == null) {
//            shop = shopLocalStore.getDealership(carLocalStore.getCar(dashboardCar.getId()).getShopId());
            shop = shopLocalStore.getDealership(dashboardCar.getShopId());
        }
        dashboardCar.setDealership(shop);
        if (shop == null) {
            networkHelper.getShops(new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        try {
                            List<Dealership> dl = Dealership.createDealershipList(response);
                            shopLocalStore.deleteAllDealerships();
                            shopLocalStore.storeDealerships(dl);

//                            Dealership d = shopLocalStore.getDealership(carLocalStore.getCar(dashboardCar.getId()).getShopId());
                            Dealership d = shopLocalStore.getDealership(dashboardCar.getId());

                            dashboardCar.setDealership(d);
                            if (dashboardCar.getDealership() != null) {
                                dealershipName.setText(dashboardCar.getDealership().getName());
                                dealershipAddress.setText(dashboardCar.getDealership().getAddress());
                                dealershipPhone.setText(dashboardCar.getDealership().getPhone());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e(TAG, "Get shops: " + requestError.getMessage());
                    }
                }
            });
        } else {
            if (dealershipName != null) {
                dealershipName.setText(shop.getName());
                dealershipAddress.setText(shop.getAddress());
                dealershipPhone.setText(shop.getPhone());
            }
        }

    }

    private void populateCarIssuesAdapter() {
        // Try local store
        Log.i(TAG, "DashboardCar id: (Try local store) "+dashboardCar.getId());
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
                            setIssuesCount();
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
        carIssuesAdapter.updateTutorial();
    }

    // From MainActivity.MainDashboardCallback

    @Override
    public void activityResultCallback(int requestCode, int resultCode, Intent data) {
        boolean shouldRefreshFromServer = data.getBooleanExtra(MainActivity.REFRESH_FROM_SERVER, false);

        if (requestCode == MainActivity.RC_ADD_CAR && resultCode == AddCarActivity.ADD_CAR_SUCCESS) {

            getActivity().findViewById(R.id.no_car_text).setVisibility(View.GONE);

            if (shouldRefreshFromServer) {
                dashboardCar = data.getParcelableExtra(MainActivity.CAR_EXTRA);
                sharedPreferences.edit().putInt(pfCurrentCar, dashboardCar.getId()).commit();
            }
        }

    }

    @Override
    public void onServerRefreshed() {
        if (getActivity() != null) {
            carIssueList = ((MainActivity) getActivity()).getCarIssueList();
        }
    }

    @Override
    public void onLocalRefreshed() {
        if (getActivity() != null) {
            carIssueList = ((MainActivity) getActivity()).getCarIssueList();
        }
    }

    @Override
    public void setDashboardCar(List<Car> carList) {
        if (getActivity() == null) {
            return;
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int currentCarId = sharedPreferences.getInt(pfCurrentCar, -1);

        for (Car car : carList) {
            if (car.getId() == currentCarId) {
                dashboardCar = car;
                return;
            }
        }
        dashboardCar = carList.get(0);
    }

    /**
     * Update ui with current car info
     * And retrieve available car issues
     */
    @Override
    public void setCarDetailsUI() {
        if (dashboardCar == null) {
            return;
        }
        setDealership();
        populateCarIssuesAdapter();

        if (carName != null) {
            carName.setText(dashboardCar.getYear() + " "
                    + dashboardCar.getMake() + " "
                    + dashboardCar.getModel());
            setIssuesCount();
        }
    }


    @Override
    public void removeTutorial() {
        Log.d(TAG, "Remove tutorial");
        if (carIssuesAdapter != null) {
            carIssuesAdapter.removeTutorial();
        }
    }

    /**
     * Issues list view
     */
    private class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        private List<CarIssue> carIssueList;
        static final int VIEW_TYPE_EMPTY = 100;
        static final int VIEW_TYPE_TENTATIVE = 101;

        public CustomAdapter(List<CarIssue> carIssues) {
            carIssueList = carIssues;
            Log.d(TAG, "Car issue list size: " + carIssueList.size());
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_issue, parent, false);
            return new ViewHolder(v);
        }

        public CarIssue getItem(int position) {
            return carIssueList.get(position);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            //Log.i(TAG,"On bind view holder");

            int viewType = getItemViewType(position);

            holder.date.setVisibility(View.GONE);

            if (viewType == VIEW_TYPE_EMPTY) {
                holder.description.setMaxLines(2);
                holder.description.setText("You have no pending Engine Code, Recalls or Services");
                holder.title.setText("Congrats!");
                holder.imageView.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.ic_check_circle_green_400_36dp));
            } else if (viewType == VIEW_TYPE_TENTATIVE) {
                holder.description.setMaxLines(2);
                holder.description.setText("Tap to start");
                holder.title.setText("Book your first tentative service");
                holder.imageView.setImageDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.ic_announcement_blue_600_24dp));
                holder.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // removeTutorial();
                        ((MainActivity) getActivity()).prepareAndStartTutorialSequence();
                    }
                });
            } else {
                final CarIssue carIssue = carIssueList.get(position);

                holder.description.setText(carIssue.getDescription());
                holder.description.setEllipsize(TextUtils.TruncateAt.END);
                if (carIssue.getIssueType().equals(CarIssue.RECALL)) {
                    holder.imageView.setImageDrawable(ContextCompat
                            .getDrawable(getContext(), R.drawable.ic_error_red_600_24dp));

                } else if (carIssue.getIssueType().equals(CarIssue.DTC)) {
                    holder.imageView.setImageDrawable(ContextCompat
                            .getDrawable(getContext(), R.drawable.car_engine_red));

                } else if (carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                    holder.imageView.setImageDrawable(ContextCompat
                            .getDrawable(getContext(), R.drawable.car_engine_yellow));
                } else {
                    holder.description.setText(carIssue.getDescription());
                    holder.imageView.setImageDrawable(ContextCompat
                            .getDrawable(getContext(), R.drawable.ic_warning_amber_300_24dp));
                }

                holder.title.setText(String.format("%s %s", carIssue.getAction(), carIssue.getItem()));

                holder.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            mixpanelHelper.trackButtonTapped(carIssueList.get(position).getItem(), MixpanelHelper.DASHBOARD_VIEW);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Intent intent = new Intent(getActivity(), IssueDetailsActivity.class);
                        intent.putExtra(MainActivity.CAR_EXTRA, dashboardCar);
                        intent.putExtra(MainActivity.CAR_ISSUE_EXTRA, carIssue);

                        startActivityForResult(intent, MainActivity.RC_DISPLAY_ISSUE);
                    }
                });
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (carIssueList.isEmpty()) {
                return VIEW_TYPE_EMPTY;
            } else if (carIssueList.get(position).getIssueType().equals(CarIssue.TENTATIVE)) {
                return VIEW_TYPE_TENTATIVE;
            }
            return super.getItemViewType(position);
        }

        @Override
        public int getItemCount() {
            if (carIssueList.isEmpty()) {
                return 1;
            }
            return carIssueList.size();
        }

        public boolean removeTutorial() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
            Set<String> carsAwaitTutorial = preferences.getStringSet(getString(R.string.pfAwaitTutorial), new HashSet<String>());
            Set<String> copy = new HashSet<>(); // The set returned by preference is immutable
            for (String item : carsAwaitTutorial) {
                if (!item.equals(String.valueOf(dashboardCar.getId()))) {
                    copy.add(item);
                }
            }
            Log.d(TAG, String.valueOf(dashboardCar.getId()));
            Log.d(TAG, String.valueOf(copy.size()));
            preferences.edit().putStringSet(getString(R.string.pfAwaitTutorial), copy).apply();

            for (int index = 0; index < carIssueList.size(); index++) {
                CarIssue issue = carIssueList.get(index);
                if (issue.getIssueType().equals(CarIssue.TENTATIVE)) {
                    carIssueList.remove(index);
                    carIssueLocalStore.deleteCarIssue(issue);
                    notifyDataSetChanged();
                    return true;
                }
            }
            return false;
        }

        private boolean addTutorial() {
            Log.d(TAG, "Create fsb row");
            if (hasTutorial()) return false;
            CarIssue tutorial = new CarIssue.Builder()
                    .setId(-1)
                    .setPriority(99)
                    .setIssueType(CarIssue.TENTATIVE)
                    .build();
            carIssueList.add(0, tutorial);
            carIssueLocalStore.storeCarIssue(tutorial);
            notifyDataSetChanged();
            return true;
        }

        private boolean hasTutorial() {
            for (CarIssue issue : carIssueList) {
                if (issue.getIssueType().equals(CarIssue.TENTATIVE)) {
                    return true;
                }
            }
            return false;
        }

        public void updateTutorial() {
            try {
                Set<String> carsAwaitTutorial = PreferenceManager.getDefaultSharedPreferences(application)
                        .getStringSet(getString(R.string.pfAwaitTutorial), new HashSet<String>());
                Log.d(TAG, "Update tutorial: dashboard car: " + dashboardCar.getId());
                for (String item : carsAwaitTutorial) {
                    Log.d(TAG, "Cars await tutorial set: " + item);
                }
                if (carsAwaitTutorial.size() == 0) {
                    Log.d(TAG, "Cars await tutorial set is empty");
                }
                boolean needToShowTutorial = carsAwaitTutorial.contains(String.valueOf(dashboardCar.getId()));
                Log.d(TAG, "Need to show tutorial: " + needToShowTutorial);
                if (needToShowTutorial) {
                    addTutorial();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
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
}