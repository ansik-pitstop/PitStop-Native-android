package com.pitstop.ui.service_request;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v7.app.AppCompatActivity;

import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.ui.service_request.view_fragment.time_picker_view.TimePickerFragment;
import com.pitstop.ui.service_request.view_fragment.main_from_view.ServiceFormFragment;

/**
 * Created by Matthew on 2017-07-11.
 */

public class RequestServiceActivity extends AppCompatActivity implements RequestServiceView,RequestServiceCallback {


    public static final String EXTRA_CAR = "extra_car";
    public static final String EXTRA_FIRST_BOOKING = "is_first_booking";
    public static final String STATE_TENTATIVE = "tentative";
    public static final String STATE_REQUESTED = "requested";


    private ServiceFormFragment serviceFormFragment;
    private TimePickerFragment timePickerFragment;

    private RequestServicePresenter presenter;

    private FragmentManager fragmentManager;

    private Car dashCar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_service);

        dashCar = getIntent().getParcelableExtra(EXTRA_CAR);

        fragmentManager = getFragmentManager();

        presenter = new RequestServicePresenter(this);
        presenter.subscribe(this);

        serviceFormFragment = new ServiceFormFragment();
        serviceFormFragment.setActivityCallback(this);
        serviceFormFragment.setCar(dashCar);

        timePickerFragment = new TimePickerFragment();
        timePickerFragment.setActivityCallback(this);

        presenter.setViewMainForm();
    }

    @Override
    public void setViewMainForm() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.request_service_fragment_holder,serviceFormFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void setViewTimePicker() {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.request_service_fragment_holder,timePickerFragment);
        fragmentTransaction.addToBackStack("time_picker");
        fragmentTransaction.commit();
    }
}
