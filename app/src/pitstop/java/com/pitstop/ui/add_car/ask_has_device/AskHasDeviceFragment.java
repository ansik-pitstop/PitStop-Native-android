package com.pitstop.ui.add_car.ask_has_device;

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

public class AskHasDeviceFragment extends Fragment implements AskHasDeviceView{

    private ViewGroup rootView;
    private AskHasDevicePresenter presenter;
    private MixpanelHelper mixpanelHelper;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext()))
                .build();

        mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());

        presenter = new AskHasDevicePresenter(useCaseComponent,mixpanelHelper);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_ask_has_device, container, false);
        ButterKnife.bind(this, rootView);
        presenter.subscribe(this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        presenter.unsubscribe();
        super.onDestroyView();
    }

    @OnClick(R.id.bt_yes_device)
    protected void yesButtonClicked(){
        if (presenter == null) return;
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_YES_HARDWARE
                , MixpanelHelper.ADD_CAR_VIEW);
        presenter.onHasDeviceSelected();
    }

    @OnClick(R.id.bt_no_device)
    protected void noButtonClicked(){
        if (presenter == null) return;
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_NO_HARDWARE
                , MixpanelHelper.ADD_CAR_VIEW);
        presenter.onNoDeviceSelected();
    }
}
