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
    private String scannerName;
    private String scannerId;
    private boolean gotScannerInfo = false;


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
        int mileage = view.getMileage();

        //Check for valid vin again, even though it should be valid here
        if (!isVinValid(vin)){
            view.onInvalidVinInput();
            return;
        }

        //Check for valid mileage
        if (mileage < 0 || mileage > 3000000){
            view.onInvalidMileage();
            return;
        }

        //Add vehicle logic below
        if (!gotScannerInfo){
            //Add without scanner
        }
        else{
            //Add with scanner
        }

    }

    public void gotDeviceInfo(String scannerName, String scannerId){
        gotScannerInfo = true;
        this.scannerName = scannerName;
        this.scannerId = scannerId;
    }

    private boolean isVinValid(String vin){
        vin = removeWhitespace(vin);
        return vin != null && (vin.length() == 17);
    }

    private String removeWhitespace(String s){
        return s.replace(" ","").replace("\n","").replace("\t","");
    }

}
