package com.pitstop.ui.vehicle_specs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalSpecsStorage;
import com.pitstop.database.TABLES;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.ui.Notifications.NotificationsPresenter;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsFragment extends android.app.Fragment implements VehicleSpecsView {
    public static final String TAG = VehicleSpecsFragment.class.getSimpleName();

    public static final String CAR_ID_KEY = "carid";
    public static final String CAR_VIN_KEY = "carVin";
    public static final String SCANNER_ID_KEY = "scannerId";
    public static final String CITY_MILEAGE_KEY = "cityMieage";
    public static final String HIGHWAY_MILEAGE_KEY = "highwayMileage";
    public static final String ENGINE_KEY = "engine";
    public static final String TRIM_KEY = "trim";
    public static final String TANK_SIZE_KEY = "tankSize";
    public static final String DEALERSHIP_KEY = "dealership";


    public static final String PITSTOP_AMAZON_LINK = "https://www.amazon.ca/gp/product/B012GWJQZE";


    private int carId;
    private AlertDialog buyDeviceDialog;
    private AlertDialog licensePlateDialog;
    private VehicleSpecsPresenter presenter;

    @BindView(R.id.dealership_tv)
    TextView dealership;

    @BindView(R.id.dealership_view)
    View dealerhsipView;


    @BindView(R.id.car_vin)
    TextView carVin;

    @BindView(R.id.scanner_view)
    View scannerView;

    @BindView(R.id.scanner_id)
    TextView scannerID;

    @BindView(R.id.car_license_plate_specs)
    TextView licensePlate;

    @BindView(R.id.car_engine)
    TextView engine;

    @BindView(R.id.city_mileage_specs)
    TextView cityMileage;

    @BindView(R.id.highway_mileage_specs)
    TextView highwayMileage;

    @BindView(R.id.license_plate_cardview)
    View plateView;

    @BindView(R.id.trim_card_view)
    View trimView;

    @BindView(R.id.trim)
    TextView trim;

    @BindView(R.id.tank_size_card_view)
    View tankSizeView;

    @BindView(R.id.tank_size)
    TextView tankSize;

    Bundle bundle;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view  = inflater.inflate(R.layout.vehicle_specs_fragment, null);
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
        this.carId = bundle.getInt(CAR_ID_KEY);
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

    public void setView(){
        Log.d(TAG, "setView()");
        carVin.setText(bundle.getString(CAR_VIN_KEY));
        if (bundle.getString(SCANNER_ID_KEY) == null)
            scannerID.setText("No scanner connected");
        else
            scannerID.setText(bundle.getString(SCANNER_ID_KEY));
        if (bundle.getString(ENGINE_KEY) == null){

            engine.setVisibility(View.GONE);
        }
        else
            engine.setText(bundle.getString(ENGINE_KEY));
        cityMileage.setText(bundle.getString(CITY_MILEAGE_KEY));
        highwayMileage.setText(bundle.getString(HIGHWAY_MILEAGE_KEY));
        if (bundle.getString(TRIM_KEY) == null)
            trimView.setVisibility(View.GONE);
        else
            trim.setText(bundle.getString(TRIM_KEY));

        if (bundle.getString(TANK_SIZE_KEY) == null)
            tankSizeView.setVisibility(View.GONE);
        else
            tankSize.setText(bundle.getString(TANK_SIZE_KEY));

        if(bundle.getString(DEALERSHIP_KEY)!= null) {
            dealerhsipView.setVisibility(View.VISIBLE);
            dealership.setText(bundle.getString(DEALERSHIP_KEY));
        }
        else{
            dealerhsipView.setVisibility(View.GONE);
        }
    }

    @Override
    public void showLicensePlate(String s) {
        Log.d(TAG, "showLicensePlate()");
        licensePlate.setText(s);
        licensePlate.setVisibility(View.VISIBLE);
    }

    @Override
    public void toast(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

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
                            -> presenter.onUpdateLicensePlateDialogConfirmClicked(carId,
                            textInputEditText.getText().toString()))
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel())
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

    private void showBuyDeviceDialog() {
        Log.d(TAG, "showBuyDeviceDialog()");
        if (buyDeviceDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            buyDeviceDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Purchase Pitstop Device")
                    .setView(dialogLayout)
                    .setMessage("It appears you do not have a Pitstop device paired to this " +
                                "car.With the device,we can trach your car's engine " +
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
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PITSTOP_AMAZON_LINK));
        startActivity(browserIntent);
    }


}
