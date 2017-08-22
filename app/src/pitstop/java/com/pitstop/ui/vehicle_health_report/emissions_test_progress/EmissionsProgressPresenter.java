package com.pitstop.ui.vehicle_health_report.emissions_test_progress;

/**
 * Created by Matt on 2017-08-14.
 */

public class EmissionsProgressPresenter {

    private EmissionsProgressView view;
    private EmissionsProgressCallback callback;

    public EmissionsProgressPresenter(EmissionsProgressCallback callback){
        this.callback = callback;
    }

    public void subscribe(EmissionsProgressView view ){
        this.view = view;
        if(view == null){return;}
        view.setColors();
        view.setViewProgress();
    }

    public void unsubscribe(){
        this.view = null;
    }

    public void unsubscribe(){
        this.view = null;
    }
}
