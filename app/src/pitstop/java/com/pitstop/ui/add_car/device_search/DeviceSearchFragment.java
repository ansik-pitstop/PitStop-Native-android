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
import android.view.KeyEvent;
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
            progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (presenter != null){
                        presenter.onProgressDialogKeyPressed(keyCode);
                    }
                    return false;
                }
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

    @Override
    public void onVinRetrievalFailed(String scannerName, String scannerId, int mileage) {

        Log.d(TAG,"onVinRetrievalFailed() scannerName: "+scannerName+", scannerId: "+scannerId);

        //Fragment switcher, go toVinEntryFragment
        if (fragmentSwitcher == null || getActivity() == null ) return;
        fragmentSwitcher.setViewVinEntry(scannerName, scannerId, mileage);

        AlertDialog dialog = new AnimatedDialogBuilder(getActivity())
                .setTitle("Could not retrieve VIN")
                .setMessage("VIN could not be retrieved from your device, please input it manually")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog1, which) -> dialog1.cancel())
                .setNegativeButton("", null).create();
        dialog.show();
    }

    @Override
    public void onCannotFindDevice() {
        Log.d(TAG,"onCannotFindDevice()");

        if (getActivity() == null) return;

        AlertDialog dialog= new AnimatedDialogBuilder(getActivity())
                .setTitle("Could not find OBD device")
                .setMessage("We were unable to find a Pitstop OBD device, please make sure that the " +
                        "device is plugged in and that the lights are flashing. Try again?")
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (presenter != null){
                            presenter.startSearch();
                        }
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
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
                .setTitle("Invalid Mileage")
                .setMessage("Please input a mileage between 0 and 3,000,000.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
                .setTitle("Add Car Error")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
                .setTitle("Car Already Added")
                .setMessage("This car has already been added and is in use." +
                        " If this is your vehicle please remove it and try again.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
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

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onBackPressed(){
        Log.d(TAG,"onBackPressed()");
        if (presenter == null) return;
        presenter.onBackPressed();
    }
}
