
package com.pitstop.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.observer.AutoConnectServiceBindingObserver;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Alarm;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.observer.AlarmObservable;
import com.pitstop.observer.AlarmObserver;
import com.pitstop.observer.FuelObservable;
import com.pitstop.observer.FuelObserver;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.alarms.AlarmsActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.pitstop.R.id.mileage;

public class DashboardFragment extends Fragment implements DashboardView, AlarmObserver, AutoConnectServiceBindingObserver {

    public static String TAG = DashboardFragment.class.getSimpleName();
    public static String  CAR_ID_KEY = "carId";
    public static final String PITSTOP_AMAZON_LINK = "https://www.amazon.ca/gp/product/B012GWJQZE";


    @BindView(R.id.dealer_background_imageview)
    ImageView mDealerBanner;

    @BindView(R.id.banner_overlay)
    FrameLayout mDealerBannerOverlay;

    @BindView(R.id.car_logo_imageview)
    ImageView mCarLogoImage;

    @BindView(R.id.alarm_badge)
    TextView alarms;


    @BindView(R.id.my_trips_icon)
    ImageView mMyTripsIcon;

    @BindView(R.id.swiperefresh)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.car_name)
    TextView carName;

    @BindView(R.id.driving_alarms_icon)
    ImageView drivingAlarmsIcon;


    @BindView(R.id.dealership_name)
    TextView dealershipName;

    @BindView(R.id.dealership_address)
    TextView dealershipAddress;

    @BindView(R.id.dealership_phone)
    TextView dealershipPhone;

    @BindView(R.id.offline_view)
    View offlineView;

    @BindView(R.id.progress)
    View loadingView;

    @BindView(R.id.no_car)
    View noCarView;

    @BindView(R.id.reg_view)
    View regView;

    @BindView(R.id.unknown_error_view)
    View unknownErrorView;

    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;
    private AlertDialog updateMileageDialog;
    private AlertDialog mileageErrorDialog;
    private AlertDialog buyDeviceAlertDialog;
    private AlertDialog fuelExpensesAlertDialog;
    private DashboardPresenter presenter;
    private AlarmObservable alarmObservable;
    private boolean hasBeenPopulated = false;




    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view = inflater.inflate(R.layout.fragment_main_dashboard, null);
        ButterKnife.bind(this, view);
        ((MainActivity)getActivity()).subscribe(this);
        if (presenter == null){
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getActivity()))
                    .build();
            MixpanelHelper mixpanelHelper = new MixpanelHelper((GlobalApplication)getContext()
                    .getApplicationContext());
            presenter = new DashboardPresenter(useCaseComponent, mixpanelHelper);

        }

        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.onUpdateNeeded();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");
        super.onDestroyView();
        hasBeenPopulated = false;
        presenter.unsubscribe();
    }

    public static int getDealerSpecificBanner(String name) {
        Log.d(TAG,"getDealerSpecificBanner()");
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

    public static int getCarSpecificLogo(String make) {
        Log.d(TAG,"getCarSpecificLogo()");
        if (make == null) return R.drawable.ford;
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

    @OnClick(R.id.my_trips_btn)
    protected void onMyTripsButtonClicked(){
        Log.d(TAG,"onMyTripsButtonClicked()");
        presenter.onMyTripsButtonClicked();
    }


    @OnClick(R.id.addCarButton)
    public void onAddCarButtonClicked(){
        Log.d(TAG,"onAddCarButtonClicked()");
        presenter.onAddCarButtonClicked();
    }

    @OnClick(R.id.offline_try_again)
    public void onOfflineTryAgainClicked(){
        Log.d(TAG,"onOfflineTryAgainClicked()");
        presenter.onOfflineTryAgainClicked();
    }

    @OnClick(R.id.unknown_error_try_again)
    public void onUnknownErrorTryAgainClicked(){
        Log.d(TAG,"onUnknownErrorTryAgainClicked()");
        presenter.onUnknownErrorTryAgainClicked();
    }

    @Override
    public void showLoading() {
        Log.d(TAG,"showLoading()");
        if (!swipeRefreshLayout.isRefreshing()) {
            loadingView.setVisibility(View.VISIBLE);
            loadingView.bringToFront();
            swipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public void hideLoading() {
        Log.d(TAG,"hideLoading()");
        if (!swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setEnabled(true);
            loadingView.setVisibility(View.GONE);
        }else{
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void hideRefreshing() {
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public boolean isRefreshing() {
        return swipeRefreshLayout.isRefreshing();
    }

    @Override
    public void displayOfflineErrorDialog() {
        Log.d(TAG,"displayOfflineErrorDialog()");
        if (offlineAlertDialog == null){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.offline_error_title);
            alertDialogBuilder
                    .setMessage(R.string.offline_error)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok_button, (dialog, id) -> {
                        dialog.dismiss();
                    });
            offlineAlertDialog = alertDialogBuilder.create();
        }

        offlineAlertDialog.show();
    }

    @Override
    public void displayUnknownErrorDialog() {
        Log.d(TAG,"displayUnknownErrorDialog()");
        if (unknownErrorDialog == null){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.unknown_error_title);
            alertDialogBuilder
                    .setMessage(R.string.unknown_error)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok_button, (dialog, id) -> {
                        dialog.dismiss();
                    });
            unknownErrorDialog = alertDialogBuilder.create();
        }

        unknownErrorDialog.show();
    }

    @Override
    public void displayUnknownErrorView() {
        Log.d(TAG,"displayUnknownErrorView()");
        offlineView.setVisibility(View.GONE);
        regView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.VISIBLE);
        unknownErrorView.bringToFront();
    }

    @Override
    public void displayOfflineView() {
        Log.d(TAG,"displayOfflineView()");
        offlineView.setVisibility(View.VISIBLE);
        regView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        offlineView.bringToFront();
    }

    @Override
    public void displayOnlineView() {
        Log.d(TAG,"displayOnlineView()");
        offlineView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        regView.setVisibility(View.VISIBLE);
        regView.bringToFront();
    }

    @Override
    public void displayNoCarView() {
        Log.d(TAG,"displayNoCarView()");
        offlineView.setVisibility(View.GONE);
        regView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        noCarView.setVisibility(View.VISIBLE);
    }

    @Override
    public void startAddCarActivity() {
        Log.d(TAG,"startAddCarActivity()");
        if (getActivity() != null){
            Intent intent = new Intent(getActivity(), AddCarActivity.class);
            getActivity().startActivityForResult(intent, MainActivity.RC_ADD_CAR);
        }
    }

    @Override
    public void displayDefaultDealershipVisuals(Dealership dealership) {
        Log.d(TAG,"displayDefaultDealershipVisual()");
        dealershipName.setText(dealership.getName());
        dealershipAddress.setText(dealership.getAddress());
        dealershipPhone.setText(dealership.getPhone());
        mDealerBanner.setImageResource(getDealerSpecificBanner(dealership.getName()));
        drivingAlarmsIcon.setImageResource(R.drawable.car_alarms_3x);
        mMyTripsIcon.setImageResource(R.drawable.route_2);
        if( (getActivity()) != null){
            ((MainActivity)getActivity()).changeTheme(false);
        }
        mCarLogoImage.setVisibility(View.VISIBLE);
        dealershipName.setVisibility(View.VISIBLE);
        carName.setTextColor(Color.BLACK);
        dealershipName.setTextColor(Color.BLACK);
        carName.setTypeface(Typeface.DEFAULT_BOLD);
        carName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        mDealerBannerOverlay.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayMercedesDealershipVisuals(Dealership dealership) {
        Log.d(TAG,"displayMercedesDealershipVisuals()");

        dealershipName.setText(dealership.getName());
        dealershipAddress.setText(dealership.getAddress());
        dealershipPhone.setText(dealership.getPhone());
        mDealerBanner.setImageResource(getDealerSpecificBanner(dealership.getName()));

        mDealerBanner.setImageResource(R.drawable.mercedes_brampton);

        drivingAlarmsIcon.setImageResource(R.drawable.mercedes_car_alarms_3x);
        mMyTripsIcon.setImageResource(R.drawable.mercedes_way_2);
        ((MainActivity)getActivity()).changeTheme(true);
        mCarLogoImage.setVisibility(View.GONE);
        dealershipName.setVisibility(View.GONE);
        carName.setTextColor(Color.WHITE);
        carName.setTypeface(Typeface.createFromAsset(getActivity().getAssets()
                , "fonts/mercedes.otf"));
        mDealerBannerOverlay.setVisibility(View.GONE);
        carName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
    }

    @Override
    public void displayCarDetails(Car car){
        Log.d(TAG,"displayCarDetails() car: "+car);
        hasBeenPopulated = true;
        carName.setText(car.getYear() + " " + car.getMake() + " "
                + car.getModel());
        mCarLogoImage.setVisibility(View.VISIBLE);
        mCarLogoImage.setImageResource(getCarSpecificLogo(car.getMake()));

    }

    @Override
    public void noScanner() {
        drivingAlarmsIcon.setImageResource(R.drawable.disabled_car_alarms_3x);

    }

    @Override
    public void startRequestServiceActivity() {
        Log.d(TAG,"startRequestServiceActivity()");
        ((MainActivity)getActivity()).requestMultiService(null);
    }

    @Override
    public void startMyAppointmentsActivity() {
        Log.d(TAG,"startMyAppointmentsActivity()");
        ((MainActivity)getActivity()).myAppointments();
    }

    @Override
    public void startMyTripsActivity() {
        Log.d(TAG,"startMyTripsActivity()");
        ((MainActivity)getActivity()).myTrips();
    }

    @Override
    public boolean hasBeenPopulated() {
        Log.d(TAG,"hasBeenPopulated() ? "+hasBeenPopulated);
        return hasBeenPopulated;
    }

    @OnClick(R.id.driving_alarms_button)
    public void onTotalAlarmsClicked(){
        Log.d(TAG, "onTotalAlarmsClicked()");
        presenter.onTotalAlarmsClicked();
    }

    public void openAlarmsActivity(){
        Log.d(TAG ,"openAlarmsActivity");
        Intent intent = new Intent(getActivity(), AlarmsActivity.class);
        Bundle bundle  = new Bundle();
        bundle.putBoolean("isMercedes", presenter.isDealershipMercedes());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void displayBuyDeviceDialog() {
        Log.d(TAG, "displayBuyDeviceDialog()");
        if (buyDeviceAlertDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            buyDeviceAlertDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Purchase Pitstop Device")
                    .setView(dialogLayout)
                    .setMessage("It appears you do not have a Pitstop device paired to this " +
                            "car.With the device,we can track your car's engine " +
                            "mileage, fuel consumption, trips, engine codes, and " +
                            "driving alarms. If you would like all these features, " +
                            "please purchase a device and connect it to your car. ")
                    .setPositiveButton("Purchase Pitstop Device", (dialog, which)
                            -> openPitstopAmazonLink())
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                    .create();
        }
        buyDeviceAlertDialog.show();
    }


    private void openPitstopAmazonLink() {
        Log.d(TAG, "openPitstopAmazonLink()");
        if(getActivity() == null) return;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PITSTOP_AMAZON_LINK));
        startActivity(browserIntent);
    }

    @Override
    public void hideBadge() {
        alarms.setVisibility(View.GONE);
    }

    @Override
    public void showBadges(int alarmCount) {
        alarms.setVisibility(View.VISIBLE);
        if (alarmCount>9)
            alarms.setText("9+");
        else
            alarms.setText(Integer.toString(alarmCount));

    }

    @Override
    public void onAlarmAdded(Alarm alarm) {
        presenter.setNumAlarms(presenter.getNumAlarms()+1);
        showBadges(presenter.getNumAlarms());
    }

    @Override
    public void onServiceBinded(@NotNull BluetoothAutoConnectService bluetoothAutoConnectService) {
        this.alarmObservable = (AlarmObservable) bluetoothAutoConnectService;
        alarmObservable.subscribe(this);

    }
}