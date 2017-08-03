package com.pitstop.ui.services.custom_service;

/**
 * Created by Matt on 2017-07-25.
 */

public class CustomServicePresenter {
    private CustomServiceView view;

    public void subscribe(CustomServiceView view){
        this.view = view;
        setViewServiceForm();
    }
    public void unsubscribe(){
        this.view = null;
    }

    public void setViewServiceForm(){
        if(view == null){return;}
        view.setViewServiceForm();
    }
}
