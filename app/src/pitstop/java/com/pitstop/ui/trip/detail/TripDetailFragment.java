package com.pitstop.ui.trip.detail;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
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
import com.pitstop.ui.trip.TripsFragment;
import com.pitstop.ui.trip.TripsView;
import com.pitstop.utils.AnimatedDialogBuilder;
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

    private AlertDialog deleteTripAlertDialog;

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

            if (getParentFragment() instanceof TripsView) {
                presenter.setCommunicationInteractor(((TripsFragment) getParentFragment()).getPresenter());
            }

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
    public void onDetach() {
        super.onDetach();

        if (getParentFragment() instanceof TripsView) {
            presenter.setCommunicationInteractor(null);
        }
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

    @Override
    public void onCloseView() {

        Log.d(TAG, "onCloseView()");

        closeView();

    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    private void closeView() {

        Log.d(TAG, "closeView()");

        FragmentManager manager = ((Fragment) this).getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove((Fragment) this);
        transaction.commit();

    }

    @OnClick(R.id.button_trip_detail_back)
    public void onBackButtonClick() {

        Log.d(TAG, "onBackButtonClick()");

        closeView();

    }

    @OnClick(R.id.button_trip_detail_delete)
    public void onDeleteButtonClick() {

        Log.d(TAG, "onDeleteButtonClick()");

        if (deleteTripAlertDialog == null) {
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.buy_device_dialog, null);
            deleteTripAlertDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle(R.string.delete_trip)
                    .setView(dialogLayout)
                    .setMessage(R.string.remove_trip_confirm_message)
                    .setPositiveButton(getString(R.string.yes_button_text), (dialog, which)
                            -> presenter.onDeleteTripClicked(trip))
                    .setNegativeButton(getString(R.string.no_button_text), (dialog, which) -> dialog.cancel())
                    .create();
        }
        deleteTripAlertDialog.show();

    }
}
