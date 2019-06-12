package com.pitstop.ui.trip;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
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

    @BindView(R.id.loading_spinner)
    protected View loadingSpinner;

    @BindView(R.id.linear_container)
    protected View mainLayout;

    @BindView(R.id.mapview_trips)
    protected MapView mapView;

    private TripsPresenter presenter;

    private TripListFragment tripListFragment = new TripListFragment();
    private TripDetailFragment tripDetailFragment;

    private AlertDialog unknownErrorDialog;

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
        loadingSpinner.bringToFront();

    }

    @Override
    public void hideLoading() {

        Log.d(TAG, "hideLoading()");
        loadingSpinner.setVisibility(View.GONE);

    }

    @Override
    public void hideRefreshing() {

    }

    @Override
    public boolean isRefreshing() { // TODO: implement
        return false;
    }

    @Override
    public void displayOfflineErrorDialog() {
        // Nothing to do here
    }

    @Override
    public void displayUnknownErrorDialog() {

        Log.d(TAG, "displayUnknownErrorDialog()");
        if (unknownErrorDialog == null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.unknown_error_title);
            alertDialogBuilder
                    .setMessage(R.string.unknown_error)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        dialog.dismiss();
                    });
            unknownErrorDialog = alertDialogBuilder.create();
        }
        unknownErrorDialog.show();

    }

    @Override
    public void displayUnknownErrorView() {
        // Nothing to do here
    }

    @Override
    public void displayOfflineView() {
        // Nothing to do here
    }

    @Override
    public void displayOnlineView() {
        // Nothing to do here
    }

    @Override
    public void displayTripPolylineOnMap(PolylineOptions polylineOptions) {

        if (mapView == null) return;
        mapView.addPolyline(polylineOptions);
    }

    @Override
    public void displayStartMarker(LatLng coordinates){
        Log.d(TAG,"displayStartMarker() coordinates: "+coordinates);
        if (mapView == null) return;
        mapView.addMarker(coordinates.latitude, coordinates.longitude, "Start");
    }

    @Override
    public void displayEndMarker(LatLng coordinates){
        Log.d(TAG,"displayEndMarker() coordinates: "+coordinates);
        if (mapView == null) return;
        mapView.addMarker(coordinates.latitude, coordinates.longitude, "End");

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
        if (tripListFragment != null) {
            Log.d(TAG, "Child found");
            tripListFragment.requestForDataUpdate(true);
        }

    }

    @Override
    public void clearMap() {

        if (mapView == null) return;

        mapView.clearPolyline();

    }

    @Override
    public void showToast(int message) {

        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void displayErrorMessage(String errorMessage) {

        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();

    }

    public TripsPresenter getPresenter() {
        return presenter;
    }

    @OnClick(R.id.unknown_error_try_again)
    public void onTryAgainClicked() {
        Log.d(TAG, "onTryAgainClicked()");
        presenter.onRefresh();
    }
}
