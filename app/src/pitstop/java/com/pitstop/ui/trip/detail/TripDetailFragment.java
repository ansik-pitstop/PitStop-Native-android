package com.pitstop.ui.trip.detail;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

    private TripDetailPresenter presenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_trip_list, null);
        ButterKnife.bind(this, view);

        if (presenter == null) {
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();

            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication) getActivity().getApplicationContext());

            presenter = new TripDetailPresenter(useCaseComponent, mixpanelHelper);
        }
//        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        presenter.subscribe(this);
        presenter.loadTripData(null);
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

        milesNum.setText(String.valueOf((int) trip.getMileageAccum()));

        // Calculate minutes using timestamps
        minutesNum.setText(String.valueOf((int) 2));

        fuelNum.setText(String.valueOf((int) trip.getFuelConsumptionAccum()));

    }
}
