package com.pitstop.ui.vehicle_specs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsFragment extends android.app.Fragment implements VehicleSpecsView {
    public static final String TAG = VehicleSpecsFragment.class.getSimpleName();

    public static final String CAR_VIN_KEY = "carVin";
    public static final String SCANNER_ID_KEY = "scannerId";
    public static final String CITY_MILEAGE_KEY = "cityMieage";
    public static final String HIGHWAY_MILEAGE_KEY = "highwayMileage";
    public static final String ENGINE_KEY = "engine";
    public static final String TRIM_KEY = "trim";
    public static final String TANK_SIZE_KEY = "tankSize";

    @BindView(R.id.car_vin)
    TextView carVin;

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

        setView();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
    }

    public void setView(){

        carVin.setText(bundle.getString(CAR_VIN_KEY));
        if (bundle.getString(SCANNER_ID_KEY) == null)
            scannerID.setText("No scanner connected");
        else
            scannerID.setText(bundle.getString(SCANNER_ID_KEY));
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
    }





}
