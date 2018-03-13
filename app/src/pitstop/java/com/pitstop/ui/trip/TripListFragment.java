package com.pitstop.ui.trip;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.adapters.TripListAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.trip.Location2;
import com.pitstop.models.trip.Trip;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by David C. on 10/3/18.
 */

public class TripListFragment extends Fragment implements TripListView {

    private final String TAG = getClass().getSimpleName();

    @BindView(R.id.relative_main_container)
    protected View mainLayout;

    @BindView(R.id.trips_recyclerview)
    protected RecyclerView tripsRecyclerView;

    private Context context;
    private TripListPresenter presenter;

    private TripListAdapter tripListAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_trip_list, null);
        ButterKnife.bind(this, view);

        context = getContext();

        if (presenter == null) {
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();

            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication) getActivity().getApplicationContext());

            presenter = new TripListPresenter(useCaseComponent, mixpanelHelper);
        }
//        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        presenter.subscribe(this);
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
        if (listTrip != null && listTrip.size() > 0) {

            tripListAdapter = new TripListAdapter(context, listTrip, this);
            tripsRecyclerView.setAdapter(tripListAdapter);
            tripsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        }

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
