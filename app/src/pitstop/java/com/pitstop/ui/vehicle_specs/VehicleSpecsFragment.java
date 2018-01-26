package com.pitstop.ui.vehicle_specs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.observer.AutoConnectServiceBindingObserver;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.alarms.AlarmsActivity;
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

public class VehicleSpecsFragment extends Fragment implements VehicleSpecsView, AutoConnectServiceBindingObserver {
    public static final String TAG = VehicleSpecsFragment.class.getSimpleName();

    public static final String PITSTOP_AMAZON_LINK = "https://www.amazon.ca/gp/product/B012GWJQZE";
    private AlertDialog fuelConsumptionExplanationDialog;
    public static final int START_CUSTOM = 347;
    private AlertDialog buyDeviceDialog;
    private AlertDialog fuelExpensesAlertDialog;
    private AlertDialog licensePlateDialog;
    private AlertDialog updateMileageDialog;
    private AlertDialog pairScannerAlertDialog;
    private AlertDialog deleteCarAlertDialog;
    private AlertDialog changeDealershipAlertDialog;
    private VehicleSpecsPresenter presenter;
    private AlertDialog mileageErrorDialog;
    private AlertDialog unknownErrorDialog;
    private AlertDialog scannerAlreadyActiveDialog;
    private AlertDialog offlineErrorDialog;
    private AlertDialog confirmScannerUpdateDialog;
    private boolean isPoppulated = false;


    @BindView(R.id.swiper)
    protected SwipeRefreshLayout swipeRefreshLayout;

//    @BindView(R.id.car_logo_imageview)
//    protected ImageView carLogo;

//    @BindView(R.id.car_name_banner)
//    protected TextView carName;

    @BindView(R.id.alarms_row)
    protected View alarmsView;

    @BindView(R.id.alarm_badge)
    TextView alarmsCount;

    @BindView(R.id.main_view)
    protected View mainLayout;

    @BindView(R.id.loading_view_main)
    protected View loadingView;

    @BindView(R.id.vin_icon)
    protected ImageView vinIcon;

    @BindView(R.id.dealer_background_imageview)
    protected ImageView mDealerBanner;

    @BindView(R.id.banner_overlay)
    protected FrameLayout mDealerBannerOverlay;

    @BindView(R.id.dealership_name)
    protected TextView dealershipName;

    @BindView(R.id.car_logo_imageview)
    protected ImageView mCarLogoImage;


    @BindView(R.id.car_name)
    protected TextView carName;

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

    public static VehicleSpecsFragment newInstance() {
        return new VehicleSpecsFragment();
    }

