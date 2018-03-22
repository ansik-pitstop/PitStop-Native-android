package com.pitstop.ui.trip.detail;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.trip.Trip;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by David C. on 14/3/18.
 */

public class TripDetailFragment extends Fragment implements TripDetailView {

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.textview_miles_num)
    protected TextView milesNum;

    @BindView(R.id.textview_mins_num)
    protected TextView minutesNum;

    @BindView(R.id.textview_fuel_num)
    protected TextView fuelNum;

    @BindView(R.id.textview_street_location)
    protected TextView streetLocation;

    @BindView(R.id.textview_country_location)
    protected TextView countryLocation;

    private TripDetailPresenter presenter;

    private Trip trip;

    public TripDetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_trip_detail, null);
        ButterKnife.bind(this, view);

        if (presenter == null) {
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();

            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication) getActivity().getApplicationContext());

            presenter = new TripDetailPresenter(useCaseComponent, mixpanelHelper);

            this.loadTripData(trip);
        }
//        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        presenter.subscribe(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void displayOfflineErrorDialog() {

    }

    @Override
    public void displayUnknownErrorDialog() {

    }

    @Override
    public void displayUnknownErrorView() {

    }

    @Override
    public void displayOfflineView() {

    }

    @Override
    public void displayOnlineView() {

    }

    @Override
    public void loadTripData(Trip trip) {

        if (trip == null) {
            return;
        }

        streetLocation.setText("Trip ID: " + trip.getTripId());

        countryLocation.setText("VIN: " + trip.getVin());

        milesNum.setText(String.valueOf(trip.getMileageAccum()));

        // Calculate minutes using timestamps
        long totalTimestamp = Long.valueOf(trip.getTimeEnd()) - Long.valueOf(trip.getTimeStart());
        long totalMinutes = totalTimestamp / (1000 * 60);
        minutesNum.setText(String.valueOf(totalMinutes));

        fuelNum.setText(String.valueOf((int) trip.getFuelConsumptionAccum()));

    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    @OnClick(R.id.button_trip_detail_back)
    public void onBackButtonClick() {

        FragmentManager manager = ((Fragment) this).getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove((Fragment) this);
        transaction.commit();

    }
}
