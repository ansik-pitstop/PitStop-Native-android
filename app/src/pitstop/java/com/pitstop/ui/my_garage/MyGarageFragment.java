package com.pitstop.ui.my_garage;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

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
import com.pitstop.ui.vehicle_specs.VehicleSpecsFragment;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

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
/*
    private static final String TAG = MyGarageFragment.class.getSimpleName();
    @BindView(R.id.appointments_view)
    protected View appointmentsView;

    @BindView(R.id.contact_view)
    protected View contactView;

    @BindView(R.id.main_linear_layout)
    protected LinearLayout mainLinearLayout;

    @BindView(R.id.contents_container)
    protected LinearLayout mainLayout;

    @BindView(R.id.car_recycler_view)
    protected RecyclerView carRecyclerView;

    @BindView(R.id.add_car_garage)
    protected View addCar;

    @BindView(R.id.progress)
    protected View loadingView;

    @BindView(R.id.swiper)
    protected SwipeRefreshLayout swipeRefreshLayout;

    private MyGaragePresenter presenter;
    private AlertDialog dealershipCallDialog;
    private AlertDialog dealershipDirectionsDialog;
    private CarsAdapter carsAdapter;
    private ProgressDialog progressDialog;
    private AlertDialog errorDialog;
    private boolean hasBeenPopulated = false;

    public static MyGarageFragment newInstance(){
        return new MyGarageFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");
        View view = inflater.inflate(R.layout.fragment_my_garage, null);
        ButterKnife.bind(this, view);
        if (presenter == null){
            UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getActivity()))
                    .build();
            MixpanelHelper mixpanelHelper = new MixpanelHelper((GlobalApplication)getContext()
                    .getApplicationContext());
            presenter = new MyGaragePresenter(useCaseComponent, mixpanelHelper);

        }
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        carRecyclerView.setLayoutManager( new LinearLayoutManager(getActivity()));
        carsAdapter = new CarsAdapter(this, presenter.getDealerships(), presenter.getCars());
        carRecyclerView.setAdapter(carsAdapter);
        swipeRefreshLayout.setOnRefreshListener(() -> presenter.onRefresh());
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onViewCreated()");
        super.onViewCreated(view, savedInstanceState);
        presenter.subscribe(this);
        presenter.onAppStateChanged();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        presenter.unsubscribe();
    }

    @Override
    public void showLoading() {
        if (!swipeRefreshLayout.isRefreshing()) {
            mainLinearLayout.setGravity(Gravity.CENTER);
            mainLinearLayout.setVerticalGravity(Gravity.CENTER_VERTICAL);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                                        LinearLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER;
            mainLinearLayout.setLayoutParams(params);
            Log.d(TAG, "showLoading()");
            mainLayout.setVisibility(View.GONE);
            loadingView.setVisibility(View.VISIBLE);
            loadingView.bringToFront();
            swipeRefreshLayout.setEnabled(false);
        }
    }

    @Override
    public void hideLoading(){
        Log.d(TAG, "hideLoading()");
        if (!swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setEnabled(true);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.NO_GRAVITY;
            mainLinearLayout.setLayoutParams(params);
            loadingView.setVisibility(View.GONE);
            mainLayout.setVisibility(View.VISIBLE);
        }
        else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void openMyAppointments() {
        Log.d(TAG, "openMyAppointments()");
        if(getActivity() == null) return;
        ((MainActivity)getActivity()).openAppointments();
    }

    @Override
    public void openRequestService() {
        Log.d(TAG, "onRequestService()");
        if(getActivity() == null) return;
        ((MainActivity)getActivity()).requestMultiService(null);
    }

    @Override
    public void toast(String message) {
        Log.d(TAG, "toast: " + message);
        if(getActivity() == null) return;
        Toast.makeText(getActivity(),message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean isUserNull() {
        if(getActivity()!= null) {
        if(((GlobalApplication)getActivity().getApplicationContext()).getCurrentUser() == null)
            return true;
        else
            return false;
        }
        return false;
    }

    @Override
    public String getUserPhone() {
        if(getActivity()!= null)
            return ((GlobalApplication)getActivity().getApplicationContext()).getCurrentUser().getPhone();
        return "";
    }

    @Override
    public String getUserFirstName() {
        if(getActivity()!= null)
            return ((GlobalApplication)getActivity().getApplicationContext()).getCurrentUser().getFirstName();
        return "";
    }

    @Override
    public String getUserEmail() {
        if(getActivity()!= null)
            return ((GlobalApplication)getActivity().getApplicationContext()).getCurrentUser().getEmail();
    return "";
    }

    @Override
    public void openSmooch() {
        Log.d(TAG, "openSmooch()");
        if (getActivity()!=null)
            ConversationActivity.show(getActivity());
    }

    @Override
    public void callDealership(Dealership dealership) {
        Log.d(TAG, "callDealership()");
        Intent intent = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + dealership.getPhone()));
        if(getActivity()!= null)
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
                    .setTitle(getContext().getString(R.string.select_dealership_toast_text))
                    .setNegativeButton("Dismiss", (dialog, which) -> dialog.cancel())
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
                    .setTitle(getContext().getString(R.string.select_dealership_toast_text))
                    .setNegativeButton("Dismiss", (dialog, which) -> dialog.cancel())

                    .create();

        }
        dealershipDirectionsDialog.show();

    }

    @Override
    public void openDealershipDirections(Dealership dealership) {
        Log.d(TAG, "openDealershipDirections()");
        String uri = String.format(Locale.ENGLISH,
                    "http://maps.google.com/maps?daddr=%s",
                    dealership.getAddress());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        if(getActivity()!= null)
            getActivity().startActivityForResult(intent, 0 );
    }

    @Override
    public void showCars(List<Car> list) {
        Log.d(TAG, "showCars() cars: "+list);
        carsAdapter.notifyDataSetChanged();
//        if (list.size() == 0){
//            appointmentsView.setVisibility(View.GONE);
//        }
        hasBeenPopulated = true;
    }

    @Override
    public void onCarClicked(Car car, int position) {
        Log.d(TAG, "onCarClicked()");

    }

    @Override
    public void onUpdateNeeded() {
        Log.d(TAG, "onUpdateNeeded");
        presenter.makeCarListNull();
        presenter.loadCars();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // requestcode set as 0 in "openSpecsActivity Method"
        if(requestCode == 0){
            if(resultCode == Activity.RESULT_OK){
                if(data.getExtras().getBoolean(VehicleSpecsFragment.CAR_DELETED)){
                    presenter.onCarRemoved(data.getExtras().getInt(VehicleSpecsFragment.CAR_POSITION));
                }
                else if (data.getExtras().getBoolean(VehicleSpecsFragment.CAR_SELECTED)){
                    presenter.onCarSetAsCurrent(data.getExtras().getInt(VehicleSpecsFragment.CAR_POSITION));
                }
            }
        }
    }

    @Override
    public void notifyCarDataChanged(){
        carsAdapter.notifyDataSetChanged();
    }
*//*

    @Override
    public void openSpecsActivity(Car car, int position) {
        Log.d(TAG, "openSpecsActivity()" + car.getModel());
        Intent intent = new Intent(getContext(), VehicleSpecsActivity.class);
        Bundle bundle  = new Bundle();
        bundle.putInt(VehicleSpecsFragment.CAR_POSITION_KEY, position);
        bundle.putBoolean(VehicleSpecsFragment.IS_CURRENT_KEY, car.isCurrentCar());
        bundle.putInt(VehicleSpecsFragment.CAR_ID_KEY, car.getId());
        bundle.putString(VehicleSpecsFragment.CAR_VIN_KEY, car.getVin());
        bundle.putString(VehicleSpecsFragment.SCANNER_ID_KEY, car.getScanner());
        bundle.putString(VehicleSpecsFragment.ENGINE_KEY, car.getEngine());
        bundle.putString(VehicleSpecsFragment.CITY_MILEAGE_KEY, car.getCityMileage());
        bundle.putString(VehicleSpecsFragment.HIGHWAY_MILEAGE_KEY, car.getHighwayMileage());
        bundle.putString(VehicleSpecsFragment.TRIM_KEY, car.getTrim());
        bundle.putString(VehicleSpecsFragment.TANK_SIZE_KEY, car.getTankSize());
        bundle.putInt(VehicleSpecsFragment.YEAR_KEY, car.getYear());
        bundle.putString(VehicleSpecsFragment.MAKE_KEY, car.getMake());
        bundle.putString(VehicleSpecsFragment.MODEL_KEY, car.getModel());
        if (car.getDealership() != null)
            bundle.putString(VehicleSpecsFragment.DEALERSHIP_KEY, car.getDealership().getName());
        intent.putExtras(bundle);
        // the zero is the requestcode sent
        startActivityForResult(intent, 0);
    }
*//*

    @Override
    public void noCarsView() {
        Log.d(TAG, "noCarsView()");
        appointmentsView.setVisibility(View.GONE);
    }

    @Override
    public void appointmentsVisible() {
        Log.d(TAG, "appointmentsVisible()");
        appointmentsView.setVisibility(View.VISIBLE);
    }

    @Override
    public void appointmentsInvisible() {
        Log.d(TAG, "appointmentsInvisible()");
        appointmentsView.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean hasBeenPopulated(){
        Log.d(TAG,"hasBeenPopulated() ? "+hasBeenPopulated);
        return hasBeenPopulated;
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
        if(getActivity()!= null) {
            Log.d(TAG, "onAddCarClicked()");
            Intent intent = new Intent(this.getActivity(), AddCarActivity.class);
            startActivityForResult(intent, MainActivity.RC_ADD_CAR);
        }
    }

    public void showLoadingDialog(String text) {
        if (progressDialog == null) {
            return;
        }
        progressDialog.setMessage(text);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    public void hideLoadingDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        } else {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setCanceledOnTouchOutside(false);
        }
    }

    @Override
    public void showErrorDialog(){
        Log.d(TAG,"displayUnknownErrorDialog()");
        if (errorDialog == null){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setTitle(R.string.unknown_error_title);
            alertDialogBuilder
                    .setMessage(R.string.unknown_error)
                    .setCancelable(true)
                    .setPositiveButton(R.string.ok, (dialog, id) -> {
                        dialog.dismiss();
                    });
            errorDialog = alertDialogBuilder.create();
        }
        errorDialog.show();
    }*/
}