    private boolean carPicgetError;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_vehicle_specs, null);
        ButterKnife.bind(this, view);
        ((MainActivity) getActivity()).subscribe(this);
        if (presenter == null) {
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getActivity()))
                    .build();

            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication) getActivity().getApplicationContext());

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
    public void showNoCarView() {
        Log.d(TAG, "showNoCarView()");
        mainLayout.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        offlineView.setVisibility(View.GONE);
        noCarView.setVisibility(View.VISIBLE);
        loadingView.bringToFront();
        swipeRefreshLayout.setEnabled(true);

    }

    public void showOfflineErrorView() {
        Log.d(TAG, "showOfflineErrorView()");
        mainLayout.setVisibility(View.GONE);
        loadingView.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        noCarView.setVisibility(View.GONE);
        offlineView.setVisibility(View.VISIBLE);
        offlineView.bringToFront();
    }

    public void showUnknownErrorView() {
        Log.d(TAG, "showUnknownErrorView()");
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
    public void hideLoading() {
        Log.d(TAG, "hideLoading()");
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setEnabled(true);
            loadingView.setVisibility(View.GONE);
        } else {
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
        engine.setText(car.getEngine());
        cityMileage.setText(car.getCityMileage());
        highwayMileage.setText(car.getHighwayMileage());
        trim.setText(car.getTrim());
        tankSize.setText(car.getTankSize());
        if (!(presenter.getDealership() == null)) {
            dealership.setText(presenter.getDealership().getName());
        }
        presenter.getLicensePlate(car.getId());
        totalMileagetv.setText(String.format("%.2fkm", car.getTotalMileage()));
    }

    @Override
    public void showLicensePlate(String s) {
        Log.d(TAG, "showLicensePlate()");
        licensePlate.setText(s);
        licensePlate.setVisibility(View.VISIBLE);
    }

    @Override
    public void displayUnknownErrorDialog() {
        Log.d(TAG, "displayUnknownErrorDialog()");
        if (unknownErrorDialog == null) {
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
        if (getActivity() != null)
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void startMyTripsActivity() {
        Log.d(TAG, "startMyTripsActivity()");
        if (presenter.getCar() == null) {
            displayOfflineErrorDialog();
            return;
        }
        ((MainActivity) getActivity()).myTrips(presenter.getCar());
    }

    public void showNormalLayout() {
        Log.d(TAG, "showNormalLayout()");
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

    @OnClick(R.id.license_plate_row)
    public void showLicensePlateDialog() {
        Log.d(TAG, "showLicensePlateDialog()");
        if (licensePlateDialog == null) {
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_input_license_plate, null);
            final TextInputEditText textInputEditText = (TextInputEditText) dialogLayout
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
    public void onScannerViewClicked() {
        Log.d(TAG, "onScannerViewClicked()");
        presenter.onScannerViewClicked();
    }

    @OnClick(R.id.my_trips_row)
    protected void onMyTripsButtonClicked() {
        Log.d(TAG, "onMyTripsButtonClicked()");
        presenter.onMyTripsButtonClicked();
    }


    @OnClick(R.id.delete_car)
    public void onDeleteCarClicked() {
        Log.d(TAG, "deleteCarClicked");
        if (deleteCarAlertDialog == null) {
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
        if (buyDeviceDialog == null) {
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
        if (getActivity() == null) return;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PITSTOP_AMAZON_LINK));
        startActivity(browserIntent);
    }

    public void startCustomShop() {
        Log.d(TAG, "startCustomShop");
        if (presenter.getCar() != null && getActivity() != null) {
            Intent intent = new Intent(getActivity(), CustomShopActivity.class);
            intent.putExtra(MainActivity.CAR_EXTRA, presenter.getCar());
            getActivity().startActivityForResult(intent, START_CUSTOM);
        }
    }

    @OnClick(R.id.dealership_row)
    public void showDealershipChangeDialog() {
        Log.d(TAG, "showDealershipChangeDialog()");
        if (changeDealershipAlertDialog == null) {
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
        Log.d(TAG, "displayUpdateMileageDialog()");
        if (updateMileageDialog == null) {
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_input_mileage, null);
            final TextInputEditText textInputEditText = (TextInputEditText) dialogLayout
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
        Log.d(TAG, "displayOfflineErrorDialog()");
        if (offlineErrorDialog == null) {
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
        Log.d(TAG, "displayUpdateMileageError()");
        if (mileageErrorDialog == null) {
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


    public void showLoadingDialog(String text) {
        Log.d(TAG, "showLoadingDialog()");
        if (progressDialog == null) {
            return;
        }
        progressDialog.setMessage(text);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    public void hideLoadingDialog() {
        Log.d(TAG, "hidLoafingDialog()");
        if (progressDialog != null) {
            progressDialog.dismiss();
        } else {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }

    @OnClick(R.id.addCarButton)
    public void onAddCarClicked() {
        Log.d(TAG, "onAddCarClicked()");
        presenter.onAddCarClicked();
    }

    @Override
    public void startAddCarActivity() {
        Log.d(TAG, "startAddCarActivity()");
        Intent intent = new Intent(getActivity(), AddCarActivity.class);
        startActivityForResult(intent, MainActivity.RC_ADD_CAR);
    }

    @Override
    public void displayMileage(double mileage) {
        Log.d(TAG, "displayMileage() mileage: " + mileage);
        totalMileagetv.setText(String.format("%.2f km", mileage));
    }


    @Override
    public void showFuelConsumptionExplanationDialog() {

        Log.d(TAG, "showFuelConsumptionExplanationDialog()");
        if (fuelConsumptionExplanationDialog == null) {
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
    public void onOfflineTryAgainClicked() {
        Log.d(TAG, "onOfflineTryAgainClicked()");
        presenter.onUpdateNeeded();
    }

    @OnClick(R.id.unknown_error_try_again)
    public void onUnknownTryAgainClicked() {
        Log.d(TAG, "onUnknownTryAgainClicked()");
        presenter.onUpdateNeeded();
    }

    @OnClick(R.id.fuel_consumption_row)
    public void onFuelConsumptionClicked() {
        Log.d(TAG, "onFuelConsumptionClicked()");
        presenter.onFuelConsumptionClicked();

    }

    @Override
    public void showFuelConsumed(double fuelCOnsumed) {
        Log.d(TAG, String.format("showFuelConsumed: %.2f", fuelCOnsumed));
        fuelConsumed.setText(Double.toString(fuelCOnsumed) + " L");
    }

    @Override
    public void onServiceBinded(@NotNull BluetoothAutoConnectService bluetoothAutoConnectService) {
        Log.d(TAG, "onServiceBinded()");
        this.presenter.onServiceBound(bluetoothAutoConnectService);
    }

    @OnClick(R.id.fuel_expense_row)
    public void onFuelExpensesClicked() {
        Log.d(TAG, "onFuelExpensesClicked()");
        presenter.onFuelExpensesClicked();
    }

    @Override
    public void hideBadge() {
        Log.d(TAG, "hideBadge()");
        alarmsCount.setVisibility(View.GONE);
    }

    @Override
    public void showBadges(int alarmCount) {
        Log.d(TAG, "showBadges, number of Badges: " + Integer.toString(alarmCount));
        alarmsCount.setVisibility(View.VISIBLE);
        if (alarmCount > 9)
            alarmsCount.setText("9+");
        else
            alarmsCount.setText(Integer.toString(alarmCount));


    }

    @Override
    public void showFuelExpense(float v) {
        Log.d(TAG, String.format("Show Fuel Expenses: $%.2f", v / 100));
        fuelExpensesTextView.setText(String.format("$%.2f", v / 100));

    }

    public String getLastKnowLocation() {
        Log.d(TAG, "getLastKnownLocation()");
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
        String provider = locationManager.getBestProvider(criteria, true);
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(provider, 1, 1, locationListener);
            String locationProvider = LocationManager.NETWORK_PROVIDER;
            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            if (lastKnownLocation == null) {
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

        } else {
            return null;
        }
    }

    @Override
    public void displayCarDetails(Car car) {
        Log.d(TAG, "displayCarDetails() car: " + car);
        carName.setText(car.getYear() + " " + car.getMake() + " "
                + car.getModel());
        totalMileagetv.setText(String.format("%.2f km", car.getTotalMileage()));
        mCarLogoImage.setVisibility(View.VISIBLE);
        mCarLogoImage.setImageResource(getCarSpecificLogo(car.getMake()));
        isPoppulated = true;
    }

    @Override
    public void showScannerID(String s) {
        Log.d(TAG, "showScannerID");
        scannerID.setText(s);

    }

    @Override
    public void displayDefaultDealershipVisuals(Dealership dealership) {
        Log.d(TAG, "displayDefaultDealershipVisual()");
        dealershipName.setText(dealership.getName());
        mDealerBanner.setImageResource(getDealerSpecificBanner(dealership.getName()));
        /*drivingAlarmsIcon.setImageResource(R.drawable.car_alarms_3x);*/
        mCarLogoImage.setVisibility(View.VISIBLE);
        dealershipName.setVisibility(View.VISIBLE);
        carName.setTextColor(Color.BLACK);
        dealershipName.setTextColor(Color.BLACK);
        carName.setTypeface(Typeface.DEFAULT_BOLD);
        carName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        mDealerBannerOverlay.setVisibility(View.VISIBLE);
    }


    @Override
    public void showFuelExpensesDialog() {
        Log.d(TAG, "showFuelExpensesDialog()");
        if (fuelExpensesAlertDialog == null) {
            final View dialogLayout = LayoutInflater.from(
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


    public static int getDealerSpecificBanner(String name) {
        Log.d(TAG, "getDealerSpecificBanner()");
        if (name.equalsIgnoreCase("Waterloo Dodge")) {
            return R.drawable.waterloo_dodge;
        } else if (name.equalsIgnoreCase("Galt Chrysler")) {
            return R.drawable.galt_chrysler;
        } else if (name.equalsIgnoreCase("GBAutos")) {
            return R.drawable.gbautos;
        } else if (name.equalsIgnoreCase("Cambridge Toyota")) {
            return R.drawable.cambridge_toyota;
        } else if (name.equalsIgnoreCase("Bay King Chrysler")) {
            return R.drawable.bay_king_chrysler;
        } else if (name.equalsIgnoreCase("Willowdale Subaru")) {
            return R.drawable.willowdale_subaru;
        } else if (name.equalsIgnoreCase("Parkway Ford")) {
            return R.drawable.parkway_ford;
        } else if (name.equalsIgnoreCase("Mountain Mitsubishi")) {
            return R.drawable.mountain_mitsubishi;
        } else if (name.equalsIgnoreCase("Subaru Of Maple")) {
            return R.drawable.subaru_maple;
        } else if (name.equalsIgnoreCase("Village Ford")) {
            return R.drawable.villageford;
        } else if (name.equalsIgnoreCase("Maple Volkswagen")) {
            return R.drawable.maple_volkswagon;
        } else if (name.equalsIgnoreCase("Toronto North Mitsubishi")) {
            return R.drawable.torontonorth_mitsubishi;
        } else if (name.equalsIgnoreCase("Kia Of Richmondhill")) {
            return R.drawable.kia_richmondhill;
        } else if (name.equalsIgnoreCase("Mercedes Benz Brampton")) {
            return R.drawable.mercedesbenz_brampton;
        } else if (name.equalsIgnoreCase("401DixieKia")) {
            return R.drawable.dixie_king;
        } else if (name.equalsIgnoreCase("Cambridge Honda")) {
            return R.drawable.cambridge_honda;
        } else {
            return R.drawable.no_dealership_background;
        }

    }

    public static int getCarSpecificLogo(String make) {
        Log.d(TAG, "getCarSpecificLogo()");
        if (make == null) return R.drawable.ford;
        if (make.equalsIgnoreCase("abarth")) {
            return R.drawable.abarth;
        } else if (make.equalsIgnoreCase("acura")) {
            return R.drawable.acura;
        } else if (make.equalsIgnoreCase("alfa romeo")) {
            return 0;
        } else if (make.equalsIgnoreCase("aston martin")) {
            return R.drawable.aston_martin;
        } else if (make.equalsIgnoreCase("audi")) {
            return R.drawable.audi;
        } else if (make.equalsIgnoreCase("bentley")) {
            return R.drawable.bentley;
        } else if (make.equalsIgnoreCase("bmw")) {
            return R.drawable.bmw;
        } else if (make.equalsIgnoreCase("buick")) {
            return R.drawable.buick;
        } else if (make.equalsIgnoreCase("cadillac")) {
            return R.drawable.cadillac;
        } else if (make.equalsIgnoreCase("chevrolet")) {
            return R.drawable.chevrolet;
        } else if (make.equalsIgnoreCase("chrysler")) {
            return R.drawable.chrysler;
        } else if (make.equalsIgnoreCase("dodge")) {
            return R.drawable.dodge;
        } else if (make.equalsIgnoreCase("ferrari")) {
            return R.drawable.ferrari;
        } else if (make.equalsIgnoreCase("fiat")) {
            return R.drawable.fiat;
        } else if (make.equalsIgnoreCase("ford")) {
            return R.drawable.ford;
        } else if (make.equalsIgnoreCase("gmc")) {
            return R.drawable.gmc;
        } else if (make.equalsIgnoreCase("honda")) {
            return R.drawable.honda;
        } else if (make.equalsIgnoreCase("hummer")) {
            return R.drawable.hummer;
        } else if (make.equalsIgnoreCase("hyundai")) {
            return R.drawable.hyundai;
        } else if (make.equalsIgnoreCase("infiniti")) {
            return R.drawable.infiniti;
        } else if (make.equalsIgnoreCase("jaguar")) {
            return R.drawable.jaguar;
        } else if (make.equalsIgnoreCase("jeep")) {
            return R.drawable.jeep;
        } else if (make.equalsIgnoreCase("kia")) {
            return R.drawable.kia;
        } else if (make.equalsIgnoreCase("landrover")) {
            return 0;//R.drawable.landrover;
        } else if (make.equalsIgnoreCase("lexus")) {
            return R.drawable.lexus;
        } else if (make.equalsIgnoreCase("lincoln")) {
            return R.drawable.lincoln;
        } else if (make.equalsIgnoreCase("maserati")) {
            return R.drawable.maserati;
        } else if (make.equalsIgnoreCase("mazda")) {
            return R.drawable.mazda;
        } else if (make.equalsIgnoreCase("mercedes-benz")) {
            return R.drawable.mercedes;
        } else if (make.equalsIgnoreCase("mercury")) {
            return R.drawable.mercury;
        } else if (make.equalsIgnoreCase("mini")) {
            return R.drawable.mini;
        } else if (make.equalsIgnoreCase("mitsubishi")) {
            return R.drawable.mitsubishi;
        } else if (make.equalsIgnoreCase("nissan")) {
            return R.drawable.nissan;
        } else if (make.equalsIgnoreCase("pontiac")) {
            return R.drawable.pontiac;
        } else if (make.equalsIgnoreCase("porsche")) {
            return R.drawable.porsche;
        } else if (make.equalsIgnoreCase("ram")) {
            return R.drawable.ram;
        } else if (make.equalsIgnoreCase("saab")) {
            return R.drawable.saab;
        } else if (make.equalsIgnoreCase("saturn")) {
            return R.drawable.saturn;
        } else if (make.equalsIgnoreCase("scion")) {
            return R.drawable.scion;
        } else if (make.equalsIgnoreCase("skota")) {
            return 0;//R.drawable.skota;
        } else if (make.equalsIgnoreCase("smart")) {
            return R.drawable.smart;
        } else if (make.equalsIgnoreCase("subaru")) {
            return R.drawable.subaru;
        } else if (make.equalsIgnoreCase("suzuki")) {
            return R.drawable.suzuki;
        } else if (make.equalsIgnoreCase("toyota")) {
            return R.drawable.toyota;
        } else if (make.equalsIgnoreCase("volkswagen")) {
            return R.drawable.volkswagen;
        } else if (make.equalsIgnoreCase("volvo")) {
            return R.drawable.volvo;
        } else {
            return 0;
        }
    }

    @OnClick(R.id.alarms_row)
    public void onTotalAlarmsClicked() {
        Log.d(TAG, "onTotalAlarmsClicked()");
        presenter.onTotalAlarmsClicked();
    }

    @Override
    public void openAlarmsActivity() {
        Log.d(TAG, "openAlarmsActivity");
        Intent intent = new Intent(getActivity(), AlarmsActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean("isMercedes", false);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    @Override
    public void showConfirmUpdateScannerDialog(String s) {

        // DO NOT DO LAMBDA EXPRESSIONS OR ONE TIME INITIALIZATION IN THIS BECAUSE THAT LEAVES THE
        //SCANNER ID AS FINAL AND DOESNT UPDATE CORRECTLY
        Log.d(TAG, "showConfirmUpdateScannerDialog()");
        View dialogLayout = LayoutInflater.from(
                getActivity()).inflate(R.layout.buy_device_dialog, null);
        confirmScannerUpdateDialog = new AnimatedDialogBuilder(getActivity())
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setTitle("Device Update")
                .setView(dialogLayout)
                .setMessage("Are you sure you want to pair this device with this car?")
                .setNegativeButton(getString(R.string.no_button_text), (dialog, which)
                        -> dialog.cancel())
                .setPositiveButton(getString(R.string.yes_button_text), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        presenter.onPairScannerConfirmClicked(s);
                    }
                })
                .create();

        confirmScannerUpdateDialog.show();

    }


    @Override
    public void showPairScannerDialog() {
        // DO NOT DO LAMBDA EXPRESSIONS OR ONE TIME INITIALIZATION IN THIS BECAUSE THAT LEAVES THE
        //SCANNER ID AS FINAL AND DOESNT UPDATE CORRECTLY
        Log.d(TAG, "showPairScannerDialog()");
        View dialogLayout = LayoutInflater.from(
                getActivity()).inflate(R.layout.dialog_input_scanner_id, null);
        TextInputEditText textInputEditText = (TextInputEditText) dialogLayout
                .findViewById(R.id.scanner_input);
        textInputEditText.setHint("Enter the device ID found on your Pitstop Device");
        pairScannerAlertDialog = new AnimatedDialogBuilder(getActivity())
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setTitle("Pair Scanner")
                .setView(dialogLayout)
                .setPositiveButton("Update Scanner", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "new new scanner id " + textInputEditText.getText().toString());
                        presenter.onUpdateScannerClicked(textInputEditText.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
                .create();
        pairScannerAlertDialog.show();

    }

    @Override
    public void showScannerAlreadyActiveDialog() {

        Log.d(TAG, "showScannerAlreadyActiveDialog()");
        if (scannerAlreadyActiveDialog == null) {
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            scannerAlreadyActiveDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Device Already Active")
                    .setView(dialogLayout)
                    .setMessage("It seems as this device is already paired with another car. Please check your " +
                            "device ID again or contact us for additional support")
                    .setPositiveButton(getString(R.string.ok_button), (dialog, which)
                            -> dialog.cancel())
                    .create();
        }
        scannerAlreadyActiveDialog.show();

    }
}
