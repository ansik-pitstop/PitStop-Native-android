package com.pitstop.ui.add_car.vin_entry;

import com.pitstop.EventBus.EventSource;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.network.RequestError;
import com.pitstop.utils.AddCarUtils;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class VinEntryPresenter {

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private VinEntryView view;
    private String scannerName = "";
    private String scannerId = "";


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

        if (AddCarUtils.isVinValid(vin)){
            view.onValidVinInput();
        }
        else{
            view.onInvalidVinInput();
        }
    }

    public void addVehicle(String vin){
        vin = AddCarUtils.removeWhitespace(vin);
        int mileage = view.getMileage();

        //Check for valid vin again, even though it should be valid here
        if (!AddCarUtils.isVinValid(vin)){
            view.onInvalidVinInput();
            return;
        }

        //Check for valid mileage
        if (!AddCarUtils.isMileageValid(mileage)){
            view.onInvalidMileage();
            return;
        }

        //Add vehicle logic below
        useCaseComponent.addCarUseCase().execute(vin, mileage, scannerId, scannerName
                , EventSource.SOURCE_ADD_CAR, new AddCarUseCase.Callback() {
            @Override
            public void onCarAddedWithBackendShop(Car car) {
                if (view == null) return;

                view.onCarAddedWithShop(car);
            }

            @Override
            public void onCarAdded(Car car) {
                if (view == null) return;

                view.onCarAddedWithoutShop(car);
            }

            @Override
            public void onError(RequestError error) {
                if (error.getError().equals(RequestError.ERR_OFFLINE)){
                    view.onErrorAddingCar("Please connect to the internet to add your vehicle.");
                }
                else{
                    view.onErrorAddingCar("Unexpected error occured adding car" +
                            ", please restart the app and try again.");
                }
            }
        });

    }

    public void gotDeviceInfo(String scannerName, String scannerId){
        this.scannerName = scannerName;
        this.scannerId = scannerId;
    }

}
