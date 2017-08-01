package com.pitstop.ui.add_car.device_search;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class DeviceSearchPresenter {

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private DeviceSearchView view;

    public DeviceSearchPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(DeviceSearchView view){
        this.view = view;
    }

    public void unsubscribe(){
        this.view = null;
    }

    public void startSearch(){

    }
}
