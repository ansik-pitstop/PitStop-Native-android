package com.pitstop.ui.add_car.device_search;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.add_car.FragmentSwitcher;
import com.pitstop.ui.add_car.PendingAddCarActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class DeviceSearchFragment extends Fragment implements DeviceSearchView{

    private final String TAG = getClass().getSimpleName();
    public static final int RC_PENDING_ADD_CAR = 1043;

    @BindView(R.id.input_mileage)
    EditText mileageInputEditText;

    private ViewGroup rootView;
    private DeviceSearchPresenter presenter;
    private MixpanelHelper mixpanelHelper;
    private FragmentSwitcher fragmentSwitcher;
    private ProgressDialog progressDialog;
    private UseCaseComponent useCaseComponent;
    private AlertDialog connectErrorDialog;

    public static DeviceSearchFragment getInstance(){
        return new DeviceSearchFragment();
    }

    public void setBluetoothConnectionObservable(BluetoothConnectionObservable bluetoothConnectionObservable){
        if (presenter == null) return;
        presenter.setBluetoothConnectionObservable(bluetoothConnectionObservable);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");

        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            /*Has to be handled because when the ProgressDialog
            **is open onBackPressed() is not invoked*/
            progressDialog.setOnKeyListener((dialog, keyCode, event) -> {
                if (presenter != null){
                    presenter.onProgressDialogKeyPressed(keyCode);
                }
                return false;
            });
        }

        if (useCaseComponent == null){
            useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();
        }

        if (mixpanelHelper == null){
            mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication)getActivity().getApplicationContext());
        }

        if (fragmentSwitcher == null){
            fragmentSwitcher = (FragmentSwitcher)getActivity();
        }

        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_device_search, container, false);
        ButterKnife.bind(this, rootView);

        if (presenter == null){
            presenter = new DeviceSearchPresenter(useCaseComponent, mixpanelHelper);
        }

        BluetoothConnectionObservable bluetoothConnectionObservable
                = ((AddCarActivity)getActivity()).getBluetoothConnectionObservable();

        if (bluetoothConnectionObservable != null){
            presenter.setBluetoothConnectionObservable(bluetoothConnectionObservable);
        }

        presenter.subscribe(this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");

        presenter.unsubscribe();
        super.onDestroyView();
    }

    @OnClick(R.id.bt_search_car)
    protected void searchForVehicleClicked(){
        Log.d(TAG,"searchForVehicleClicked()");

        if (presenter == null) return;
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_SEARCH_TAPPED
                , MixpanelHelper.ADD_CAR_VIEW);
        presenter.startSearch();
    }

    @OnClick(R.id.cant_turn_on_car_button)
    public void onCantTurnOnCarClicked(){
        Log.d(TAG, "onCantTurnOnCarClicked");
        ((AddCarActivity)getActivity()).setViewVinAndDeviceEntry();
    }

    @Override
    public void onVinRetrievalFailed(String scannerName, String scannerId, int mileage) {

        Log.d(TAG,"onVinRetrievalFailed() scannerName: "+scannerName+", scannerId: "+scannerId);

        //Fragment switcher, go toVinEntryFragment
        AlertDialog dialog = new AnimatedDialogBuilder(getActivity())
                .setTitle(getString(R.string.vin_retrieval_failed_alert_title))
                .setCancelable(true)
                .setMessage(getString(R.string.vin_retrieval_failed_alert_message))
                .setPositiveButton("Enter Manually", (dialog1, which) -> {
                    if (fragmentSwitcher != null && getActivity() != null)
                        fragmentSwitcher.setViewVinEntry(scannerName, scannerId, mileage);
                })
                .setNegativeButton(getString(R.string.try_again), (dialogInterface, i) -> {
                    presenter.startSearch();
                }).create();
        dialog.show();
    }

    @Override
    public void onCannotFindDevice() {
        Log.d(TAG,"onCannotFindDevice()");

        if (getActivity() == null) return;

        AlertDialog dialog= new AnimatedDialogBuilder(getActivity())
                .setTitle(getString(R.string.cannot_find_device_alert_title))
                .setMessage(getString(R.string.cannot_find_device_alert_message))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes_button_text), (dialog1, which) -> {
                    if (presenter != null){
                        presenter.startSearch();
                    }
                })
                .setNegativeButton(getString(R.string.no_button_text), (dialog12, which) -> dialog12.cancel())
                .create();
        dialog.show();
    }

    @Override
    public String getMileage(){
        Log.d(TAG,"getMileage(), returning: "
                +mileageInputEditText.getText().toString());
        if (mileageInputEditText == null || mileageInputEditText.getText() == null){
            return "";
        }
        else{
            return mileageInputEditText.getText().toString();
        }
    }

    @Override
    public void onMileageInvalid() {
        Log.d(TAG,"onMileageInvalid()");
        if (getActivity() == null) return;
        AlertDialog dialog= new AnimatedDialogBuilder(getActivity())
                .setTitle(getString(R.string.invalid_mileage_alert_title))
                .setMessage(getString(R.string.invalid_mileage_alert_message))
                .setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("",null)
                .create();
        dialog.show();
    }

    @Override
    public void onCarAddedWithShop(Car car) {
        Log.d(TAG,"onCarAddedWithShop() car: "+car);

        if (fragmentSwitcher == null) return;

        fragmentSwitcher.endAddCarSuccess(car,true);
    }

    @Override
    public void onCarAddedWithoutShop(Car car) {
        Log.d(TAG,"onCarAddedWithoutShop() car: "+car);

        if (fragmentSwitcher == null) return;

        fragmentSwitcher.endAddCarSuccess(car,false);
    }

    @Override
    public void onErrorAddingCar(String message) {
        Log.d(TAG,"onErrorAddingCar(), message: "+message);

        if (getActivity() == null) return;

        AlertDialog dialog= new AnimatedDialogBuilder(getActivity())
                .setTitle(getString(R.string.add_car_error_alert_title))
                .setMessage(message)
                .setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("",null)
                .create();
        dialog.show();
    }

    @Override
    public void onCarAlreadyAdded(Car car) {
        Log.d(TAG,"onCarAlreadyAdded() car: "+car);

        if (getActivity() == null) return;

        AlertDialog dialog= new AnimatedDialogBuilder(getActivity())
                .setTitle(getString(R.string.car_already_added_alert_title))
                .setMessage(getString(R.string.car_already_added_alert_message))
                .setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("",null)
                .create();

        dialog.show();
    }

    @Override
    public void showLoading(@NonNull String message) {
        Log.d(TAG,"showLoading(): "+message);
        if (progressDialog == null || getActivity() == null) return;

        progressDialog.setMessage(message);
        progressDialog.show();
    }

    @Override
    public void showLoading(@NonNull int message) {
        Log.d(TAG,"showLoading(): "+getText(message));
        if (progressDialog == null || getActivity() == null) return;

        progressDialog.setMessage(getText(message));
        progressDialog.show();
    }

    @Override
    public void hideLoading(@Nullable String message) {
        Log.d(TAG,"hideLoading(): "+message);
        if (progressDialog == null || getActivity() == null) return;

        progressDialog.dismiss();
        if (message != null && !message.isEmpty()){
            Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void setLoadingCancelable(boolean cancelable) {
        Log.d(TAG,"setLoadingCancelable() cancelable: "+cancelable);
        if (progressDialog == null) return;

        progressDialog.setCancelable(cancelable);
    }

    @Override
    public void showAskHasDeviceView() {
        Log.d(TAG, "showAskHasDeviceView()");
        if (fragmentSwitcher == null) return;

        fragmentSwitcher.setViewAskHasDevice();
    }

    @Override
    public void beginPendingAddCarActivity(String vin, double mileage, String scannerId) {
        if (getActivity() == null) return;

        Intent intent = new Intent(getActivity(), PendingAddCarActivity.class);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_MILEAGE, mileage);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_SCANNER, scannerId);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_VIN, vin);
        startActivityForResult(intent, RC_PENDING_ADD_CAR);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,"onActivityResult()");
        if (presenter == null) return;

        if (requestCode == RC_PENDING_ADD_CAR){
            String vin = data.getStringExtra(PendingAddCarActivity.ADD_CAR_VIN);
            int mileage = Integer.valueOf(data.getStringExtra(PendingAddCarActivity
                    .ADD_CAR_MILEAGE));
            String scannerId = data.getStringExtra(PendingAddCarActivity.ADD_CAR_SCANNER);

            presenter.onGotPendingActivityResults(vin,mileage,scannerId,scannerId);
        }
        else{
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onBackPressed(){
        Log.d(TAG,"onBackPressed()");
        if (presenter == null) return;
        presenter.onBackPressed();
    }

    @Override
    public void onCouldNotConnectToDevice() {
        Log.d(TAG,"onCouldNotConnectToDevice()");
        if (connectErrorDialog == null) {
            connectErrorDialog = new AnimatedDialogBuilder(getActivity())
                    .setTitle(getString(R.string.connection_error))
                    .setCancelable(true)
                    .setMessage(getString(R.string.connection_try_again))
                    .setPositiveButton(getString(R.string.yes_button_text), (dialog1, which) -> {
                            presenter.startSearch();
                    })
                    .setNegativeButton(getString(R.string.no_button_text), (dialogInterface, i) -> {
                        dialogInterface.cancel();
                    }).create();
        }
        connectErrorDialog.show();

    }

    @Override
    public void connectingToDevice() {
        showLoading(getString(R.string.connecting_to_device));
    }


}
