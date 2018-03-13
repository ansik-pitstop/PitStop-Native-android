package com.pitstop.ui.trip;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.trip.Location2;
import com.pitstop.models.trip.Trip;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by David C. on 10/3/18.
 */

public class TripListFragment extends Fragment implements TripListView {

    private final String TAG = getClass().getSimpleName();

    private TripListPresenter presenter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view = inflater.inflate(R.layout.fragment_trip_list, null);
        ButterKnife.bind(this,view);

//        notificationAdapter = new NotificationAdapter(this, notificationList);
//        notificationRecyclerView.setAdapter(notificationAdapter);
//        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        if (presenter == null) {
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();

            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication)getActivity().getApplicationContext());

            presenter = new TripListPresenter(useCaseComponent, mixpanelHelper);
        }
//        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        presenter.loadView();
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void showLoading() {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    public void hideRefreshing() {

    }

    @Override
    public boolean isRefreshing() {
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
    public void displayTripList(List<Trip> listTrip) {

        // TODO: displayTripList

    }

    @Override
    public void displayTripPolylineOnMap(List<Location2> listLocation) {

        // TODO: displayTripPolylineOnMap

    }

    @Override
    public void openTripDetailsView(Trip trip) {

        // TODO: openTripDetailsView

    }

    @Override
    public void removeTrip() {

        // TODO: removeTrip

    }

    @Override
    public void updateTripList(List<Trip> listTrip) {

        // TODO: updateTripList

    }

    @Override
    public void displayErrorMessage(String errorMessage) {

        // TODO: displayErrorMessage

    }
}
