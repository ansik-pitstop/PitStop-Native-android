package com.pitstop.ui.add_car.select_device;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.ui.add_car.FragmentSwitcher;
import com.pitstop.utils.MixpanelHelper;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class SelectDeviceFragment extends Fragment implements SelectDeviceView {

    private final String TAG = getClass().getSimpleName();
    private MixpanelHelper mixpanelHelper;
    private ViewGroup rootView;
    private SelectDevicePresenter presenter;
    private FragmentSwitcher fragmentSwitcher;

    public static SelectDeviceFragment getInstance(){
        return new SelectDeviceFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_select_device, container, false);
        ButterKnife.bind(this, rootView);

        if (mixpanelHelper == null) {
            mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication) getActivity().getApplicationContext());
        }

        if (presenter == null) {
            presenter = new SelectDevicePresenter();
        }

        if (fragmentSwitcher == null){
            fragmentSwitcher = (FragmentSwitcher)getActivity();
        }
        presenter.subscribe(this);

        return rootView;
    }

    @OnClick(R.id.imageButton2)
    protected void simCardDeviceClicked() {
        Log.d(TAG,"simCardDeviceClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.SIM_CARD_TAPPED
                , MixpanelHelper.SELECT_DEVICE_VIEW);
        presenter.addSIMCardDevice();
    }

    @OnClick(R.id.imageButton3)
    protected void bluetoothDeviceClicked() {
        Log.d(TAG,"bluetoothDeviceClicked()");
        mixpanelHelper.trackButtonTapped(MixpanelHelper.BLUETOOTH_DEVICE_TAPPED
                , MixpanelHelper.SELECT_DEVICE_VIEW);
        presenter.connectToBluetoothDevice();
    }

    @Override
    public void loadConnectToBluetoothView() {
        Log.d(TAG,"loadConnectToBluetoothView()");
        if (fragmentSwitcher == null) return;
        fragmentSwitcher.setViewDeviceSearch();
    }

    @Override
    public void loadInsertVinView() {
        Log.d(TAG,"loadInsertVinView()");
        if (fragmentSwitcher == null) return;
        fragmentSwitcher.setViewVinEntry();
    }
}
