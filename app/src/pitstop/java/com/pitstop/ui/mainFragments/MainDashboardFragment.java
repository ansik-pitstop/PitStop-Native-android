
package com.pitstop.ui.mainFragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.pitstop.BuildConfig;
import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.issue_detail.IssueDetailsActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.pitstop.bluetooth.BluetoothAutoConnectService.LAST_MILEAGE;
import static com.pitstop.bluetooth.BluetoothAutoConnectService.LAST_RTC;

public class MainDashboardFragment extends CarDataFragment implements MainDashboardCallback {

    public static String TAG = MainDashboardFragment.class.getSimpleName();
    public final EventSource EVENT_SOURCE = new EventSourceImpl(EventSource.SOURCE_DASHBOARD);

    public final static String pfName = "com.pitstop.login.name";
    public final static String pfCurrentCar = "ccom.pitstop.currentcar";

    public final static int MSG_UPDATE_CONNECTED_CAR = 1076;

    private View rootview;
    private TextView dealershipAddress;
    private TextView dealershipPhone;
    private TextView carName, dealershipName;

    private AlertDialog updateMileageDialog;

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

    @BindView(R.id.my_appts_icon)
    ImageView mMyAppointmentsIcon;

    @BindView(R.id.my_trips_icon)
    ImageView mMyTripsIcon;

    @Inject
    GetUserCarUseCase getUserCarUseCase;

    ProgressDialog progressDialog;

    // Models
    private Car dashboardCar;
    private List<CarIssue> carIssueList = new ArrayList<>();


    // Database accesses
    private LocalCarAdapter carLocalStore;
    private LocalShopAdapter shopLocalStore;

    private GlobalApplication application;
    private SharedPreferences sharedPreferences;

    // Utils / Helper
    private NetworkHelper networkHelper;
    private MixpanelHelper mixpanelHelper;

