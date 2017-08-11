package com.pitstop.ui.add_car.ask_has_device;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.ui.add_car.FragmentSwitcher;
import com.pitstop.utils.MixpanelHelper;

import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class AskHasDeviceFragment extends Fragment implements AskHasDeviceView{

    private final String TAG = getClass().getSimpleName();

    private ViewGroup rootView;
    private AskHasDevicePresenter presenter;
    private MixpanelHelper mixpanelHelper;
    private FragmentSwitcher fragmentSwitcher;
    private UseCaseComponent useCaseComponent;

    public static AskHasDeviceFragment getInstance(){
        return new AskHasDeviceFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");

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
                R.layout.fragment_ask_has_device, container, false);
        ButterKnife.bind(this, rootView);

        if (presenter == null){
            presenter = new AskHasDevicePresenter(useCaseComponent,mixpanelHelper);
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

    @OnClick(R.id.bt_yes_device)
    protected void yesButtonClicked(){
        Log.d(TAG,"yesButtonClicked()");

        if (presenter == null) return;
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_YES_HARDWARE
                , MixpanelHelper.ADD_CAR_VIEW);
        presenter.onHasDeviceSelected();
    }

    @OnClick(R.id.bt_no_device)
    protected void noButtonClicked(){
        Log.d(TAG,"noButtonClicked()");

        if (presenter == null) return;
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_NO_HARDWARE
                , MixpanelHelper.ADD_CAR_VIEW);
        presenter.onNoDeviceSelected();
    }

    @Override
    public void loadVinEntryView() {
        Log.d(TAG,"loadVinEntryView()");
        if (fragmentSwitcher == null) return;
        fragmentSwitcher.setViewVinEntry();
    }

    @Override
    public void loadDeviceSearchView() {
        Log.d(TAG,"loadDeviceSearchView()");
        if (fragmentSwitcher == null) return;
        fragmentSwitcher.setViewDeviceSearch();

    }
}
