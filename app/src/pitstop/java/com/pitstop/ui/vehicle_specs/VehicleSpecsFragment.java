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


    @BindView(R.id.car_vin)
    TextView carVin;

    @BindView(R.id.scanner_id)
    TextView scannerID;

    @BindView(R.id.car_license_plate_specs)
    TextView licensePlate;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view  = inflater.inflate(R.layout.vehicle_specs_fragment, null);
        ButterKnife.bind(this, view);
        Bundle bundle  = getArguments();


        carVin.setText(bundle.getString(CAR_VIN_KEY));
        scannerID.setText(bundle.getString(SCANNER_ID_KEY));
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
    }
}
