package com.pitstop.ui.vehicle_health_report.emissions_test_progress;

import android.util.Log;

/**
 * Created by Matt on 2017-08-14.
 */

public class EmissionsProgressPresenter {

    private final String TAG = getClass().getSimpleName();

    private EmissionsProgressView view;
    private EmissionsProgressCallback callback;

    public EmissionsProgressPresenter(EmissionsProgressCallback callback){
        this.callback = callback;
    }

    public void subscribe(EmissionsProgressView view ){
        Log.d(TAG,"subscribe()");
        this.view = view;
        if(view == null){return;}
        view.setColors();
        view.setViewProgress();
    }

    public void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    public void unsubscribe(){
        this.view = null;
    }
}
