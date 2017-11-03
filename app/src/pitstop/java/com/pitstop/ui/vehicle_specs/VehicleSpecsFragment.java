package com.pitstop.ui.vehicle_specs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.pitstop.ui.main_activity.MainActivity.CAR_EXTRA;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsFragment extends android.app.Fragment implements VehicleSpecsView {
    public static final String TAG = VehicleSpecsFragment.class.getSimpleName();
    public static final String CAR_POSITION_KEY = "position";
    public static final String CAR_ID_KEY = "carid";
    public static final String CAR_VIN_KEY = "carVin";
    public static final String SCANNER_ID_KEY = "scannerId";
    public static final String CITY_MILEAGE_KEY = "cityMieage";
    public static final String HIGHWAY_MILEAGE_KEY = "highwayMileage";
    public static final String ENGINE_KEY = "engine";
    public static final String TRIM_KEY = "trim";
    public static final String TANK_SIZE_KEY = "tankSize";
    public static final String DEALERSHIP_KEY = "dealership";
    public static final String MODEL_KEY = "model";
    public static final String MAKE_KEY = "make";
    public static final String YEAR_KEY = "year";
    public static final String PITSTOP_AMAZON_LINK = "https://www.amazon.ca/gp/product/B012GWJQZE";
    public static final String IS_CURRENT_KEY = "isCurrent?";
    public static final String CAR_DELETED = "deleted";
    public static final String CAR_POSITION ="position" ;
    public static final String CAR_SELECTED ="carCurrent" ;

    public static final int START_CUSTOM = 347;
    private int carId;
    private AlertDialog buyDeviceDialog;
    private AlertDialog licensePlateDialog;
    private AlertDialog dealershipAlertDialog;
    private AlertDialog deleteCarAlertDialog;
    private AlertDialog changeDealershipAlertDialog;
    private VehicleSpecsPresenter presenter;
    private AlertDialog currentCarConfirmDialog;
    private Car myCar;

    @BindView(R.id.car_logo_imageview)
    protected ImageView carLogo;

    @BindView(R.id.car_name_banner)
    protected TextView carName;

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

    @BindView(R.id.make_car_current)
    protected View selectCarAsCurrent;

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

    private Bundle bundle;

    private boolean carPicgetError;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view  = inflater.inflate(R.layout.fragment_vehicle_specs, null);
        ButterKnife.bind(this, view);
        bundle  = getArguments();
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
        this.carId = bundle.getInt(CAR_ID_KEY);
        this.myCar = new Car();
        setCar();
        setView();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        presenter.subscribe(this);
        Log.d(TAG, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.getLicensePlate(carId);
        presenter.getCarImage(bundle.getString(CAR_VIN_KEY));
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView()");
        presenter.unsubscribe();
        super.onDestroyView();
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



    }

    public void setView(){
        Log.d(TAG, "setView()");
        if (bundle.getBoolean(IS_CURRENT_KEY)){
            selectCarAsCurrent.setVisibility(View.GONE);
        }
        carVin.setText(bundle.getString(CAR_VIN_KEY));
        if (this.myCar.getScannerId() == null)
            scannerID.setText(getString(R.string.no_scanner_connected));
        else
            scannerID.setText(this.myCar.getScannerId());
        if (bundle.getString(ENGINE_KEY) == null){
            engine.setVisibility(View.GONE);
        }
        else
            engine.setText(this.myCar.getEngine());

        cityMileage.setText(this.myCar.getCityMileage());
        highwayMileage.setText(this.myCar.getHighwayMileage());
        if (bundle.getString(TRIM_KEY) == null)
            trimView.setVisibility(View.GONE);
        else
            trim.setText(this.myCar.getTrim());

        if (bundle.getString(TANK_SIZE_KEY) == null)
            tankSizeView.setVisibility(View.GONE);
        else
            tankSize.setText(this.myCar.getTankSize());

        if(bundle.getString(DEALERSHIP_KEY)!= null) {
            dealerhsipView.setVisibility(View.VISIBLE);
            dealership.setText(bundle.getString(DEALERSHIP_KEY));
        }
        else{
            dealerhsipView.setVisibility(View.GONE);
        }
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(Integer.valueOf(this.myCar.getYear()) + " " +
                        this.myCar.getMake() + " " + this.myCar.getModel());
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
        carName.setText(Integer.toString(myCar.getYear()) + " " +
                        myCar.getMake() + " " +
                        myCar.getModel());
        carLogo.setImageResource(DashboardFragment.getCarSpecificLogo( myCar.getMake()));
        dealershipName.setText(bundle.getString(DEALERSHIP_KEY));
        carPic.setImageResource(DashboardFragment.getDealerSpecificBanner(bundle.getString(DEALERSHIP_KEY)));
        bannerOverlay.setVisibility(View.VISIBLE);
    }

    @Override
    public void closeSpecsFragmentAfterDeletion() {
        Log.d(TAG, "closeSpecsFragmentAfterDeletion");
        Intent resultIntent = new Intent();
        resultIntent.putExtra(CAR_DELETED, true);
        resultIntent.putExtra(CAR_SELECTED, false);
        resultIntent.putExtra(CAR_POSITION, bundle.getInt(CAR_POSITION_KEY));
        if(getActivity() == null) return;
        getActivity().setResult(Activity.RESULT_OK, resultIntent);
        getActivity().finish();
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

    @Override
    public void closeSpecsFragmentAfterSettingCurrent() {
        Log.d(TAG, "closeSpecsFragmentAfterSettingCurrent");
        Intent resultIntent = new Intent();
        resultIntent.putExtra(CAR_DELETED, false);
        resultIntent.putExtra(CAR_SELECTED, true);
        resultIntent.putExtra(CAR_POSITION, bundle.getInt(CAR_POSITION_KEY));
        if(getActivity() == null) return;
        getActivity().setResult(Activity.RESULT_OK, resultIntent);
        getActivity().finish();
    }

    @OnClick(R.id.license_plate_cardview)
    public void showLicensePlateDialog(){
        Log.d(TAG, "showLicensePlateDialog()");
        if (licensePlateDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dialog_input_license_plate, null);
            final TextInputEditText textInputEditText = (TextInputEditText)dialogLayout
                    .findViewById(R.id.plate_input);
            textInputEditText.setHint(getString(R.string.license_plate));
            licensePlateDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(getString(R.string.update_license_plate))
                    .setView(dialogLayout)
                    .setPositiveButton(getString(R.string.confirm_button), (dialog, which)
                            -> presenter.onUpdateLicensePlateDialogConfirmClicked(carId,
                            textInputEditText.getText().toString()))
                    .setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> dialog.cancel())
                    .create();
        }
        licensePlateDialog.show();
    }

    @OnClick(R.id.scanner_view)
    public void onScannerViewClicked(){
        Log.d(TAG, "onScannerViewClicked()");
        if (bundle.getString(SCANNER_ID_KEY) == null){
            showBuyDeviceDialog();
        }
    }

    @OnClick(R.id.make_car_current)
    public void onMakeCarCurrentClicked(){
        Log.d(TAG, "makeCarCurrentClicked");
        if(currentCarConfirmDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            currentCarConfirmDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Select as Current Car")
                    .setView(dialogLayout)
                    .setMessage("Are you sure you want to select this as your current car?")
                    .setPositiveButton("Yes", (dialog, which)
                            -> presenter.makeCarCurrent(bundle.getInt(CAR_ID_KEY)))
                    .setNegativeButton("No", (dialog, which) -> dialog.cancel())
                    .create();
        }
        currentCarConfirmDialog.show();

    }

    @OnClick(R.id.delete_car)
    public void onDeleteCarClicked(){
        Log.d(TAG, "deleteCarClicked");
        if (deleteCarAlertDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            deleteCarAlertDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(getString(R.string.car_delete))
                    .setView(dialogLayout)
                    .setMessage(getString(R.string.delete_car))
                    .setPositiveButton(getString(R.string.yes_button_text), (dialog, which)
                            -> presenter.deleteCar(bundle.getInt(CAR_ID_KEY)))
                    .setNegativeButton(getString(R.string.no_button_text), (dialog, which) -> dialog.cancel())
                    .create();
        }
        deleteCarAlertDialog.show();
    }

    private void showBuyDeviceDialog() {
        Log.d(TAG, "showBuyDeviceDialog()");
        if (buyDeviceDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            buyDeviceDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(getString(R.string.purchase_pitstop_device))
                    .setView(dialogLayout)
                    .setMessage(getString(R.string.purchase_device_message))
                    .setPositiveButton(getString(R.string.purchase_pitstop_device), (dialog, which)
                            -> openPitstopAmazonLink())
                    .setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> dialog.cancel())
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
        Intent intent = new Intent(getActivity(), CustomShopActivity.class);
        intent.putExtra(CAR_EXTRA,this.myCar);
        startActivityForResult(intent,START_CUSTOM);
    }

    @OnClick(R.id.dealership_view)
    public void showDealershipChangeDialog(){
        if (changeDealershipAlertDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            changeDealershipAlertDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(getString(R.string.change_dealership))
                    .setView(dialogLayout)
                    .setMessage(getString(R.string.change_dealerhsip_confirmation))
                    .setPositiveButton(getString(R.string.ok_button), (dialog, which)
                            -> startCustomShop())
                    .setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> dialog.cancel())
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
