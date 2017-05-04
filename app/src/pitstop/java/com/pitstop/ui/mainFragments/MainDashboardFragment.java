package com.pitstop.ui.mainFragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.BuildConfig;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainDashboardFragment extends Fragment implements MainActivity.MainDashboardCallback {

    public static String TAG = MainDashboardFragment.class.getSimpleName();

    public final static String pfName = "com.pitstop.login.name";
    public final static String pfCodeForObjectID = "com.pitstop.login.objectID";
    public final static String pfCurrentCar = "ccom.pitstop.currentcar";
    public final static String pfShopName = "com.pitstop.shop.name";
    public final static String pfCodeForShopObjectID = "com.pitstop.shop.objectID";

    public final static int MSG_UPDATE_CONNECTED_CAR = 1076;

    // Views
    private CustomAdapter carIssuesAdapter;

    private View rootview;
/*    private RecyclerView carIssueListView;
    private CustomAdapter carIssuesAdapter;
    private RecyclerView.LayoutManager layoutManager;*/
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
    private LinearLayout requestServiceButton;
    private boolean dialogShowing = false;

    private AlertDialog updateMileageDialog;
    private AlertDialog uploadHistoricalDialog;
    private AlertDialog connectTimeoutDialog;

    @BindView(R.id.dealer_background_imageview)
    ImageView mDealerBanner;

    @BindView(R.id.banner_overlay)
    FrameLayout mDealerBannerOverlay;

    @BindView(R.id.car_logo_imageview)
    ImageView mCarLogoImage;

    @BindView(R.id.mileage_icon)
    ImageView mMileageIcon;

    @BindView(R.id.mileage)
    TextView mMileageText;

    @BindView(R.id.engine_icon)
    ImageView mEngineIcon;

    @BindView(R.id.engine)
    TextView mEngineText;

    @BindView(R.id.highway_icon)
    ImageView mHighwayIcon;

    @BindView(R.id.highway_mileage)
    TextView mHighwayText;

    @BindView(R.id.city_icon)
    ImageView mCityIcon;

    @BindView(R.id.city_mileage)
    TextView mCityText;

    @BindView(R.id.past_appts_icon)
    ImageView mPastApptsIcon;

    @BindView(R.id.request_appts_icon)
    ImageView mRequestApptsIcon;

    ProgressDialog progressDialog;

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
                    Log.d(TAG, "BluetoothAutoConnectState: " + autoConnectService.getState());
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
        ButterKnife.bind(this, rootview);
        setUpUIReferences();
/*        if (dashboardCar != null) {
            carName.setText(dashboardCar.getYear() + " "
                    + dashboardCar.getMake() + " "
                    + dashboardCar.getModel());
            setIssuesCount();*/
            setCarDetailsUI();
        //}
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCanceledOnTouchOutside(false);
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
        //carIssueList = ((MainActivity) getActivity()).getCarIssueList();
        //carIssuesAdapter = new CustomAdapter(dashboardCar, carIssueList, this.getActivity());

        // Local db adapters
        carLocalStore = new LocalCarAdapter(getActivity());
        carIssueLocalStore = new LocalCarIssueAdapter(getActivity());
        shopLocalStore = new LocalShopAdapter(getActivity());
        scannerLocalStore = new LocalScannerAdapter(getActivity());

        MainActivity.callback = this;
    }

    private void setUpUIReferences() {
        toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        //carIssueListView = (RecyclerView) rootview.findViewById(R.id.car_issues_list);
/*        carIssueListView.setLayoutManager(new LinearLayoutManager(getContext()));
        carIssueListView.setHasFixedSize(true);
        carIssuesAdapter = new CustomAdapter(dashboardCar, carIssueList, this.getActivity());
        carIssueListView.setAdapter(carIssuesAdapter);

        setSwipeDeleteListener(carIssueListView);*/

        carName = (TextView) rootview.findViewById(R.id.car_name);
        //serviceCountText = (TextView) rootview.findViewById(R.id.service_count_text);
        dealershipName = (TextView) rootview.findViewById(R.id.dealership_name);
        dealershipAddress = (TextView) rootview.findViewById(R.id.dealership_address);
        dealershipPhone = (TextView) rootview.findViewById(R.id.dealership_phone);
        //serviceCountBackground = (ImageView) rootview.findViewById(R.id.service_count_background);
        dealershipLayout = (LinearLayout) rootview.findViewById(R.id.dealership_info_layout);

        carScan = (RelativeLayout) rootview.findViewById(R.id.dashboard_car_scan_btn);
        addressLayout = (RelativeLayout) rootview.findViewById(R.id.address_layout);

        phoneNumberLayout = (RelativeLayout) rootview.findViewById(R.id.phone_layout);

        connectedCarIndicator = (ImageView) rootview.findViewById(R.id.car_connected_indicator_layout);

        requestServiceButton = (LinearLayout) rootview.findViewById(R.id.dashboard_request_service_btn);
    }

    private void updateConnectedCarIndicator(boolean isConnected) {
        if (isConnected) {
            /*connectedCarIndicator.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.device_connected_indicator));*/
            ((MainActivity)getActivity()).toggleConnectionStatusActionBar(true);
        } else {
            /*connectedCarIndicator.setImageDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.circle_indicator_stroke));*/
            ((MainActivity)getActivity()).toggleConnectionStatusActionBar(false);
        }
    }

    /**
     * Detect Swipes on each list item
     *
     * @param //carIssueListView
     */
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

    private void setIssuesCount() { // sets the number of active issues to display
        int total = dashboardCar.getActiveIssues().size();

        //serviceCountText.setText(String.valueOf(total));

        /*Drawable background = serviceCountBackground.getDrawable();
        GradientDrawable gradientDrawable = (GradientDrawable) background;

        if (total > 0) {
            gradientDrawable.setColor(Color.rgb(203, 77, 69));
        } else {
            gradientDrawable.setColor(Color.rgb(93, 172, 129));
        }*/
    }

    private void setDealership() {
        Dealership shop = dashboardCar.getDealership();
        if (shop == null) {
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

                            Dealership d = shopLocalStore.getDealership(dashboardCar.getId());

                            dashboardCar.setDealership(d);
                            if (dashboardCar.getDealership() != null) {
                                bindDealerInfo(d);

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
                bindDealerInfo(shop);
            }
        }
    }

    private void bindDealerInfo(Dealership dealership) {
        dealershipName.setText(dealership.getName());
        dealershipAddress.setText(dealership.getAddress());
        dealershipPhone.setText(dealership.getPhone());
        setDealerVisuals(dealership);
/*        mDealerBanner.setImageResource(R.drawable.mercedes_brampton);
        mMileageIcon.setImageResource(R.drawable.mercedes_mileage);
        mEngineIcon.setImageResource(R.drawable.mercedes_engine);
        mHighwayIcon.setImageResource(R.drawable.mercedes_h);
        mCityIcon.setImageResource(R.drawable.mercedes_c);
        mPastApptsIcon.setImageResource(R.drawable.mercedes_book);
        mRequestApptsIcon.setImageResource(R.drawable.mercedes_book);*/
    }

    private void setDealerVisuals(Dealership dealership) {
        if (BuildConfig.DEBUG && (dealership.getId() == 4 || dealership.getId() == 18)){
            bindMercedesDealerUI();
        } else if (!BuildConfig.DEBUG && dealership.getId() == 14){
            bindMercedesDealerUI();
        } else {
            mDealerBanner.setImageResource(getDealerSpecificBanner(dealership.getName()));
            mMileageIcon.setImageResource(R.drawable.odometer);
            mEngineIcon.setImageResource(R.drawable.car_engine);
            mHighwayIcon.setImageResource(R.drawable.highwaymileage);
            mCityIcon.setImageResource(R.drawable.citymileage);
            mPastApptsIcon.setImageResource(R.drawable.mercedes_book);
            mRequestApptsIcon.setImageResource(R.drawable.request_service_dashboard);
            ((MainActivity)getActivity()).changeTheme(false);
            mCarLogoImage.setVisibility(View.VISIBLE);
            dealershipName.setVisibility(View.VISIBLE);
            carName.setTextColor(Color.BLACK);
            dealershipName.setTextColor(Color.BLACK);
            carName.setTypeface(Typeface.DEFAULT_BOLD);
            carName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            mDealerBannerOverlay.setVisibility(View.VISIBLE);

        }
    }

    private void bindMercedesDealerUI() {
        mDealerBanner.setImageResource(R.drawable.mercedes_brampton);
        mMileageIcon.setImageResource(R.drawable.mercedes_mileage);
        mEngineIcon.setImageResource(R.drawable.mercedes_engine);
        mHighwayIcon.setImageResource(R.drawable.mercedes_h);
        mCityIcon.setImageResource(R.drawable.mercedes_c);
        mPastApptsIcon.setImageResource(R.drawable.mercedes_book);
        mRequestApptsIcon.setImageResource(R.drawable.mercedes_request_service);
        ((MainActivity)getActivity()).changeTheme(true);
        mCarLogoImage.setVisibility(View.GONE);
        dealershipName.setVisibility(View.GONE);
        carName.setTextColor(Color.WHITE);
        carName.setTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/mercedes.otf"));
        mDealerBannerOverlay.setVisibility(View.GONE);
        carName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
    }

    private int getDealerSpecificBanner(String name) {
        if (name.equalsIgnoreCase("Waterloo Dodge")) {
            return R.drawable.waterloo_dodge;
        }else if (name.equalsIgnoreCase("Galt Chrysler")) {
            return R.drawable.galt_chrysler;
        }else if (name.equalsIgnoreCase("GBAutos")) {
            return R.drawable.gbautos;
        }else if (name.equalsIgnoreCase("Cambridge Toyota")) {
            return R.drawable.cambridge_toyota;
        }else if (name.equalsIgnoreCase("Bay King Chrysler")) {
            return R.drawable.bay_king_chrysler;
        }else if (name.equalsIgnoreCase("Willowdale Subaru")) {
            return R.drawable.willowdale_subaru;
        }else if (name.equalsIgnoreCase("Parkway Ford")) {
            return R.drawable.parkway_ford;
        }else if (name.equalsIgnoreCase("Mountain Mitsubishi")) {
            return R.drawable.mountain_mitsubishi;
        }else if (name.equalsIgnoreCase("Subaru Of Maple")) {
            return R.drawable.subaru_maple;
        }else if (name.equalsIgnoreCase("Village Ford")) {
            return R.drawable.villageford;
        }else if (name.equalsIgnoreCase("Maple Volkswagen")) {
            return R.drawable.maple_volkswagon;
        }else if (name.equalsIgnoreCase("Toronto North Mitsubishi")) {
            return R.drawable.torontonorth_mitsubishi;
        }else if (name.equalsIgnoreCase("Kia Of Richmondhill")) {
            return R.drawable.kia_richmondhill;
        }else if (name.equalsIgnoreCase("Mercedes Benz Brampton")) {
            return R.drawable.mercedesbenz_brampton;
        }else if (name.equalsIgnoreCase("401DixieKia")) {
            return R.drawable.dixie_king;
        }else if (name.equalsIgnoreCase("Cambridge Honda")) {
            return R.drawable.cambridge_honda;
        } else{
            return R.drawable.no_dealership_background;
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
                            //carIssuesAdapter.notifyDataSetChanged();
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
            //carIssuesAdapter.notifyDataSetChanged();
        }
        //carIssuesAdapter.updateTutorial();
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
            //carIssueList = ((MainActivity) getActivity()).getCarIssueList();
        }
    }

    @Override
    public void onLocalRefreshed() {
        if (getActivity() != null) {
            //carIssueList = ((MainActivity) getActivity()).getCarIssueList();
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

/*        carIssuesAdapter = new CustomAdapter(dashboardCar, carIssueList, this.getActivity());
        if (carIssueListView != null)
            carIssueListView.setAdapter(carIssuesAdapter);*/
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
/*        carIssuesAdapter = new CustomAdapter(dashboardCar, carIssueList, this.getActivity());
        if (carIssueListView != null)
            carIssueListView.setAdapter(carIssuesAdapter);*/

        mMileageText.setText(String.valueOf(dashboardCar.getDisplayedMileage()) + " km");
        mEngineText.setText(dashboardCar.getEngine());
        mHighwayText.setText(dashboardCar.getHighwayMileage());
        mCityText.setText(dashboardCar.getCityMileage());
        mCarLogoImage.setImageResource(getCarSpecificLogo(dashboardCar.getMake()));

    }

    private int getCarSpecificLogo(String make) {
        if (make.equalsIgnoreCase("abarth")){
            return R.drawable.abarth;
        }else if (make.equalsIgnoreCase("acura")){
            return R.drawable.acura;
        }else if (make.equalsIgnoreCase("alfa romeo")){
            return 0;
        }else if (make.equalsIgnoreCase("aston martin")){
            return R.drawable.aston_martin;
        }else if (make.equalsIgnoreCase("audi")){
            return R.drawable.audi;
        }else if (make.equalsIgnoreCase("bentley")){
            return R.drawable.bentley;
        }else if (make.equalsIgnoreCase("bmw")){
            return R.drawable.bmw;
        }else if (make.equalsIgnoreCase("buick")){
            return R.drawable.buick;
        }else if (make.equalsIgnoreCase("cadillac")){
            return R.drawable.cadillac;
        }else if (make.equalsIgnoreCase("chevrolet")){
            return R.drawable.chevrolet;
        }else if (make.equalsIgnoreCase("chrysler")){
            return R.drawable.chrysler;
        }else if (make.equalsIgnoreCase("dodge")){
            return R.drawable.dodge;
        }else if (make.equalsIgnoreCase("ferrari")){
            return R.drawable.ferrari;
        }else if (make.equalsIgnoreCase("fiat")){
            return R.drawable.fiat;
        }else if (make.equalsIgnoreCase("ford")){
            return R.drawable.ford;
        }else if (make.equalsIgnoreCase("gmc")){
            return R.drawable.gmc;
        }else if (make.equalsIgnoreCase("honda")){
            return R.drawable.honda;
        }else if (make.equalsIgnoreCase("hummer")){
            return R.drawable.hummer;
        }else if (make.equalsIgnoreCase("hyundai")){
            return R.drawable.hyundai;
        }else if (make.equalsIgnoreCase("infiniti")){
            return R.drawable.infiniti;
        }else if (make.equalsIgnoreCase("jaguar")){
            return R.drawable.jaguar;
        }else if (make.equalsIgnoreCase("jeep")){
            return R.drawable.jeep;
        }else if (make.equalsIgnoreCase("kia")){
            return R.drawable.kia;
        }else if (make.equalsIgnoreCase("landrover")){
            return 0;//R.drawable.landrover;
        }else if (make.equalsIgnoreCase("lexus")){
            return R.drawable.lexus;
        }else if (make.equalsIgnoreCase("lincoln")){
            return R.drawable.lincoln;
        }else if (make.equalsIgnoreCase("maserati")){
            return R.drawable.maserati;
        }else if (make.equalsIgnoreCase("mazda")){
            return R.drawable.mazda;
        }else if (make.equalsIgnoreCase("mercedes-benz")){
            return R.drawable.mercedes;
        }else if (make.equalsIgnoreCase("mercury")){
            return R.drawable.mercury;
        }else if (make.equalsIgnoreCase("mini")){
            return R.drawable.mini;
        }else if (make.equalsIgnoreCase("mitsubishi")){
            return R.drawable.mitsubishi;
        }else if (make.equalsIgnoreCase("nissan")){
            return R.drawable.nissan;
        }else if (make.equalsIgnoreCase("pontiac")){
            return R.drawable.pontiac;
        }else if (make.equalsIgnoreCase("porsche")){
            return R.drawable.porsche;
        }else if (make.equalsIgnoreCase("ram")){
            return R.drawable.ram;
        }else if (make.equalsIgnoreCase("saab")){
            return R.drawable.saab;
        }else if (make.equalsIgnoreCase("saturn")){
            return R.drawable.saturn;
        }else if (make.equalsIgnoreCase("scion")){
            return R.drawable.scion;
        }else if (make.equalsIgnoreCase("skota")){
            return 0;//R.drawable.skota;
        }else if (make.equalsIgnoreCase("smart")){
            return R.drawable.smart;
        }else if (make.equalsIgnoreCase("subaru")){
            return R.drawable.subaru;
        }else if (make.equalsIgnoreCase("suzuki")){
            return R.drawable.suzuki;
        }else if (make.equalsIgnoreCase("toyota")){
            return R.drawable.toyota;
        }else if (make.equalsIgnoreCase("volkswagen")){
            return R.drawable.volkswagen;
        }else if (make.equalsIgnoreCase("volvo")){
            return R.drawable.volvo;
        }else{
            return 0;
        }
    }


    @Override
    public void removeTutorial() {
        Log.d(TAG, "Remove tutorial");
/*        if (carIssuesAdapter != null) {
            carIssuesAdapter.removeTutorial();
        }*/
    }

    @Override
    public void tripData(TripInfoPackage tripInfoPackage) {
        if (tripInfoPackage.flag == TripInfoPackage.TripFlag.UPDATE) { // live mileage update
            final double newTotalMileage = ((int) ((dashboardCar.getTotalMileage() + tripInfoPackage.mileage) * 100)) / 100.0; // round to 2 decimal places

            Log.v(TAG, "Mileage updated: tripMileage: " + tripInfoPackage.mileage + ", baseMileage: " + dashboardCar.getTotalMileage() + ", newMileage: " + newTotalMileage);

            if (dashboardCar.getDisplayedMileage() < newTotalMileage) {
                dashboardCar.setDisplayedMileage(newTotalMileage);
                carLocalStore.updateCar(dashboardCar);
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMileageText.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.mileage_update));
                    mMileageText.setText(String.valueOf(newTotalMileage));
                }
            });

        } else if (tripInfoPackage.flag == TripInfoPackage.TripFlag.END) { // uploading historical data
            dashboardCar = carLocalStore.getCar(dashboardCar.getId());
            final double newBaseMileage = dashboardCar.getTotalMileage();
            //mCallback.onTripMileageUpdated(newBaseMileage);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMileageText.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.mileage_update));
                    mMileageText.setText(String.valueOf(newBaseMileage));
                }
            });
        }
    }

    /**
     * Issues list view
     */
    private static class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

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
                        //intent.putExtra(MainActivity.CAR_ISSUE_EXTRA, carIssue);

                        //activity.startActivityForResult(intent, MainActivity.RC_DISPLAY_ISSUE);
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

        public void removeTutorial() {
            if (activityReference.get() == null) return;
            GlobalApplication application = (GlobalApplication) activityReference.get().getApplicationContext();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(application);
            Set<String> carsAwaitTutorial = preferences.getStringSet(application.getString(R.string.pfAwaitTutorial), new HashSet<String>());
            Set<String> copy = new HashSet<>(); // The set returned by preference is immutable
            for (String item : carsAwaitTutorial) {
                if (!item.equals(String.valueOf(dashboardCar.getId()))) {
                    copy.add(item);
                }
            }
            Log.d(TAG, String.valueOf(dashboardCar.getId()));
            Log.d(TAG, String.valueOf(copy.size()));
            preferences.edit().putStringSet(application.getString(R.string.pfAwaitTutorial), copy).apply();

            for (int index = 0; index < carIssues.size(); index++) {
                CarIssue issue = carIssues.get(index);
                if (issue.getIssueType().equals(CarIssue.TENTATIVE)) {
                    carIssues.remove(index);
                    new LocalCarIssueAdapter(application).deleteCarIssue(issue);
                    notifyDataSetChanged();
                }
            }
        }

        private void addTutorial() {
            Log.d(TAG, "Create fsb row");
            if (activityReference.get() == null) return;
            GlobalApplication application = (GlobalApplication) activityReference.get().getApplicationContext();
            if (hasTutorial()) return;
            CarIssue tutorial = new CarIssue.Builder()
                    .setId(-1)
                    .setPriority(99)
                    .setIssueType(CarIssue.TENTATIVE)
                    .build();
            carIssues.add(0, tutorial);
            new LocalCarIssueAdapter(application).storeCarIssue(tutorial);
            notifyDataSetChanged();
        }

        private boolean hasTutorial() {
            for (CarIssue issue : carIssues) {
                if (issue.getIssueType().equals(CarIssue.TENTATIVE)) {
                    return true;
                }
            }
            return false;
        }

        public void updateTutorial() {
            if (activityReference.get() == null) return;

            GlobalApplication application = (GlobalApplication) activityReference.get().getApplicationContext();

            try {
                Set<String> carsAwaitTutorial = PreferenceManager.getDefaultSharedPreferences(application)
                        .getStringSet(application.getString(R.string.pfAwaitTutorial), new HashSet<String>());
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
        public static class ViewHolder extends RecyclerView.ViewHolder {
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

    @OnClick(R.id.dashboard_request_service_btn)
    protected void onServiceRequestButtonClicked(){
        ((MainActivity)getActivity()).requestMultiService(null);
    }

    @OnClick(R.id.mileage_container)
    protected void onMileageClicked(){
/*        if (isFinishing() || isDestroyed() || (updateMileageDialog != null && updateMileageDialog.isShowing())) {
            return;
        }*/

        if (updateMileageDialog != null && updateMileageDialog.isShowing())
            return;

        final View dialogLayout = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_input_mileage, null);
        final TextInputEditText input = (TextInputEditText) dialogLayout.findViewById(R.id.mileage_input);
        input.setText(mMileageText.getText().toString().split(" ")[0]);

        if (updateMileageDialog == null) {
            updateMileageDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Update Mileage")
                    .setView(dialogLayout)
                    .setPositiveButton("Confirm", null)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();

            updateMileageDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(final DialogInterface d) {
                    updateMileageDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mixpanelHelper.trackButtonTapped(MixpanelHelper.SCAN_CAR_CONFIRM_SCAN, MixpanelHelper.SCAN_CAR_VIEW);
                            // POST (entered mileage - the trip mileage) so (mileage in backend + trip mileage) = entered mileage
                            final double mileage = Double.parseDouble(input.getText().toString()) - (dashboardCar.getDisplayedMileage() - dashboardCar.getTotalMileage());
                            if (mileage > 20000000) {
                                Toast.makeText(getActivity(), "Please enter valid mileage", Toast.LENGTH_SHORT).show();
                            } else {
                                d.dismiss();
                                ((MainActivity)getActivity()).getBluetoothConnectService().manuallyUpdateMileage = true;
                                showLoading("Updating Mileage...");
                                networkHelper.updateCarMileage(dashboardCar.getId(), mileage, new RequestCallback() {
                                    @Override
                                    public void done(String response, RequestError requestError) {
                                        hideLoading("Mileage updated!");
                                        if (requestError != null) {
                                            hideLoading(requestError.getMessage());
                                            return;
                                        }

                                        /*
                                        * Ask Ben why this updateMileageStart is being called here
                                        * */
                                        if (((MainActivity)getActivity()).getBluetoothConnectService().getState() == BluetoothCommunicator.CONNECTED && ((MainActivity)getActivity()).getBluetoothConnectService().getLastTripId() != -1){
                                            networkHelper.updateMileageStart(mileage, ((MainActivity)getActivity()).getBluetoothConnectService().getLastTripId(), null);
                                        }

                                        dashboardCar.setDisplayedMileage(mileage);
                                        dashboardCar.setTotalMileage(mileage);
                                        carLocalStore.updateCar(dashboardCar);
                                        //mCallback.onInputtedMileageUpdated(mileage);
                                        if (IBluetoothCommunicator.CONNECTED == ((MainActivity)getActivity()).getBluetoothConnectService().getState()
                                                || ((MainActivity)getActivity()).getBluetoothConnectService().isCommunicatingWithDevice()) {
                                            mMileageText.setText(String.format("%.2f", mileage));
                                            ((MainActivity)getActivity()).getBluetoothConnectService();
                                        } else {
                                            if (((MainActivity)getActivity()).getBluetoothConnectService().getState() == IBluetoothCommunicator.CONNECTED||
                                                    ((MainActivity)getActivity()).getBluetoothConnectService().isCommunicatingWithDevice())
                                                ((MainActivity)getActivity()).getBluetoothConnectService().startBluetoothSearch();
                                                //connectToDevice();
                                        }
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }
        updateMileageDialog.show();
    }

    private void showLoading(String loadingMessage) {
        if (progressDialog != null) {
            if (loadingMessage != null)
                progressDialog.setMessage(loadingMessage);
            progressDialog.show();
        }
    }

    private void hideLoading(String toastMessage) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            if (toastMessage != null)
                Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_SHORT).show();
        }
    }
}