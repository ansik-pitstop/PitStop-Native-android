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

    public void vinChanged(String vin){
        if (view == null) return;

        if (isVinValid(vin)){
            view.onValidVinInput();
        }
        else{
            view.onInvalidVinInput();
        }
    }

    public void addVehicle(String vin){
        vin = removeWhitespace(vin);

        //Add vehicle logic below
    }

    private boolean isVinValid(String vin){
        vin = removeWhitespace(vin);
        return vin != null && (vin.length() == 17);
    }

    private String removeWhitespace(String s){
        return s.replace(" ","").replace("\n","").replace("\t","");
    }

}
