package com.pitstop.ui.service_request;

import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Matthew on 2017-07-11.
 */

public class RequestServicePresenter {
    private RequestServiceView view;
    private RequestServiceCallback callback;

    private MixpanelHelper mixpanelHelper;



    public RequestServicePresenter(RequestServiceCallback callback, MixpanelHelper mixpanelHelper){
        this.callback = callback;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(RequestServiceView view){
        this.view = view;
        setViewMainForm();
    }

    public void unsubscribe(){
        view = null;
    }

    public void setViewMainForm(){
        if(view == null || callback == null){return;}
        callback.setViewMainForm();
    }
}
