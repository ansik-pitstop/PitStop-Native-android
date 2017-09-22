package com.pitstop.ui.my_garage;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.adapters.CarsAdapter;
import com.pitstop.adapters.DealershipListAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.smooch.ui.ConversationActivity;

import static com.pitstop.ui.main_activity.MainActivity.RC_ADD_CAR;

/**
 * Created by ishan on 2017-09-19.
 */

public class MyGarageFragment extends Fragment implements MyGarageView {

    private static final String TAG = MyGarageFragment.class.getSimpleName();
    @BindView(R.id.appointments_view)
    View appointmentsView;

    @BindView(R.id.contact_view)
    View contactView;


    @BindView(R.id.car_recycler_view)
    RecyclerView carRecyclerView;

    @BindView(R.id.add_car_garage)
    View addCar;

    private MyGaragePresenter presenter;
    private AlertDialog dealershipCallDialog;
    private AlertDialog dealershipDirectionsDialog;
    private CarsAdapter carsAdapter;

    List <Car> carList = new ArrayList<>();

    public static MyGarageFragment newInstance(){
        return new MyGarageFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view = inflater.inflate(R.layout.my_garage_fragment, null);
        ButterKnife.bind(this, view);

        if (presenter == null){
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getActivity()))
                    .build();
            MixpanelHelper mixpanelHelper = new MixpanelHelper((GlobalApplication)getContext()
                    .getApplicationContext());
            presenter = new MyGaragePresenter(useCaseComponent, mixpanelHelper);

        }
        carRecyclerView.setLayoutManager( new LinearLayoutManager(getActivity()));
        carsAdapter = new CarsAdapter(this, carList);
        carRecyclerView.setAdapter(carsAdapter);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.loadCars();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        presenter.unsubscribe();
    }

    @Override
    public void openMyAppointments() {
        Log.d(TAG, "openMyAppointments()");
        ((MainActivity)getActivity()).openAppointments();
    }

    @Override
    public void openRequestService() {
        Log.d(TAG, "onRequestService()");
        ((MainActivity)getActivity()).requestMultiService(null);
    }

    @Override
    public void toast(String message) {
        Toast.makeText(getActivity(),message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean isUserNull() {
        if(((GlobalApplication)getActivity().getApplicationContext()).getCurrentUser() == null)
            return true;
        else
            return false;
    }

    @Override
    public String getUserPhone() {
        return  ((GlobalApplication)getActivity().getApplicationContext()).getCurrentUser().getPhone();
    }

    @Override
    public String getUserFirstName() {
        return ((GlobalApplication)getActivity().getApplicationContext()).getCurrentUser().getFirstName();
    }

    @Override
    public String getUserEmail() {
        return ((GlobalApplication)getActivity().getApplicationContext()).getCurrentUser().getEmail();
    }

    @Override
    public void openSmooch() {
        Log.d(TAG, "openSmooch()");
        ConversationActivity.show(getActivity());
    }

    @Override
    public void callDealership(Dealership dealership) {


        Log.d(TAG, "callDealership()");
        Intent intent = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + dealership.getPhone()));
        getActivity().startActivity(intent);

    }

    @Override
    public void showDealershipsCallDialog(List<Dealership> dealerships) {
        Log.d(TAG, "showDealershipsCallDialog()");
        if (dealershipCallDialog == null){
            DealershipListAdapter listAdapter = new DealershipListAdapter(getContext(), R.layout.list_item_dealership, dealerships);
            AnimatedDialogBuilder builder = new AnimatedDialogBuilder(getActivity());
            builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    callDealership(dealerships.get(i));
                }
            });
            dealershipCallDialog = builder
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Select a Dealership")
                    .setPositiveButton("", null)
                    .setNegativeButton("Dissmiss", (dialog, which) -> dialog.cancel())
                    .create();
        }
        dealershipCallDialog.show();
    }

    @Override
    public void showDealershipsDirectionDialog(List<Dealership> dealerships) {
        Log.d(TAG, "showDealershipsDirectionsDialog()");
        if (dealershipDirectionsDialog == null){
            DealershipListAdapter listAdapter = new DealershipListAdapter(getContext(), R.layout.list_item_dealership, dealerships);
            AnimatedDialogBuilder builder = new AnimatedDialogBuilder(getActivity());
            builder.setAdapter(listAdapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    openDealershipDirections(dealerships.get(i));
                }
            });
            dealershipDirectionsDialog= builder
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Select a Dealership")
                    .setPositiveButton("", null)
                    .setNegativeButton("Dissmiss", (dialog, which) -> dialog.cancel())
                    .create();
        }
        dealershipDirectionsDialog.show();
    }


    @Override
    public void openDealershipDirections(Dealership dealership) {
        Log.d(TAG, "openDealershipDirections()");


        Log.d(TAG, dealership.getAddress());
        String uri = String.format(Locale.ENGLISH,
                    "http://maps.google.com/maps?daddr=%s",
                    dealership.getAddress());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        getActivity().startActivity(intent);
    }

    @Override
    public void showCars(List<Car> list) {
        Log.d(TAG, "showCars()");
        carList.clear();
        carList.addAll(list);
        carsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCarClicked(Car car) {
        Log.d(TAG, "onCarClicked()");
    }

    @OnClick(R.id.my_appointments_garage)
    public void onMyAppointmentsClicked(){
        Log.d(TAG, "onMyAppointmentsClicked()");
        presenter.onMyAppointmentsClicked();

    }
    @OnClick(R.id.request_service_garage)
    public void onRequestServiceClicked(){
        Log.d(TAG, "onRequestServiceClicked");
        presenter.onRequestServiceClicked();
    }

    @OnClick (R.id.message_my_garage)
    public void onMessageClicked(){
        Log.d(TAG, "onMessageClicked()");
        presenter.onMessageClicked();
    }

    @OnClick (R.id.call_garage)
    public void onCallClicked(){
        Log.d(TAG, "onCallClicked()");
        presenter.onCallClicked();
    }

    @OnClick (R.id.find_direction_garage)
    public void onFindDirectionsClicked(){
        Log.d(TAG, "onFindDirectionsClicked()");
        presenter.onFindDirectionsClicked();
    }

    @OnClick(R.id.add_car_garage)
    public void onAddCarClicked(){
        Log.d(TAG, "onAddCarClicked()");
        Intent intent = new Intent(this.getActivity(),AddCarActivity.class);
        startActivityForResult(intent,RC_ADD_CAR);
    }

}
