package com.pitstop.ui.add_car.ask_has_device;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class AskHasDevicePresenter {

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private AskHasDeviceView view;

    public AskHasDevicePresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){

        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(AskHasDeviceView view){
        this.view = view;
    }

    public void unsubscribe(){
        this.view = null;
    }

    public void onNoDeviceSelected(){

    }

    public void onHasDeviceSelected(){

    }

}
