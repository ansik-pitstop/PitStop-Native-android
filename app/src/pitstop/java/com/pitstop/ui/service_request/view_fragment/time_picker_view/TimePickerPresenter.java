package com.pitstop.ui.service_request.view_fragment.time_picker_view;

import com.pitstop.ui.service_request.RequestServiceCallback;

/**
 * Created by Matthew on 2017-07-11.
 */

public class TimePickerPresenter {

    private TimePickerView view;

    private RequestServiceCallback callback;

    public TimePickerPresenter(RequestServiceCallback callback){
        this.callback = callback;
    }

    public void subscribe(TimePickerView view){
        this.view = view;
    }
}