    private Context context;

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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        MainActivity.mainDashboardCallback = this;
    }

    public static MainDashboardFragment newInstance() {
        MainDashboardFragment fragment = new MainDashboardFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_main_dashboard, null);
        ButterKnife.bind(this, rootview);

        this.context = getContext().getApplicationContext();
        application = (GlobalApplication)context;
        networkHelper = new NetworkHelper(context);
        mixpanelHelper = new MixpanelHelper((GlobalApplication)context);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Local db adapters
        carLocalStore = new LocalCarAdapter(context);
        shopLocalStore = new LocalShopAdapter(context);

        UseCaseComponent component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        component.injectUseCases(this);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCanceledOnTouchOutside(false);

        setStaticUI();
        updateUI();

        return rootview;
    }

    private void setStaticUI(){
        carName = (TextView) rootview.findViewById(R.id.car_name);
        dealershipName = (TextView) rootview.findViewById(R.id.dealership_name);
        dealershipAddress = (TextView) rootview.findViewById(R.id.dealership_address);
        dealershipPhone = (TextView) rootview.findViewById(R.id.dealership_phone);
    }

    @Override
    public void updateUI(){
        showLoading("Loading...");

        getUserCarUseCase.execute(new GetUserCarUseCase.Callback() {
            @Override
            public void onCarRetrieved(Car car) {
                dashboardCar = car;

                //Setup dealership
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

                if (carName != null) {
                    carName.setText(car.getYear() + " "
                            + car.getMake() + " "
                            + car.getModel());
                }

                mMileageText.setText(String.format("%.2f km",car.getTotalMileage()));
                mEngineText.setText(car.getEngine());
                mHighwayText.setText(car.getHighwayMileage());
                mCityText.setText(car.getCityMileage());
                mCarLogoImage.setImageResource(getCarSpecificLogo(car.getMake()));

                hideLoading(null);
            }

            @Override
            public void onNoCarSet() {
                Toast.makeText(getActivity(),
                        "Error retrieving car details", Toast.LENGTH_SHORT).show();
                hideLoading(null);
            }

            @Override
            public void onError() {
                Toast.makeText(getActivity(),
                        "Error retrieving car details", Toast.LENGTH_SHORT).show();
                hideLoading(null);
            }
        });
    }

    @Override
    public EventSource getSourceType() {
        return EVENT_SOURCE;
    }

    @Override
    public void onStop() {
        super.onStop();
        hideLoading(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        //updateUI();
        handler.postDelayed(carConnectedRunnable, 1000);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(carConnectedRunnable);
        application.getMixpanelAPI().flush();
        hideLoading(null);
        super.onPause();
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
            mMyAppointmentsIcon.setImageResource(R.drawable.clipboard3x);
            mMyTripsIcon.setImageResource(R.drawable.route_2);
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
        mMyAppointmentsIcon.setImageResource(R.drawable.mercedes_clipboard3x);
        mMyTripsIcon.setImageResource(R.drawable.mercedes_way_2);
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
    public void tripData(TripInfoPackage tripInfoPackage) {
        Log.d(TAG,"Got trip data.");
        if (tripInfoPackage.flag == TripInfoPackage.TripFlag.UPDATE) { // live mileage update
            final double newTotalMileage;
            if (((MainActivity)getActivity()).getBluetoothConnectService().isConnectedTo215() && sharedPreferences.getString(LAST_RTC.replace("{car_vin}", dashboardCar.getVin()), null) != null )
                if (tripInfoPackage.rtcTime >= Long.valueOf(sharedPreferences.getString(LAST_RTC.replace("{car_vin}", dashboardCar.getVin()), null)))
                    newTotalMileage = (dashboardCar.getTotalMileage() + tripInfoPackage.mileage) - Double.valueOf(sharedPreferences.getString(LAST_MILEAGE.replace("{car_vin}", dashboardCar.getVin()), null));
                else
                    return;
            else
                newTotalMileage = ((int) ((dashboardCar.getTotalMileage() + tripInfoPackage.mileage) * 100)) / 100.0; // round to 2 decimal places

            Log.v(TAG, "Mileage updated: tripMileage: " + tripInfoPackage.mileage + ", baseMileage: " + dashboardCar.getTotalMileage() + ", newMileage: " + newTotalMileage);

            if (dashboardCar.getDisplayedMileage() < newTotalMileage) {
                dashboardCar.setDisplayedMileage(newTotalMileage);
                carLocalStore.updateCar(dashboardCar);
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMileageText.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.mileage_update));
                    mMileageText.setText(String.format("%.2f km", newTotalMileage));
                }
            });

        } else if (tripInfoPackage.flag == TripInfoPackage.TripFlag.END) { // uploading historical data
            //dashboardCar = carLocalStore.getCar(dashboardCar.getId());
            final double newBaseMileage = dashboardCar.getTotalMileage();
            //mCallback.onTripMileageUpdated(newBaseMileage);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mMileageText.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.mileage_update));
                    mMileageText.setText(String.format("%.2f km", newBaseMileage));
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
        ((MainActivity)getActivity()).requestMultiService(null);//find
    }
    @OnClick(R.id.my_appointments_btn)
    protected void onMyAppointmentsButtonClicked(){
        ((MainActivity)getActivity()).myAppointments();
    }
    @OnClick(R.id.my_trips_btn)
    protected void onMyTripsButtonCllicked(){
        ((MainActivity)getActivity()).myTrips();
    }

    @OnClick(R.id.mileage_container)
    protected void onMileageClicked(){

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

                                //Update mileage in the GUI so it doesn't have to be loaded from network
                                mMileageText.setText(String.format("%.2f km", mileage));

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

                                        EventType eventType = new EventTypeImpl(EventType.EVENT_MILEAGE);
                                        EventBus.getDefault().post(new CarDataChangedEvent(eventType
                                                ,EVENT_SOURCE));

                                        if (IBluetoothCommunicator.CONNECTED == ((MainActivity)getActivity()).getBluetoothConnectService().getState()
                                                || ((MainActivity)getActivity()).getBluetoothConnectService().isCommunicatingWithDevice()) {
                                            mMileageText.setText(String.format("%.2f km", mileage));
                                            ((MainActivity)getActivity()).getBluetoothConnectService().get215RtcAndMileage();
                                        } else {
                                            if (((MainActivity)getActivity()).getBluetoothConnectService().getState() == IBluetoothCommunicator.CONNECTED||
                                                    ((MainActivity)getActivity()).getBluetoothConnectService().isCommunicatingWithDevice())
                                                ((MainActivity)getActivity()).getBluetoothConnectService().startBluetoothSearch();
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
        if (progressDialog != null && !progressDialog.isShowing() && getUserVisibleHint()) {
            if (loadingMessage != null)
                progressDialog.setMessage(loadingMessage);
            progressDialog.show();
        }
    }

    private void hideLoading(String toastMessage) {
        if (progressDialog != null) {
            progressDialog.dismiss();
            if (toastMessage != null && getUserVisibleHint())
                Toast.makeText(getActivity(), toastMessage, Toast.LENGTH_SHORT).show();
        }
    }
}