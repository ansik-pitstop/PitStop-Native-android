package com.pitstop.ui.trip;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.model.PolylineOptions;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.trip.Trip;
import com.pitstop.ui.MapView;
import com.pitstop.ui.trip.detail.TripDetailFragment;
import com.pitstop.ui.trip.list.TripListFragment;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by David C. on 10/3/18.
 */

public class TripsFragment extends Fragment implements TripsView {

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.unknown_error_view)
    protected View unknownErrorView;

    @BindView(R.id.no_trip_view)
    protected View noTripView;

    @BindView(R.id.loading_spinner)
    protected View loadingSpinner;

    @BindView(R.id.linear_container)
    protected View mainLayout;

    @BindView(R.id.mapview_trips)
    protected MapView mapView;

    private TripsPresenter presenter;

    private TripListFragment tripListFragment;
    private TripDetailFragment tripDetailFragment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_trips, null);
        ButterKnife.bind(this, view);

        if (presenter == null) {
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();

            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication) getActivity().getApplicationContext());

            presenter = new TripsPresenter(useCaseComponent, mixpanelHelper);
        }

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        presenter.subscribe(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);

        tripListFragment = new TripListFragment();

        getChildFragmentManager().beginTransaction().add(R.id.fragment_container, tripListFragment).commit();

    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        presenter.unsubscribe();
    }

    @Override
    public void showLoading() {

        Log.d(TAG, "showLoading()");
        loadingSpinner.setVisibility(View.VISIBLE);

    }

    @Override
    public void hideLoading() {

        Log.d(TAG, "hideLoading()");
        loadingSpinner.setVisibility(View.GONE);

    }

    @Override
    public void hideRefreshing() { // TODO: implement

    }

    @Override
    public boolean isRefreshing() { // TODO: implement
        return false;
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
    public void noTrips() {

        Log.d(TAG, "noTrips()");
        mainLayout.setVisibility(View.GONE);
        loadingSpinner.setVisibility(View.GONE);
        unknownErrorView.setVisibility(View.GONE);
        //offlineView.setVisibility(View.GONE);
        noTripView.setVisibility(View.VISIBLE);
        loadingSpinner.bringToFront();

    }

    @Override
    public void thereAreTrips() {

        if (noTripView.getVisibility() == View.VISIBLE) {

            mainLayout.setVisibility(View.VISIBLE);
            noTripView.setVisibility(View.GONE);

        }

    }

    @Override
    public void displayTripPolylineOnMap(PolylineOptions polylineOptions) {

        mapView.addPolyline(polylineOptions);

    }

    @Override
    public void displayTripDetailsView(Trip trip) {

        // Replace the child fragment (Trip List) for the Trip Details View
        tripDetailFragment = new TripDetailFragment();
        tripDetailFragment.setTrip(trip);

        getChildFragmentManager().beginTransaction().add(R.id.fragment_container, tripDetailFragment).commit();

    }

    @Override
    public void requestForDataUpdate() {

        Log.d(TAG, "requestForDataUpdate()");

        FragmentManager fragmentManager = getChildFragmentManager();
        TripListFragment childListFragment = (TripListFragment) fragmentManager.findFragmentById(R.id.fragment_container);

        if (childListFragment != null) {
            Log.d(TAG, "Child found");
            childListFragment.requestForDataUpdate();
        }

    }

    @Override
    public void removeTrip() {

        // TODO: removeTrip

    }

    @Override
    public void displayErrorMessage(String errorMessage) {

        // TODO: displayErrorMessage

    }

    public TripsPresenter getPresenter() {
        return presenter;
    }

    @OnClick(R.id.try_again_btn)
    public void onTryAgainClicked() {
        Log.d(TAG, "onTryAgainClicked()");
        presenter.onRefresh();
    }
}
