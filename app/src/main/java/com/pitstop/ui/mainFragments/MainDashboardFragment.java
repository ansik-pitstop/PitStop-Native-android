package com.pitstop.ui.mainFragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.ui.AddCarActivity;
import com.pitstop.ui.MainActivity;
import com.pitstop.ui.CarScanActivity;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.models.Dealership;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.IssueDetailsActivity;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainDashboardFragment extends Fragment implements ObdManager.IBluetoothDataListener,
        MainActivity.MainDashboardCallback {



    public final static String pfName = "com.pitstop.login.name";
    private final static String pfTutorial = "com.pitstop.tutorial";
    public final static String pfCodeForObjectID = "com.pitstop.login.objectID";
    public final static String pfCurrentCar = "ccom.pitstop.currentcar";
    public final static String pfShopName = "com.pitstop.shop.name";
    public final static String pfCodeForShopObjectID = "com.pitstop.shop.objectID";


    private NetworkHelper networkHelper;

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


    private Car dashboardCar;
    private List<CarIssue> carIssueList = new ArrayList<>();

    private LocalCarAdapter carLocalStore;
    private LocalCarIssueAdapter carIssueLocalStore;
    private LocalShopAdapter shopLocalStore;

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    private SharedPreferences sharedPreferences;

    private boolean dialogShowing = false;

    public static String TAG = MainDashboardFragment.class.getSimpleName();

    private View rootview;

    private class CarListAdapter extends BaseAdapter {
        private List<Car> ownedCars;

        public CarListAdapter(List<Car> cars) {
            ownedCars = cars;
        }

        @Override
        public int getCount () {
            return ownedCars.size();
        }

        @Override
        public Object getItem (int position) {
            return ownedCars.get(position);
        }

        @Override
        public long getItemId (int position) {
            return 0;
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View rowView = convertView != null ? convertView :
                    inflater.inflate(android.R.layout.simple_list_item_single_choice, parent, false);
            Car ownedCar = (Car) getItem(position);

            TextView carName = (TextView) rowView.findViewById(android.R.id.text1);
            carName.setText(String.format("%s %s", ownedCar.getMake(), ownedCar.getModel()));
            return rowView;
        }
    }

    private boolean askForCar = true; // do not ask for car if user presses cancel

    /**
     * Monitor app connection to device, so that ui can be updated
     * appropriately.
     * */


    public Runnable carConnectedRunnable = new Runnable() {
        @Override
        public void run() {
            handler.sendEmptyMessage(0);
        }
    };
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(getActivity() == null) {
                return;
            }
            final BluetoothAutoConnectService autoConnectService=((MainActivity)getActivity()).getBluetoothConnectService();
            if(msg.what == 0) {
                if(autoConnectService != null
                        && autoConnectService.getState() == IBluetoothCommunicator.CONNECTED
                        && dashboardCar != null
                        && dashboardCar.getScannerId() != null
                        && dashboardCar.getScannerId()
                        .equals(autoConnectService.getCurrentDeviceId())) {

                    updateConnectedCarIndicator(true);

                } else if(autoConnectService != null
                        && autoConnectService.getState() == IBluetoothCommunicator.CONNECTED
                        && askForCar
                        && !dialogShowing
                        && dashboardCar != null
                        && (dashboardCar.getScannerId() == null || dashboardCar.getScannerId().isEmpty())
                        && autoConnectService.getCurrentDeviceId() != null
                        && carLocalStore.getCarByScanner(autoConnectService.getCurrentDeviceId()) == null) {

                    final ArrayList<Car> selectedCar = new ArrayList<>(1); // must be final because this is accessed in the inner class
                    final CarListAdapter carAdapter = new CarListAdapter(MainActivity.carList);

                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    dialog.setCancelable(false);
                    dialog.setTitle("New Module Detected. Please select the car this device is connected to.");
                    dialog.setSingleChoiceItems(carAdapter, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedCar.clear();
                            selectedCar.add((Car) carAdapter.getItem(which));
                        }
                    });
                    dialog.setNeutralButton("Add New Car", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(getActivity(), AddCarActivity.class);
                            getActivity().startActivityForResult(intent, MainActivity.RC_ADD_CAR);
                        }
                    });
                    dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            askForCar = false;
                            dialog.dismiss();
                        }
                    });
                    dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();

                            if (selectedCar.isEmpty()) {
                                return;
                            }

                            networkHelper.createNewScanner(selectedCar.get(0).getId(), autoConnectService.getCurrentDeviceId(),
                                    new RequestCallback() {
                                        @Override
                                        public void done(String response, RequestError requestError) {
                                            if(requestError == null) {
                                                Toast.makeText(getActivity(), "Device added successfully", Toast.LENGTH_SHORT).show();
                                                sharedPreferences.edit().putInt(pfCurrentCar, selectedCar.get(0).getId()).commit();
                                                ((MainActivity)getActivity()).refreshFromServer();
                                            } else {
                                                Toast.makeText(getActivity(), "An error occurred, please try again", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    });

                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialogShowing = false;
                        }
                    });

                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            dialogShowing = false;
                        }
                    });

                    dialog.show();
                    dialogShowing = true;

                } else {
                    updateConnectedCarIndicator(false);
                }
                handler.postDelayed(carConnectedRunnable, 2000);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        //Log.w(TAG, "onResume");
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
        carIssuesAdapter = new CustomAdapter(carIssueList);
        carIssueList = ((MainActivity)getActivity()).getCarIssueList();

        // Local db adapters
        carLocalStore = MainActivity.carLocalStore;
        carIssueLocalStore = MainActivity.carIssueLocalStore;
        shopLocalStore = MainActivity.shopLocalStore;
        MainActivity.callback = this;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_main_dashboard,null);
        setUpUIReferences();
        if(dashboardCar!=null) {
            carName.setText(dashboardCar.getYear() + " "
                    + dashboardCar.getMake() + " "
                    + dashboardCar.getModel());
            setIssuesCount();
            setCarDetailsUI();
        }
        return rootview;
    }




    /**
     * UI update methods
     */
    private void setUpUIReferences() {

        toolbar = (Toolbar)  getActivity().findViewById(R.id.toolbar);
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

        carScan = (RelativeLayout) rootview.findViewById(R.id.car_scan_btn);
        carScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mixpanelHelper.trackCustom("Button Tapped",
                            new JSONObject(String.format("{'Button':'Scan', 'View':'%s', 'Make':'%s', 'carModel':'%s', 'Device':'Android'}",
                                    TAG, dashboardCar.getMake(), dashboardCar.getModel())));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sharedPreferences.edit().putBoolean(MainActivity.REFRESH_FROM_SERVER, true).apply();

                Intent intent = new Intent(getActivity(), CarScanActivity.class);
                intent.putExtra(MainActivity.CAR_EXTRA, dashboardCar);
                getActivity().startActivityForResult(intent, MainActivity.RC_SCAN_CAR);
                getActivity().overridePendingTransition(R.anim.activity_slide_left_in, R.anim.activity_slide_left_out);
            }
        });

        addressLayout = (RelativeLayout) rootview.findViewById(R.id.address_layout);
        addressLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    mixpanelHelper.trackButtonTapped("Directions to " + dashboardCar.getDealership().getName(), TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String uri = String.format(Locale.ENGLISH,
                        "http://maps.google.com/maps?daddr=%s",
                        dashboardCar.getDealership().getAddress());
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        });

        phoneNumberLayout = (RelativeLayout) rootview.findViewById(R.id.phone_layout);
        phoneNumberLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mixpanelHelper.trackButtonTapped("Call " + dashboardCar.getDealership().getName(), TAG);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                        dashboardCar.getDealership().getPhone()));
                startActivity(intent);
            }
        });

        connectedCarIndicator = (ImageView) rootview.findViewById(R.id.car_connected_indicator_layout);
    }

    private void updateConnectedCarIndicator(boolean isConnected) {
        if(isConnected) {
            connectedCarIndicator.setImageDrawable(getResources().getDrawable(R.drawable.severity_low_indicator));
        } else {
            connectedCarIndicator.setImageDrawable(getResources().getDrawable(R.drawable.circle_indicator_stroke ));
        }
    }

    /**
     * Detect Swipes on each list item
     * @param //carIssueListView
     */
    private void setSwipeDeleteListener(RecyclerView recyclerView) {
        SwipeableRecyclerViewTouchListener swipeTouchListener =
                new SwipeableRecyclerViewTouchListener(recyclerView,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {

                            @Override
                            public boolean canSwipe(int position) {
                                if(carIssuesAdapter.getItemViewType(position)
                                        == CustomAdapter.VIEW_TYPE_EMPTY) {
                                    return false;
                                }

                                return true;
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

                                DatePickerDialog datePicker = new DatePickerDialog(getContext(),
                                        new DatePickerDialog.OnDateSetListener() {
                                            @Override
                                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                                if(year > currentYear || (year == currentYear
                                                        && (monthOfYear > currentMonth
                                                        || (monthOfYear == currentMonth && dayOfMonth > currentDay)))) {
                                                    Toast.makeText(getActivity(), "Please choose a date that has passed", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    long currentTime = calendar.getTimeInMillis();

                                                    calendar.set(year, monthOfYear, dayOfMonth);

                                                    String timeCompleted;

                                                    int daysAgo = (int) TimeUnit.MILLISECONDS.toDays(currentTime - calendar.getTimeInMillis());

                                                    if(daysAgo < 13) { // approximate categorization of the time service was completed
                                                        timeCompleted = "Recently";
                                                    } else if(daysAgo < 28) {
                                                        timeCompleted = "2 Weeks Ago";
                                                    } else if(daysAgo < 56) {
                                                        timeCompleted = "1 Month Ago";
                                                    } else if(daysAgo < 170) {
                                                        timeCompleted = "2 to 3 Months Ago";
                                                    } else {
                                                        timeCompleted = "6 to 12 Months Ago";
                                                    }

                                                    CarIssue carIssue = carIssuesAdapter.getItem(i);

                                                    try {
                                                        mixpanelHelper.trackButtonTapped("Completed Service: " +
                                                                (carIssue.getAction() == null ? "" : (carIssue.getAction() + " ")) +
                                                                carIssue.getItem() + " " + timeCompleted, TAG);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }

                                                    networkHelper.serviceDone(dashboardCar.getId(), carIssue.getId(),
                                                            daysAgo, dashboardCar.getTotalMileage(), new RequestCallback() {
                                                                @Override
                                                                public void done(String response, RequestError requestError) {
                                                                    if(requestError == null) {
                                                                        Toast.makeText(getActivity(), "Issue cleared", Toast.LENGTH_SHORT).show();
                                                                        carIssueList.remove(i);
                                                                        carIssuesAdapter.notifyDataSetChanged();
                                                                        ((MainActivity)getActivity()).refreshFromServer();
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
                                TextView titleView = new TextView(getActivity());
                                titleView.setText("When was this service completed?");
                                titleView.setBackgroundColor(getResources().getColor(R.color.primary_dark));
                                titleView.setTextColor(getResources().getColor(R.color.white_text));
                                titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                titleView.setTextSize(18);
                                titleView.setPadding(10,10,10,10);

                                datePicker.setCustomTitle(titleView);


                                datePicker.show();
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView
                                    , int[] reverseSortedPositions) {
                                onDismissedBySwipeLeft(recyclerView,reverseSortedPositions);
                            }
                        });

        recyclerView.addOnItemTouchListener(swipeTouchListener);
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }



    public void onServerRefreshed() {
        if(getActivity() != null) {
            carIssueList = ((MainActivity) getActivity()).getCarIssueList();
        }
    }

    public void onLocalRefreshed() {
        if(getActivity() != null) {
            carIssueList = ((MainActivity) getActivity()).getCarIssueList();
        }
    }


    /**
     * Update ui with current car info
     * And retrieve available car issues
     * */
    public void setCarDetailsUI() {
        if (dashboardCar == null) {
            return;
        }
        setDealership();
        populateCarIssuesAdapter();

        if(carName!=null) {
            carName.setText(dashboardCar.getYear() + " "
                    + dashboardCar.getMake() + " "
                    + dashboardCar.getModel());
            setIssuesCount();
        }
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
        shop = (shop == null)?shopLocalStore.getDealership(carLocalStore.getCar(dashboardCar.getId()).getShopId()):shop;
        dashboardCar.setDealership(shop);
        if (shop==null) {
            networkHelper.getShops(new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        try {
                            List<Dealership> dl = Dealership.createDealershipList(response);
                            shopLocalStore.deleteAllDealerships();
                            shopLocalStore.storeDealerships(dl);

                            Dealership d = shopLocalStore.getDealership(carLocalStore.getCar(dashboardCar.getId()).getShopId());

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
            if(dealershipName!=null) {
                dealershipName.setText(shop.getName());
                dealershipAddress.setText(shop.getAddress());
                dealershipPhone.setText(shop.getPhone());
            }
        }

    }


    @Override
    public void getBluetoothState(int state) {
        if(state==IBluetoothCommunicator.DISCONNECTED) {
            Log.i(TAG,"Bluetooth disconnected");
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {}

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {}

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {   }

    @Override
    public void tripData(TripInfoPackage tripInfoPackage) {

    }

    @Override
    public void parameterData(ParameterPackage parameterPackage) {

    }

    @Override
    public void pidData(PidPackage pidPackage) {

    }

    @Override
    public void dtcData(DtcPackage dtcPackage) {

    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {  }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if(loginPackageInfo.flag.
                equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            Log.i(TAG, "Device logout");
        }
    }

    private void populateCarIssuesAdapter() {
        // Try local store
        Log.i(TAG, "DashboardCar id: (Try local store) "+dashboardCar.getId());
        List<CarIssue> carIssues = carIssueLocalStore.getAllCarIssues(dashboardCar.getId());
        if(carIssues.isEmpty() && (dashboardCar.getNumberOfServices() > 0
                || dashboardCar.getNumberOfRecalls() > 0)) {
            Log.i(TAG, "No car issues in local store");

            networkHelper.getCarsById(dashboardCar.getId(), new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if(requestError == null) {
                        try {
                            dashboardCar.setIssues(CarIssue.createCarIssues(
                                    new JSONObject(response).getJSONArray("issues"), dashboardCar.getId()));
                            carIssueList.clear();
                            carIssueList.addAll(dashboardCar.getActiveIssues());
                            carIssuesAdapter.notifyDataSetChanged();
                            setIssuesCount();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(),
                                    "Error retrieving car details", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Load issues error: " + requestError.getMessage());
                        Toast.makeText(getActivity(),
                                "Error retrieving car details", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            Log.i(TAG, "Trying local store for carIssues");
            dashboardCar.setIssues(carIssues);
            carIssueList.clear();
            carIssueList.addAll(dashboardCar.getActiveIssues());
            carIssuesAdapter.notifyDataSetChanged();
        }


    }

    public void setDashboardCar(List<Car> carList) {
        if(getActivity() == null) {
            return;
        }
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int currentCarId = sharedPreferences.getInt(pfCurrentCar, -1);

        for(Car car : carList) {
            if(car.getId() == currentCarId) {
                dashboardCar = car;
                return;
            }
        }
        dashboardCar = carList.get(0);
    }

    /**
     *  Issues list view
     */
    class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        private List<CarIssue> carIssueList;
        static final int VIEW_TYPE_EMPTY = 100;

        public CustomAdapter(List<CarIssue> carIssues) {
            carIssueList = carIssues;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_issue, parent, false);
            ViewHolder viewHolder = new ViewHolder(v);
            return viewHolder;
        }

        public CarIssue getItem(int position) {
            return carIssueList.get(position);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            //Log.i(TAG,"On bind view holder");

            int viewType = getItemViewType(position);

            holder.date.setVisibility(View.GONE);

            if(viewType == VIEW_TYPE_EMPTY) {
                holder.description.setMaxLines(2);
                holder.description.setText("You have no pending Engine Code, Recalls or Services");
                holder.title.setText("Congrats!");
                holder.imageView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_check_circle_green_400_36dp));
            } else {
                final CarIssue carIssue = carIssueList.get(position);

                holder.description.setText(carIssue.getDescription());
                holder.description.setEllipsize(TextUtils.TruncateAt.END);
                if(carIssue.getIssueType().equals(CarIssue.RECALL)) {
                    holder.imageView.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_error_red_600_24dp));

                } else if(carIssue.getIssueType().equals(CarIssue.DTC)) {
                    holder.imageView.setImageDrawable(getResources().
                            getDrawable(R.drawable.car_engine_red));

                } else if(carIssue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                    holder.imageView.setImageDrawable(getResources().
                            getDrawable(R.drawable.car_engine_yellow));
                } else {
                    holder.description.setText(carIssue.getDescription());
                    holder.imageView.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_warning_amber_300_24dp));
                }

                holder.title.setText(String.format("%s %s", carIssue.getAction(), carIssue.getItem()));

                holder.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            mixpanelHelper.trackButtonTapped(carIssueList.get(position).getItem(), TAG);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Intent intent = new Intent(getActivity(), IssueDetailsActivity.class);
                        intent.putExtra(MainActivity.CAR_EXTRA, dashboardCar);
                        intent.putExtra(MainActivity.CAR_ISSUE_EXTRA, carIssueList.get(position));
                        startActivityForResult(intent, MainActivity.RC_DISPLAY_ISSUE);
                    }
                });
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(carIssueList.isEmpty()) {
                return VIEW_TYPE_EMPTY;
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


    @Override
    public void activityResultCallback(int requestCode, int resultCode, Intent data) {
        boolean shouldRefreshFromServer = data.getBooleanExtra(MainActivity.REFRESH_FROM_SERVER,false);

        if(requestCode == MainActivity.RC_ADD_CAR && resultCode== AddCarActivity.ADD_CAR_SUCCESS) {

            getActivity().findViewById(R.id.no_car_text).setVisibility(View.GONE);

            if(shouldRefreshFromServer)  {
                dashboardCar = data.getParcelableExtra(MainActivity.CAR_EXTRA);
                sharedPreferences.edit().putInt(pfCurrentCar, dashboardCar.getId()).commit();
            }
        }

    }
}