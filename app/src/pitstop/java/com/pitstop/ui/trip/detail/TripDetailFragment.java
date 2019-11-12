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
import android.widget.Toast;

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
import com.pitstop.utils.UnitOfLength;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by David C. on 14/3/18.
 */

public class TripDetailFragment extends Fragment implements TripDetailView {

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.textview_km_num)
    protected TextView milesNum;

    @BindView(R.id.textview_km_label)
    protected TextView mileageLabel;

    @BindView(R.id.textview_mins_num)
    protected TextView minutesNum;

//    @BindView(R.id.textview_fuel_num)
//    protected TextView fuelNum;

    @BindView(R.id.textview_street_location)
    protected TextView streetLocation;

    @BindView(R.id.textview_country_location)
    protected TextView countryLocation;

    private TripDetailPresenter presenter;

    private Trip trip;

    private AlertDialog deleteTripAlertDialog;
    private AlertDialog offlineAlertDialog;
    private AlertDialog unknownErrorDialog;

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
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        presenter.subscribe(this);
        super.onViewCreated(view, savedInstanceState);
        this.loadTripData(trip);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");
        super.onDestroyView();
        presenter.unsubscribe();
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
        Log.d(TAG, "displayOfflineErrorDialog()");
        Toast.makeText(getContext(),R.string.offline_error,Toast.LENGTH_LONG).show();
    }

    @Override
    public void displayUnknownErrorDialog() {
        Log.d(TAG, "displayUnknownErrorDialog()");
        Toast.makeText(getContext(),R.string.unknown_error,Toast.LENGTH_LONG).show();
    }

    @Override
    public void displayUnknownErrorView() {
        // nothing to do here
    }

    @Override
    public void displayOfflineView() {
        // nothing to do here
    }

    @Override
    public void displayOnlineView() {
        // nothing to do here
    }

    @Override
    public void loadTripData(Trip trip) {

        if (trip == null) {
            return;
        }

        String unknown = getResources().getString(R.string.unknown);

        String startStreet = (trip.getLocationStart().getStartStreetLocation() != null ? trip.getLocationStart().getStartStreetLocation() : unknown);
        String endStreet = (trip.getLocationEnd().getEndStreetLocation() != null ? trip.getLocationEnd().getEndStreetLocation() : unknown);
        String startCity = (trip.getLocationStart().getStartCityLocation() != null ? trip.getLocationStart().getStartCityLocation() : unknown);
        String endCity = (trip.getLocationEnd().getEndCityLocation() != null ? trip.getLocationEnd().getEndCityLocation() : unknown);
        String startCountry = (trip.getLocationStart().getStartLocation() != null ? trip.getLocationStart().getStartLocation() : unknown);;
        String endCountry = (trip.getLocationEnd().getEndLocation() != null ? trip.getLocationEnd().getEndLocation() : unknown);

        streetLocation.setText(startStreet + " - " + endStreet);
        countryLocation.setText(startCity + ", " + startCountry + " - " + endCity + ", " + endCountry);

//        streetLocation.setText("Trip ID: " + trip.getTripId());
//        countryLocation.setText("VIN: " + trip.getVin());


        presenter.setMileage(String.format(Locale.getDefault(), "%.2f",trip.getMileageAccum()));

        Log.d(TAG,"trip time start: "+trip.getTimeStart()+", trip time end: "+trip.getTimeEnd());

        // Calculate minutes using timestamps
        long totalMinutes = trip.getTripLength() / 60;
        minutesNum.setText(String.valueOf(totalMinutes));


//        fuelNum.setText(String.valueOf((int) trip.getFuelConsumptionAccum()));

    }

    @Override
    public void setTripMileage(String mileage) {
        milesNum.setText(mileage);
    }

    public void setUnitOfLength(UnitOfLength unitOfLength) {
        mileageLabel.setText(unitOfLength.uiString());
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
                    .setMessage(R.string.delete_trip_confirm_message)
                    .setPositiveButton(getString(R.string.yes_button_text), (dialog, which)
                            -> presenter.onDeleteTripClicked(trip))
                    .setNegativeButton(getString(R.string.no_button_text), (dialog, which) -> dialog.cancel())
                    .create();
        }
        deleteTripAlertDialog.show();

    }
}
