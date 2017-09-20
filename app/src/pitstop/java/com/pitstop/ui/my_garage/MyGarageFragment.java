package com.pitstop.ui.my_garage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.ui.dashboard.DashboardPresenter;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.ui.service_request.RequestServiceActivity;
import com.pitstop.utils.MixpanelHelper;

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
    public void callDealership(Car car) {
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" +
                car.getDealership().getPhone()));
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


}
