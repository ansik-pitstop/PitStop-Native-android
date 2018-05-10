package com.pitstop.ui.trip.list;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.adapters.TripListAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.trip.Trip;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.trip.TripActivityObservable;
import com.pitstop.ui.trip.TripsFragment;
import com.pitstop.ui.trip.TripsView;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.SecretUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnItemSelected;

/**
 * Created by David C. on 14/3/18.
 */

public class TripListFragment extends Fragment implements TripListView {

    private final String TAG = getClass().getSimpleName();

    private static final String INTERPOLATE = "true";

    @BindView(R.id.swiperefresh_trip_list)
    protected SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.spinner_sort_by)
    protected Spinner sortSpinner;

    @BindView(R.id.trips_recyclerview)
    protected RecyclerView tripsRecyclerView;

    @BindView(R.id.bottom_list_button)
    protected Button bottomListButton;

    @BindView(R.id.no_trips_text)
    protected TextView noTripsText;

    @BindView(R.id.trip_holder)
    protected View tripHolder;

    private Context context;
    private boolean hasBeenPopulated = false;
    private TripListPresenter presenter;

    private List<Trip> mTripList = new ArrayList<>();
    private TripListAdapter tripListAdapter;
    private TripActivityObservable tripActivityObservable;
    private AlertDialog unknownErrorDialog;

    private String apiKey;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = inflater.inflate(R.layout.fragment_trip_list, null);
        ButterKnife.bind(this, view);

        context = getContext();

        apiKey = SecretUtils.getMapsApiKey(context);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.trip_sort, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

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

            if (!presenter.isRefreshing()) {
                Log.d(TAG, "swipeRefreshLayout.onRefresh()");
                presenter.onRefresh(sortSpinner.getSelectedItemPosition());
            } else {
                swipeRefreshLayout.setRefreshing(false);
            }

        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated()");
        presenter.subscribe(this);

        presenter.onUpdateNeeded(sortSpinner.getSelectedItemPosition());

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
    public int getSortType(){
        Log.d(TAG,"getSortType()");
        return sortSpinner.getSelectedItemPosition();
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
    public void displayTripList(List<Trip> listTrip) {

        Log.d(TAG, "displayTripList() notifList: " + listTrip);

        tripsRecyclerView.setVisibility(View.VISIBLE);
        noTripsText.setVisibility(View.GONE);
        if (listTrip != null && listTrip.size() > 0) {

            mTripList.clear();
            mTripList.addAll(listTrip);
            tripListAdapter.notifyDataSetChanged();

        }
        hasBeenPopulated = true;
    }

    @Override
    public void displayNoTrips(){
        Log.d(TAG,"displayNoTrips()");
        noTripsText.setVisibility(View.VISIBLE);
        tripsRecyclerView.setVisibility(View.GONE);
        noTripsText.setText(R.string.no_trips_message);
    }

    @Override
    public void displayNoCar(){
        Log.d(TAG,"displayNoCar()");
        noTripsText.setVisibility(View.VISIBLE);
        tripsRecyclerView.setVisibility(View.GONE);
        noTripsText.setText(R.string.add_car_trips_message);
        bottomListButton.setText(R.string.title_activity_add_car);
    }

    @Override
    public void beginAddCar() {
        if (getActivity() != null && getActivity() instanceof MainActivity){
            ((MainActivity)getActivity()).openAddCarActivity();
        }
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
    public void showToastStillRefreshing() {

        Toast.makeText(context, R.string.wait_loading_finish, Toast.LENGTH_LONG).show();

    }

    @Override
    public boolean hasBeenPopulated() {
        Log.d(TAG, "hasBeenPopulated() ? " + hasBeenPopulated);
        return hasBeenPopulated;
    }

    @Override
    public void toggleRecordingButton(boolean recording) {
        Log.d(TAG,"toggleRecordingButton() recording: "+recording);
        if (recording){
            bottomListButton.setBackgroundColor(getContext().getResources().getColor(R.color.red));
            bottomListButton.setText(R.string.stop_recording);
        }else{
            bottomListButton.setBackgroundColor(getContext().getResources().getColor(R.color.facebook_blue));
            bottomListButton.setText(R.string.begin_recording);
        }

    }

    @Override
    public void showLoading() {
        Log.d(TAG, "showLoading()");
        if (!swipeRefreshLayout.isRefreshing()) {
            presenter.sentOnShowLoading();
            //loadingView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void hideLoading() {
        Log.d(TAG, "hideLoading()");
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        } else {
            presenter.sendOnHideLoading();
            //loadingView.setVisibility(View.GONE);
            swipeRefreshLayout.setEnabled(true);
        }
    }

    @Override
    public void hideRefreshing() {

        Log.d(TAG, "hideRefreshing()");
        swipeRefreshLayout.setRefreshing(false);

    }

    @Override
    public boolean isRefreshing() {

        Log.d(TAG, "isRefreshing()");
        return swipeRefreshLayout.isRefreshing();

    }

    public void onTripActivityObservableReady(TripActivityObservable tripActivityObservable){
        Log.d(TAG,"onTripActivityObservableReady()");
        this.tripActivityObservable = tripActivityObservable;
        if (presenter != null) presenter.onTripActivityObservableReady(tripActivityObservable);
    }

    @Override
    public TripActivityObservable getTripActivityObservable(){
        Log.d(TAG,"getTripActivityObservable()");
        return tripActivityObservable;
    }

    public void requestForDataUpdate(boolean restartAdapterSelectedId) {
        Log.d(TAG, "isRefreshing()");

        if (restartAdapterSelectedId) {
            tripListAdapter.restartSelectedId();
        }

        presenter.onUpdateNeeded(sortSpinner.getSelectedItemPosition());
    }

    @OnItemSelected(R.id.spinner_sort_by)
    void onSortItemSelected(int position) {
        Log.d(TAG, "onSortItemSelected()");

        // It's mandatory to create a new array, by passing mTripList the array content will be removed
        // when this.displayTripList() will be called
        List<Trip> listToSort = new ArrayList<>();

        for (Trip trip : mTripList) {
            listToSort.add(trip);
        }

        presenter.sortTripListBy(listToSort, position);

    }

    @Override
    public void checkPermissions() {
        if (getActivity() != null && getActivity() instanceof IBluetoothServiceActivity){
            ((IBluetoothServiceActivity)getActivity()).checkPermissions();
        }
    }

    @OnClick(R.id.bottom_list_button)
    public void onBottomListButtonClicked(){
        Log.d(TAG,"onTripRecordClicked");
        presenter.onBottomListButtonClicked();
    }
}
