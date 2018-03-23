package com.pitstop.ui.trip.list;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.pitstop.models.trip.Trip;
import com.pitstop.ui.trip.TripsFragment;
import com.pitstop.ui.trip.TripsView;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.SecretUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by David C. on 14/3/18.
 */

public class TripListFragment extends Fragment implements TripListView {

    private final String TAG = getClass().getSimpleName();

    private static final String INTERPOLATE = "true";

    @BindView(R.id.swiperefresh_trip_list)
    protected SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.trips_recyclerview)
    protected RecyclerView tripsRecyclerView;

    private Context context;
    private boolean hasBeenPopulated = false;
    private TripListPresenter presenter;

    private List<Trip> mTripList = new ArrayList<>();
    private TripListAdapter tripListAdapter;

    private String apiKey;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_trip_list, null);
        ButterKnife.bind(this, view);

        context = getContext();

        apiKey = SecretUtils.getMapsApiKey(context);

        tripListAdapter = new TripListAdapter(context, mTripList, this);
        tripsRecyclerView.setAdapter(tripListAdapter);
        tripsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (presenter == null) {
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();

            MixpanelHelper mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication) getActivity().getApplicationContext());

            presenter = new TripListPresenter(useCaseComponent, mixpanelHelper);

            if (getParentFragment() instanceof TripsView) {
                presenter.setCommunicationInteractor(((TripsFragment) getParentFragment()).getPresenter());
            }
        }
        swipeRefreshLayout.setOnRefreshListener(() -> {

            presenter.onRefresh();

        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        presenter.subscribe(this);

        presenter.onUpdateNeeded();

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        presenter.unsubscribe();
        hasBeenPopulated = false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
    public void displayTripList(List<Trip> listTrip) {

        Log.d(TAG, "displayTripList() notifList: " + listTrip);

        if (listTrip != null && listTrip.size() > 0) {

            mTripList.clear();
            mTripList.addAll(listTrip);
            tripListAdapter.notifyDataSetChanged();

        }

        hasBeenPopulated = true;

    }

    @Override
    public void onTripRowClicked(Trip trip) {

        presenter.onTripRowClicked(trip, INTERPOLATE, apiKey);

    }

    @Override
    public void onTripInfoClicked(Trip trip) {

        presenter.onTripInfoClicked(trip);

    }

    @Override
    public boolean hasBeenPopulated() {
        Log.d(TAG, "hasBeenPopulated() ? " + hasBeenPopulated);
        return hasBeenPopulated;
    }

    @Override
    public void showLoading(){
        Log.d(TAG,"showLoading()");
        if (!swipeRefreshLayout.isRefreshing()){
            presenter.sentOnShowLoading();
            //loadingView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideLoading(){
        Log.d(TAG,"hideLoading()");
        if (swipeRefreshLayout.isRefreshing()){
            swipeRefreshLayout.setRefreshing(false);
        }else{
            presenter.sendOnHideLoading();
            //loadingView.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(true);
        }
    }

    @Override
    public void hideRefreshing() {

        Log.d(TAG,"hideRefreshing()");
        swipeRefreshLayout.setRefreshing(false);

    }

    @Override
    public boolean isRefreshing() {

        Log.d(TAG,"isRefreshing()");
        return swipeRefreshLayout.isRefreshing();

    }

    public void requestForDataUpdate() {
        Log.d(TAG,"isRefreshing()");

        presenter.onUpdateNeeded();
    }
}
