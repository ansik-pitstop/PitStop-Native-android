package com.pitstop.ui.add_car.vin_entry;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class VinEntryPresenter {

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private VinEntryView view;

    public VinEntryPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper){
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    public void subscribe(VinEntryView view){
        this.view = view;
    }

    public void unsubscribe(){
        this.view = null;
    }

}
