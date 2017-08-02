package com.pitstop.ui.add_car.ask_has_device;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

    private ViewGroup rootView;
    private AskHasDevicePresenter presenter;
    private MixpanelHelper mixpanelHelper;
    private FragmentSwitcher fragmentSwitcher;
    private ProgressDialog progressDialog;

    public static AskHasDeviceFragment getInstance(){
        return new AskHasDeviceFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext()))
                .build();

        mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());

        fragmentSwitcher = (FragmentSwitcher)getActivity();

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

    @Override
    public void showLoading(@NonNull String message) {
        if (progressDialog == null || getActivity() == null) return;

        progressDialog.setMessage(message);
        progressDialog.show();
    }

    @Override
    public void hideLoading(@Nullable String message) {
        if (progressDialog == null || getActivity() == null) return;

        progressDialog.dismiss();
        if (message != null){
            Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void loadVinEntryView() {
        if (fragmentSwitcher == null) return;
        fragmentSwitcher.setViewVinEntry();
    }

    @Override
    public void loadDeviceSearchView() {
        if (fragmentSwitcher == null) return;
        fragmentSwitcher.setViewDeviceSearch();

    }
}
