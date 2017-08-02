package com.pitstop.ui.add_car.device_search;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.ui.add_car.FragmentSwitcher;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class DeviceSearchFragment extends Fragment implements DeviceSearchView{

    private ViewGroup rootView;
    private DeviceSearchPresenter presenter;
    private MixpanelHelper mixpanelHelper;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext()))
                .build();

        BluetoothConnectionObservable bluetoothConnectionObservable
                = ((MainActivity)getActivity()).getBluetoothConnectService();

        mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());

        FragmentSwitcher fragmentSwitcher = (FragmentSwitcher)getActivity();

        presenter = new DeviceSearchPresenter(useCaseComponent, mixpanelHelper
                , bluetoothConnectionObservable, fragmentSwitcher);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_device_search, container, false);
        ButterKnife.bind(this, rootView);
        presenter.subscribe(this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        presenter.unsubscribe();
        super.onDestroyView();
    }

    @OnClick(R.id.bt_search_car)
    protected void searchForVehicleClicked(){
        if (presenter == null) return;
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_YES_HARDWARE_ADD_VEHICLE
                , MixpanelHelper.ADD_CAR_VIEW);
        presenter.startSearch();
    }

    @Override
    public void onVinRetrievalFailed() {

        //Fragment switcher, go toVinEntryFragment

        AlertDialog invalidMileageDialog= new AnimatedDialogBuilder(getActivity())
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
}
