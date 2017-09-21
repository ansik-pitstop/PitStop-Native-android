package com.pitstop.ui.my_garage;

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
import com.pitstop.adapters.DealershipDialogAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.dashboard.DashboardPresenter;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.service_request.RequestServiceActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.smooch.ui.ConversationActivity;

import static com.pitstop.ui.main_activity.MainActivity.RC_REQUEST_SERVICE;

/**
 * Created by ishan on 2017-09-19.
 */

public class MyGarageFragment extends Fragment implements MyGarageView {

    private static final String TAG = MyGarageFragment.class.getSimpleName();

    @BindView(R.id.appointments_view)
    View appointmentsView;

    @BindView(R.id.contact_view)
    View contactView;

    private MyGaragePresenter presenter;
    private AlertDialog dealershipCallDialog;
    private AlertDialog dealershipDirectionsDialog;

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
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
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
        ConversationActivity.show(getActivity());
    }

    @Override
    public void callDealership(Dealership dealership) {
        Log.d(TAG, "callDealership()");
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                dealership.getPhone()));
        getActivity().startActivity(intent);
    }

    @Override
    public void showDealershipsCallDialog(List<Dealership> dealerships, int origin) {
        Log.d(TAG, "showDealershipsCallDialog()");
        if (dealershipCallDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dealerships_dialog, null);
            RecyclerView recyclerView = (RecyclerView)dialogLayout
                    .findViewById(R.id.dealership_recycler_view);
            DealershipDialogAdapter dealershipDialogAdapter = new DealershipDialogAdapter(dealerships, this, origin);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.setAdapter(dealershipDialogAdapter);
            recyclerView.setVisibility(View.VISIBLE);
            dealershipCallDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Select a Dealership")
                    .setView(dialogLayout)
                    .setPositiveButton("", null)
                    .setNegativeButton("Dissmiss", (dialog, which) -> dialog.cancel())
                    .create();
        }
        dealershipCallDialog.show();
    }

    @Override
    public void showDealershipsDirectionDialog(List<Dealership> dealerships, int origin) {
        Log.d(TAG, "showDealershipsDirectionsDialog()");
        if (dealershipDirectionsDialog == null){
            final View dialogLayout = LayoutInflater.from(
                    getActivity()).inflate(R.layout.dealerships_dialog, null);
            RecyclerView recyclerView = (RecyclerView)dialogLayout
                    .findViewById(R.id.dealership_recycler_view);
            DealershipDialogAdapter dealershipDialogAdapter = new DealershipDialogAdapter(dealerships, this, origin);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.setAdapter(dealershipDialogAdapter);
            recyclerView.setVisibility(View.VISIBLE);
            dealershipDirectionsDialog = new AnimatedDialogBuilder(getActivity())
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setTitle("Select a Dealership")
                    .setView(dialogLayout)
                    .setPositiveButton("", null)
                    .setNegativeButton("Dissmiss", (dialog, which) -> dialog.cancel())
                    .create();
        }
        dealershipDirectionsDialog.show();
    }
    @Override
    public void onDealershipSelected(Dealership dealership, int origin) {
        Log.d(TAG, "onDealershipSelected()");
        if (origin == MyGaragePresenter.FROM_CALL_CLICKED)
            callDealership(dealership);
        else
            openDealershipDirections(dealership);
    }

    @Override
    public void openDealershipDirections(Dealership dealership) {
        String uri = String.format(Locale.ENGLISH,
                "http://maps.google.com/maps?daddr=%s",
                dealership.getAddress());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        getActivity().startActivity(intent);
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

}
