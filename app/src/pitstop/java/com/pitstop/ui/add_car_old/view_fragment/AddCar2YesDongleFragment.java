package com.pitstop.ui.add_car_old.view_fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;

/**
 * Created by David on 7/20/2016.
 */
public class AddCar2YesDongleFragment extends Fragment {
    private ViewGroup rootView;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_device_search, container, false);

        return rootView;
    }
}
