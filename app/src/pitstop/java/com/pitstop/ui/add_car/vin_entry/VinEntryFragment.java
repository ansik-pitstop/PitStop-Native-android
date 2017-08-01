package com.pitstop.ui.add_car.vin_entry;

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

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class VinEntryFragment extends Fragment implements VinEntryView{
    private ViewGroup rootView;
    private VinEntryPresenter presenter;
    private MixpanelHelper mixpanelHelper;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getContext()))
                .build();

        mixpanelHelper = new MixpanelHelper(
                (GlobalApplication)getActivity().getApplicationContext());

        presenter = new VinEntryPresenter(useCaseComponent,mixpanelHelper);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_vin_entry, container, false);
        ButterKnife.bind(this, rootView);
        presenter.subscribe(this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        presenter.unsubscribe();
        super.onDestroyView();
    }
}
