package com.pitstop.ui.add_car.ask_has_device;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class AskHasDevicePresenter {

    private final String TAG = getClass().getSimpleName();

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private AskHasDeviceView view;

    public AskHasDevicePresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){

        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(AskHasDeviceView view){
        Log.d(TAG,"subscribe()");
        this.view = view;
    }

    public void unsubscribe() {
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    public void onNoDeviceSelected(){
        Log.d(TAG,"onNoDeviceSelected()");
        if (view == null) return;

        view.loadVinEntryView();
    }

    public void onHasDeviceSelected(){
        Log.d(TAG,"onHasDeviceSelected()");
        if (view == null) return;

        view.loadDeviceSearchView();
    }

}
