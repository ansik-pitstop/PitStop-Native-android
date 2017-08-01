package com.pitstop.ui.add_car.device_search;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
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

        mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());

        presenter = new DeviceSearchPresenter(useCaseComponent,mixpanelHelper);
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
}
