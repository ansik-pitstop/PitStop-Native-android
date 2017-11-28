package com.pitstop.ui.vehicle_specs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.observer.AutoConnectServiceBindingObserver;
import com.pitstop.observer.FuelObservable;
import com.pitstop.observer.FuelObserver;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.custom_shops.CustomShopActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;



/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsFragment extends Fragment implements VehicleSpecsView, FuelObserver, AutoConnectServiceBindingObserver {
    public static final String TAG = VehicleSpecsFragment.class.getSimpleName();

    public static final String PITSTOP_AMAZON_LINK = "https://www.amazon.ca/gp/product/B012GWJQZE";
    public static final String CAR_DELETED = "deleted";
    public static final String CAR_POSITION ="position" ;
    public static final String CAR_SELECTED ="carCurrent" ;

    private AlertDialog fuelConsumptionExplanationDialog;
    public static final int START_CUSTOM = 347;
    private AlertDialog buyDeviceDialog;
    private AlertDialog fuelExpensesAlertDialog;
    private AlertDialog licensePlateDialog;
    private AlertDialog updateMileageDialog;
    private AlertDialog deleteCarAlertDialog;
    private AlertDialog changeDealershipAlertDialog;
    private VehicleSpecsPresenter presenter;
    private AlertDialog mileageErrorDialog;
    private AlertDialog unknownErrorDialog;
    private AlertDialog offlineErrorDialog;
    private boolean isPoppulated = false;
    private FuelObservable fuelObservable;
    @BindView(R.id.swiper)
    protected SwipeRefreshLayout swipeRefreshLayout;

//    @BindView(R.id.car_logo_imageview)
//    protected ImageView carLogo;

//    @BindView(R.id.car_name_banner)
//    protected TextView carName;

    @BindView(R.id.main_view_lin_layout)
    protected LinearLayout mainLayout;

    @BindView(R.id.main_linear_layout)
    protected RelativeLayout mainLinearLayout;

    @BindView(R.id.loading_view_main)
    protected View loadingView;

    @BindView(R.id.vin_icon)
    protected ImageView vinIcon;

    @BindView(R.id.scanner_icon)
    protected ImageView scannerIcon;

    @BindView(R.id.license_icon)
    protected ImageView licenseIcon;

    @BindView(R.id.dealership_icon)
    protected ImageView dealershipIcon;

    @BindView(R.id.mileage_icon)
    protected ImageView mileageIcon;

    @BindView(R.id.engine_icon)
    protected ImageView engineIcon;

    @BindView(R.id.city_mileage_icon)
    protected ImageView cityMileageIcon;

    @BindView(R.id.highway_mileage_icon)
    protected ImageView highwayMileageIcon;

    @BindView(R.id.trim_icon)
    protected ImageView trimIcon;

    @BindView(R.id.tank_size_icon)
    protected ImageView tankSizeIcon;

    @BindView(R.id.money_spent)
    TextView fuelExpensesTextView;


//    @BindView(R.id.banner_overlay)
//    protected FrameLayout bannerOverlay;

//    @BindView(R.id.dealership_name_banner)
//    protected TextView dealershipName;
//
//    @BindView(R.id.background_image)
//    protected ImageView carPic;

    @BindView(R.id.no_car)
    protected View noCarView;

    @BindView(R.id.offline_view)
    protected View offlineView;

    @BindView(R.id.unknown_error_view)
    protected View unknownErrorView;

    @BindView(R.id.total_mileage_tv)
    protected TextView totalMileagetv;

    @BindView(R.id.dealership_tv)
    protected TextView dealership;

    @BindView(R.id.dealership_row)
    protected View dealershipView;

    @BindView(R.id.car_vin)
    protected TextView carVin;

    @BindView(R.id.scanner_row)
    protected View scannerView;

    @BindView(R.id.delete_car)
    protected View deleteCarView;

    @BindView(R.id.scanner_id)
    protected TextView scannerID;

    @BindView(R.id.car_license_plate_specs)
    protected TextView licensePlate;

    @BindView(R.id.car_engine)
    protected TextView engine;

    @BindView(R.id.fuel_consumed)
    protected TextView fuelConsumed;

    @BindView(R.id.city_mileage_specs)
    protected TextView cityMileage;

    @BindView(R.id.highway_mileage_specs)
    protected TextView highwayMileage;

    @BindView(R.id.license_plate_row)
    protected View plateView;

    @BindView(R.id.fuel_trim_row)
    protected View trimView;

    @BindView(R.id.trim)
    protected TextView trim;

    @BindView(R.id.tank_size_row)
    protected View tankSizeView;

    @BindView(R.id.tank_size)
    protected TextView tankSize;

//    @BindView(R.id.progress)
//    protected View imageLoadingView;


    public static VehicleSpecsFragment newInstance(){
        return new VehicleSpecsFragment();
    }

    private boolean carPicgetError;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view  = inflater.inflate(R.layout.fragment_vehicle_specs, null);
        ButterKnife.bind(this, view);
        ((MainActivity)getActivity()).subscribe(this);
        if (presenter == null) {
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getActivity()))
                    .build();

            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication)getActivity().getApplicationContext());

            presenter = new VehicleSpecsPresenter(useCaseComponent, mixpanelHelper);
        }
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        presenter.subscribe(this);
        Log.d(TAG, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.onUpdateNeeded();

    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView()");
        presenter.unsubscribe();
        super.onDestroyView();
        isPoppulated = false;
    }
    @Override
    public void showNoCarView(){
        Log.d(TAG, "showNoCarView()");
        mainLayout.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noCarView.setVisibility(View.VISIBLE);
        loadingView.bringToFront();
        swipeRefreshLayout.setEnabled(true);

    }

    public void showOfflineErrorView(){
        mainLayout.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        offlineView.setVisibility(View.VISIBLE);
        offlineView.bringToFront();
    }

    public void showUnknownErrorView(){
        mainLayout.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.VISIBLE);
        unknownErrorView.bringToFront();
    }

    @Override
    public void showLoading() {
        if (!swipeRefreshLayout.isRefreshing()) {
            Log.d(TAG, "showLoading()");
            loadingView.setVisibility(View.VISIBLE);
            loadingView.bringToFront();
            swipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public void hideLoading(){
        Log.d(TAG, "hideLoading()");
        if (!swipeRefreshLayout.isRefreshing()) {
           swipeRefreshLayout.setEnabled(true);
            loadingView.setVisibility(View.GONE);
        }
        else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void showImage(String s) {
        Log.d(TAG, "showImage()");
       // carLogo.setVisibility(View.GONE);
//        dealershipName.setVisibility(View.GONE);
//        carName.setVisibility(View.GONE);
        //bannerOverlay.setVisibility(View.GONE);
//        if (getActivity()!=null)
//            Picasso.with(getActivity()).load(s).into(carPic);
    }

    @Override
    public void setCarView(Car car) {
        Log.d(TAG, "setView()");

        //Set other views to GONE and main to VISIBLE
        offlineView.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);

        //Populate view
        carVin.setText(car.getVin());
        if (car.getScannerId() == null)
            scannerID.setText("No scanner connected");
        else
            scannerID.setText(car.getScannerId());
        if (car.getEngine() == null){
            engine.setVisibility(View.GONE);
        }
        else
            engine.setText(car.getEngine());

        cityMileage.setText(car.getCityMileage());
        highwayMileage.setText(car.getHighwayMileage());
        if (car.getTrim() == null)
            trimView.setVisibility(View.GONE);
        else
            trim.setText(car.getTrim());

        if (car.getTankSize() == null)
            tankSizeView.setVisibility(View.GONE);
        else
            tankSize.setText(car.getTankSize());

        if(!(presenter.getDealership()== null)) {
            dealership.setText(presenter.getDealership().getName());
        }

        presenter.getLicensePlate(car.getId());

        totalMileagetv.setText(String.format("%.2fkm", car.getTotalMileage()));
        isPoppulated = true;
    }

    @Override
    public void showLicensePlate(String s) {
        Log.d(TAG, "showLicensePlate()");
        licensePlate.setText(s);
        licensePlate.setVisibility(View.VISIBLE);
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
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        dialog.dismiss();
                    });
            unknownErrorDialog = alertDialogBuilder.create();
        }

        unknownErrorDialog.show();
    }


    @Override
    public void toast(String message) {
        Log.d(TAG, "toast " + message);
        if (getActivity()!=null)
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showDealershipBanner() {
        Log.d(TAG, "showDealershipBanner()");
        carPicgetError = true;
        //carLogo.setVisibility(View.VISIBLE);
//        dealershipName.setVisibility(View.VISIBLE);
//        carName.setVisibility(View.VISIBLE);
//        carName.setText(Integer.toString(presenter.getCar().getYear()) + " " +
//                        presenter.getCar().getMake() + " " +
//                        presenter.getCar().getModel());
        //carLogo.setImageResource(DashboardFragment.getCarSpecificLogo( presenter.getCar().getMake()));
//        dealershipName.setText(presenter.getDealership().getName());
//        carPic.setImageResource(DashboardFragment.getDealerSpecificBanner(presenter.getDealership().getName()));
        //bannerOverlay.setVisibility(View.VISIBLE);
    }

    public void showMercedesLayout(){
        vinIcon.setImageResource(R.drawable.mercedes_vin_3x);
        scannerIcon.setImageResource(R.drawable.mercedes_scanner_3x);
        licenseIcon.setImageResource(R.drawable.mercedes_license_3x);
        dealershipIcon.setImageResource(R.drawable.mercedes_dealership);
        mileageIcon.setImageResource(R.drawable.mercedes_mileage);
        engineIcon.setImageResource(R.drawable.mercedes_engine);
        cityMileageIcon.setImageResource(R.drawable.traffic_lights_mercedes_3x);
        highwayMileageIcon.setImageResource(R.drawable.highway_mileage_mercedes_2x);
        trimIcon.setImageResource(R.drawable.mercedes_trim_3x);
        tankSizeIcon.setImageResource(R.drawable.mercedes_tank_size_3x);
    }

    public void showNormalLayout(){
        vinIcon.setImageResource(R.drawable.vin_2x);
        scannerIcon.setImageResource(R.drawable.scanner_2x);
        licenseIcon.setImageResource(R.drawable.license_2x);
        dealershipIcon.setImageResource(R.drawable.dealership_2x);
        mileageIcon.setImageResource(R.drawable.odometer3x);
        engineIcon.setImageResource(R.drawable.car_engine);
        cityMileageIcon.setImageResource(R.drawable.traffic_lights_2x);
        highwayMileageIcon.setImageResource(R.drawable.highway_mileage2x);
        trimIcon.setImageResource(R.drawable.trim_2x);
        tankSizeIcon.setImageResource(R.drawable.tank_size_2x);
    }




    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult()");
        if(requestCode == START_CUSTOM && (resultCode == AddCarActivity.ADD_CAR_SUCCESS_HAS_DEALER)) {
            dealership.setText(data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY));
            //dealershipName.setText(data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY));
            if (carPicgetError) {
                showDealershipBanner();
                //dealershipName.setText(data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY));
                //carPic.setImageResource(DashboardFragment.getDealerSpecificBanner(data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY)));
            }
            Log.d(TAG,data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY) );

        }
        else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @OnClick(R.id.license_plate_row)
    public void showLicensePlateDialog(){
        Log.d(TAG, "showLicensePlateDialog()");
        if (licensePlateDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_input_license_plate, null);
            final TextInputEditText textInputEditText = (TextInputEditText)dialogLayout
                    .findViewById(R.id.plate_input);
            textInputEditText.setHint("License Plate");
            licensePlateDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Update License Plate")
                    .setView(dialogLayout)
                    .setPositiveButton("Confirm", (dialog, which)
                            -> presenter.onUpdateLicensePlateDialogConfirmClicked(presenter.getCar().getId(),
                            textInputEditText.getText().toString()))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                    .create();
        }
        licensePlateDialog.show();
    }

    @OnClick(R.id.scanner_row)
    public void onScannerViewClicked(){
        Log.d(TAG, "onScannerViewClicked()");
        presenter.onScannerViewClicked();
    }


    @OnClick(R.id.delete_car)
    public void onDeleteCarClicked(){
        Log.d(TAG, "deleteCarClicked");
        if (deleteCarAlertDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            deleteCarAlertDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Delete Car")
                    .setView(dialogLayout)
                    .setMessage("Are you sure you want to delete this car")
                    .setPositiveButton("Yes", (dialog, which)
                            -> presenter.deleteCar())
                    .setNegativeButton("No", (dialog, which) -> dialog.cancel())
                    .create();
        }
        deleteCarAlertDialog.show();
    }

    @Override
    public void showBuyDeviceDialog() {
        Log.d(TAG, "showBuyDeviceDialog()");
        if (buyDeviceDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            buyDeviceDialog = new AnimatedDialogBuilder(getActivity())
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
        buyDeviceDialog.show();
    }
    private void openPitstopAmazonLink() {
        Log.d(TAG, "openPitstopAmazonLink()");
        if(getActivity() == null) return;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PITSTOP_AMAZON_LINK));
        startActivity(browserIntent);
    }

    public void startCustomShop(){
        if (presenter.getCar()!=null && getActivity() != null) {
            Intent intent = new Intent(getActivity(), CustomShopActivity.class);
            intent.putExtra(MainActivity.CAR_EXTRA, presenter.getCar());
            getActivity().startActivityForResult(intent, START_CUSTOM);
        }
    }

    @OnClick(R.id.dealership_row)
    public void showDealershipChangeDialog(){
        if (changeDealershipAlertDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            changeDealershipAlertDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Change Dealership")
                    .setView(dialogLayout)
                    .setMessage("Are you sure you want to change the dealership of this car?")
                    .setPositiveButton(getString(R.string.ok_button), (dialog, which)
                            -> startCustomShop())
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                    .create();
        }
        changeDealershipAlertDialog.show();
    }

    @OnClick(R.id.mileage_row)
    public void displayUpdateMileageDialog() {
        Log.d(TAG,"displayUpdateMileageDialog()");
        if (updateMileageDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_input_mileage, null);
            final TextInputEditText textInputEditText = (TextInputEditText)dialogLayout
                    .findViewById(R.id.mileage_input);
            updateMileageDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Update Mileage")
                    .setView(dialogLayout)
                    .setPositiveButton("Confirm", (dialog, which)
                            -> presenter.onUpdateMileageDialogConfirmClicked(
                            textInputEditText.getText().toString()))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                    .create();
        }

        updateMileageDialog.show();
    }


    @Override
    public void displayOfflineErrorDialog() {
        Log.d(TAG,"displayOfflineErrorDialog()");
        if (offlineErrorDialog == null){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.offline_error_title);
            alertDialogBuilder
                    .setMessage(R.string.offline_error)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        dialog.dismiss();
                    });
            offlineErrorDialog = alertDialogBuilder.create();
        }

        offlineErrorDialog.show();
    }

    @Override
    public void displayUpdateMileageError() {
        Log.d(TAG,"displayUpdateMileageError()");
        if (mileageErrorDialog == null){
            mileageErrorDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Invalid Mileage")
                    .setMessage("Please input a valid mileage.")
                    .setPositiveButton("OK", (dialog, which)
                            -> dialog.dismiss())
                    .create();
        }

        mileageErrorDialog.show();
    }


    public void showImageLoading(){
//        imageLoadingView.bringToFront();
//        imageLoadingView.setVisibility(View.VISIBLE);
    }

    public void showLoadingDialog(String text) {
        if (progressDialog == null) {
            return;
        }
        progressDialog.setMessage(text);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    public void hideLoadingDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        } else {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }

    public void hideImageLoading(){
        //imageLoadingView.setVisibility(View.GONE);
    }

    @OnClick(R.id.addCarButton)
    public void startaddCarActivity(){
        Log.d(TAG, "onAddCarClicked()");
        Intent intent = new Intent(getActivity(), AddCarActivity.class);
        startActivityForResult(intent, MainActivity.RC_ADD_CAR);
    }

    @Override
    public void displayMileage(double mileage) {
        Log.d(TAG,"displayMileage() mileage: "+mileage);
        totalMileagetv.setText(String.format("%.2f km",mileage));
    }


    @Override
    public void showFuelConsumptionExplanationDialog() {

        Log.d(TAG, "displayBuyDeviceDialog()");
        if (fuelConsumptionExplanationDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            fuelConsumptionExplanationDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Fuel Consumption")
                    .setView(dialogLayout)
                    .setMessage("With the device, we are able to track your fuel usage from the time the device was plugged in. ")
                    .setPositiveButton("OK", (dialog, which) -> dialog.cancel())
                    .create();
        }
        fuelConsumptionExplanationDialog.show();



    }

    @Override
    public boolean hasBeenPopulated() {
        return isPoppulated;
    }

    @OnClick(R.id.offline_try_again)
    public void onOfflineTryAgainClicked(){
        presenter.onUpdateNeeded();
    }

    @OnClick(R.id.unknown_error_try_again)
    public void onUnknownTryAgainClicked(){
        presenter.onUpdateNeeded();
    }

    @OnClick(R.id.fuel_consumption_row)
    public void onfuelConsumptionClicked(){
        Log.d(TAG, "onFuelConsumptionClicked()");
        presenter.onFuelConsumptionClicked();

    }

    @Override
    public void onFuelConsumedUpdated() {
        presenter.getFuelConsumed();

    }

    @Override
    public void showFuelConsumed(double fuelCOnsumed) {
        fuelConsumed.setText(Double.toString(fuelCOnsumed) + " L");
    }

    @Override
    public void onServiceBinded(@NotNull BluetoothAutoConnectService bluetoothAutoConnectService) {
        this.fuelObservable = (FuelObservable) bluetoothAutoConnectService;
        fuelObservable.subscribe(this);
    }


    @OnClick(R.id.fuel_expense_row)
    public void onFuelExpensesClicked(){
        Log.d(TAG, "onFuelExpensesCLicked()");
        presenter.onFuelExpensesClicked();
    }


    @Override
    public void showFuelExpense(float v) {
        fuelExpensesTextView.setText(String.format("$%.2f", v/100));

    }

    public String getLastKnowLocation(){
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria,true);
        if(ContextCompat.checkSelfPermission( getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
            locationManager.requestLocationUpdates(provider, 1, 1,locationListener);
            String locationProvider = LocationManager.NETWORK_PROVIDER;
            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            if (lastKnownLocation == null){
                return null;
            }
            locationManager.removeUpdates(locationListener);
            Geocoder geocoder = new Geocoder(getActivity());
            try {
                ArrayList<Address> list = new ArrayList<>(geocoder.getFromLocation(lastKnownLocation.getLatitude(),
                        lastKnownLocation.getLongitude(), 5));
                return list.get(0).getPostalCode();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
        else {
            return null;
        }
    }


    @Override
    public void showFuelExpensesDialog() {
        if (fuelExpensesAlertDialog == null){ final View dialogLayout = LayoutInflater.from(
                getActivity()).inflate(R.layout.buy_device_dialog, null);
            fuelExpensesAlertDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Fuel Expense")
                    .setView(dialogLayout)
                    .setMessage(getString(R.string.fuel_expense_dialog_description))
                    .setPositiveButton(getString(R.string.ok_button), (dialog, which)
                            -> dialog.cancel())
                    .create();
        }
        fuelExpensesAlertDialog.show();

    }
}
