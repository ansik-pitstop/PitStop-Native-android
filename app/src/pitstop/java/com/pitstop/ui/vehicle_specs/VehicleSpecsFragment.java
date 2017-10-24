package com.pitstop.ui.vehicle_specs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.custom_shops.CustomShopActivity;
import com.pitstop.ui.dashboard.DashboardFragment;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.my_garage.MyGarageFragment;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;



/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsFragment extends Fragment implements VehicleSpecsView {
    public static final String TAG = VehicleSpecsFragment.class.getSimpleName();

    public static final String PITSTOP_AMAZON_LINK = "https://www.amazon.ca/gp/product/B012GWJQZE";
    public static final String CAR_DELETED = "deleted";
    public static final String CAR_POSITION ="position" ;
    public static final String CAR_SELECTED ="carCurrent" ;

    public static final int START_CUSTOM = 347;
    private AlertDialog buyDeviceDialog;
    private AlertDialog licensePlateDialog;
    private AlertDialog dealershipAlertDialog;
    private AlertDialog deleteCarAlertDialog;
    private AlertDialog changeDealershipAlertDialog;
    private VehicleSpecsPresenter presenter;
    private AlertDialog currentCarConfirmDialog;

    @BindView(R.id.swiper)
    protected SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.car_logo_imageview)
    protected ImageView carLogo;

    @BindView(R.id.car_name_banner)
    protected TextView carName;

    @BindView(R.id.main_view_lin_layout)
    protected LinearLayout mainLayout;

    @BindView(R.id.main_linear_layout)
    protected LinearLayout mainLinearLayout;

    @BindView(R.id.loading_view_main)
    protected View loadingView;

    @BindView(R.id.banner_overlay)
    protected FrameLayout bannerOverlay;

    @BindView(R.id.dealership_name_banner)
    protected TextView dealershipName;

    @BindView(R.id.background_image)
    protected ImageView carPic;

    @BindView(R.id.dealership_tv)
    protected TextView dealership;

    @BindView(R.id.dealership_view)
    protected View dealerhsipView;

    @BindView(R.id.car_vin)
    protected TextView carVin;

    @BindView(R.id.scanner_view)
    protected View scannerView;


    @BindView(R.id.delete_car)
    protected View deleteCarView;

    @BindView(R.id.scanner_id)
    protected TextView scannerID;

    @BindView(R.id.car_license_plate_specs)
    protected TextView licensePlate;

    @BindView(R.id.car_engine)
    protected TextView engine;

    @BindView(R.id.city_mileage_specs)
    protected TextView cityMileage;

    @BindView(R.id.highway_mileage_specs)
    protected TextView highwayMileage;

    @BindView(R.id.license_plate_cardview)
    protected View plateView;

    @BindView(R.id.trim_card_view)
    protected View trimView;

    @BindView(R.id.trim)
    protected TextView trim;

    @BindView(R.id.tank_size_card_view)
    protected View tankSizeView;

    @BindView(R.id.tank_size)
    protected TextView tankSize;

    @BindView(R.id.progress)
    protected View imageLoadingView;


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
    }


    @Override
    public void showLoading() {
        if (!swipeRefreshLayout.isRefreshing()) {
            mainLinearLayout.setGravity(Gravity.CENTER);
            mainLinearLayout.setVerticalGravity(Gravity.CENTER_VERTICAL);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            mainLinearLayout.setLayoutParams(params);
            Log.d(TAG, "showLoading()");
            mainLayout.setVisibility(View.GONE);
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
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.NO_GRAVITY;
            mainLinearLayout.setLayoutParams(params);
            loadingView.setVisibility(View.GONE);
            mainLayout.setVisibility(View.VISIBLE);
        }
        else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void showImage(String s) {
        Log.d(TAG, "showImage()");
        carLogo.setVisibility(View.GONE);
        dealershipName.setVisibility(View.GONE);
        carName.setVisibility(View.GONE);
        if (getActivity()!=null)
            Picasso.with(getActivity()).load(s).into(carPic);
    }
/*
    public void setCar(){
        this.myCar.setCurrentCar(bundle.getBoolean(IS_CURRENT_KEY));
        this.myCar.setId(bundle.getInt(CAR_ID_KEY));
        this.myCar.setVin(bundle.getString(CAR_VIN_KEY));
        this.myCar.setScannerId(bundle.getString(SCANNER_ID_KEY));
        this.myCar.setEngine(bundle.getString(ENGINE_KEY));
        this.myCar.setCityMileage(bundle.getString(CITY_MILEAGE_KEY));
        this.myCar.setHighwayMileage(bundle.getString(HIGHWAY_MILEAGE_KEY));
        this.myCar.setTrim(bundle.getString(TRIM_KEY));
        this.myCar.setTankSize(bundle.getString(TANK_SIZE_KEY));
        this.myCar.setYear(bundle.getInt(YEAR_KEY));
        this.myCar.setMake(bundle.getString(MAKE_KEY));
        this.myCar.setModel(bundle.getString(MODEL_KEY));
    }*/

    @Override
    public void setCarView(Car car) {
        Log.d(TAG, "setView()");

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

        if(!(car.getDealership()== null)) {
            dealerhsipView.setVisibility(View.VISIBLE);
            dealership.setText(car.getDealership().getName());
        }
        else{
            dealerhsipView.setVisibility(View.GONE);
        }
        presenter.getLicensePlate(car.getId());
    }

    @Override
    public void showLicensePlate(String s) {
        Log.d(TAG, "showLicensePlate()");
        licensePlate.setText(s);
        licensePlate.setVisibility(View.VISIBLE);
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
        carLogo.setVisibility(View.VISIBLE);
        dealershipName.setVisibility(View.VISIBLE);
        carName.setVisibility(View.VISIBLE);
        carName.setText(Integer.toString(presenter.getCar().getYear()) + " " +
                        presenter.getCar().getMake() + " " +
                        presenter.getCar().getModel());
        carLogo.setImageResource(DashboardFragment.getCarSpecificLogo( presenter.getCar().getMake()));
        dealershipName.setText(presenter.getCar().getDealership().getName());
        carPic.setImageResource(DashboardFragment.getDealerSpecificBanner(presenter.getCar().getDealership().getName()));
        bannerOverlay.setVisibility(View.VISIBLE);
    }




    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult()");
        if(requestCode == START_CUSTOM && (resultCode == AddCarActivity.ADD_CAR_SUCCESS_HAS_DEALER)) {
            dealership.setText(data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY));
            dealershipName.setText(data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY));
            if (carPicgetError) {
                showDealershipBanner();
                dealershipName.setText(data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY));
                carPic.setImageResource(DashboardFragment.getDealerSpecificBanner(data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY)));
            }
            Log.d(TAG,data.getStringExtra(CustomShopActivity.DEALERSHIP_NAME_KEY) );

        }
    }


    @OnClick(R.id.license_plate_cardview)
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

    @OnClick(R.id.scanner_view)
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
        if (presenter.getCar()!=null) {
            Intent intent = new Intent(getActivity(), CustomShopActivity.class);
            intent.putExtra(MainActivity.CAR_EXTRA, presenter.getCar());
            startActivityForResult(intent, START_CUSTOM);
        }
    }

    @OnClick(R.id.dealership_view)
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

    public void showImageLoading(){
        imageLoadingView.bringToFront();
        imageLoadingView.setVisibility(View.VISIBLE);
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
        imageLoadingView.setVisibility(View.GONE);
    }





}
