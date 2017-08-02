package com.pitstop.ui.add_car.device_search;

import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import com.pitstop.ui.add_car.FragmentSwitcher;
import com.pitstop.ui.main_activity.MainActivity;
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

    @BindView(R.id.mileage_input)
    EditText mileageInputEditText;

    private ViewGroup rootView;
    private DeviceSearchPresenter presenter;
    private MixpanelHelper mixpanelHelper;
    private FragmentSwitcher fragmentSwitcher;
    private ProgressDialog progressDialog;

    public static DeviceSearchFragment getInstance(){
        return new DeviceSearchFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.d(TAG,"onActivityCreated()");

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext()))
                .build();

        BluetoothConnectionObservable bluetoothConnectionObservable
                = ((MainActivity)getActivity()).getBluetoothConnectService();

        mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());

        fragmentSwitcher = (FragmentSwitcher)getActivity();

        presenter = new DeviceSearchPresenter(useCaseComponent, mixpanelHelper
                , bluetoothConnectionObservable);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");

        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_device_search, container, false);
        ButterKnife.bind(this, rootView);
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
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_YES_HARDWARE_ADD_VEHICLE
                , MixpanelHelper.ADD_CAR_VIEW);
        presenter.startSearch();
    }

    @Override
    public void onVinRetrievalFailed(String scannerName, String scannerId) {

        Log.d(TAG,"onVinRetrievalFailed() scannerName: "+scannerName+", scannerId: "+scannerId);

        //Fragment switcher, go toVinEntryFragment
        fragmentSwitcher.setViewVinEntry(scannerName, scannerId);

        AlertDialog invalidMileageDialog = new AnimatedDialogBuilder(getActivity())
                .setTitle("Could not retrieve VIN")
                .setMessage("VIN could not be retrieved from your device, please input it manually")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("", null).create();
        invalidMileageDialog.show();
    }

    @Override
    public void onCannotFindDevice() {
        Log.d(TAG,"onCannotFindDevice()");

        AlertDialog invalidMileageDialog= new AnimatedDialogBuilder(getActivity())
                .setTitle("Could not find OBD device")
                .setMessage("We were unable to find a Pitstop OBD device, please make sure that the " +
                        "device is plugged in and that the lights are flashing. Try again?")
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        presenter.startSearch();
                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
        invalidMileageDialog.show();
    }

    @Override
    public int getMileage(){
        Log.d(TAG,"getMileage(), returning: "
                +Integer.valueOf(mileageInputEditText.getText().toString()));
        return Integer.valueOf(mileageInputEditText.getText().toString());
    }

    @Override
    public void onMileageInvalid() {
        Log.d(TAG,"onMileageInvalid()");

        AlertDialog invalidMileageDialog= new AnimatedDialogBuilder(getActivity())
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
        invalidMileageDialog.show();
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

        AlertDialog invalidMileageDialog= new AnimatedDialogBuilder(getActivity())
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
        invalidMileageDialog.show();
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
        if (message != null){
            Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
        }
    }
}
