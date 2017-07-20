package com.pitstop.ui.service_request;

/**
 * Created by Matthew on 2017-07-11.
 */

public class RequestServicePresenter {
    private RequestServiceView view;
    private RequestServiceCallback callback;



    public RequestServicePresenter(RequestServiceCallback callback){
        this.callback = callback;
    }

    public void subscribe(RequestServiceView view){
        this.view = view;
    }

    public void setViewMainForm(){
        callback.setViewMainForm();
    }
}
