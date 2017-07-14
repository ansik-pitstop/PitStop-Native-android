package com.pitstop.ui.service_request.view_fragment.time_picker_view;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.ui.service_request.RequestServiceCallback;

/**
 * Created by Matthew on 2017-07-11.
 */

public class TimePickerFragment extends Fragment implements TimePickerView {

    RequestServiceCallback callback;

    TimePickerPresenter presenter;

    public void setActivityCallback(RequestServiceCallback callback){
        this.callback = callback;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_time_picker,container,false);


        presenter = new TimePickerPresenter(callback);
        presenter.subscribe(this);

        return view;
    }
}
