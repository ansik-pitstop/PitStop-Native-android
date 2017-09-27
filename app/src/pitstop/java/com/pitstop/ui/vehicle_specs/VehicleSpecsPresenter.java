package com.pitstop.ui.vehicle_specs;

import android.app.Dialog;
import android.util.Log;
import android.widget.Toast;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddLicensePlateUseCase;
import com.pitstop.interactors.get.GetLicensePlateUseCase;
import com.pitstop.network.RequestError;
import com.pitstop.ui.Presenter;
import com.pitstop.ui.scan_car.ScanCarContract;
import com.pitstop.utils.MixpanelHelper;

/**
 * Created by ishan on 2017-09-25.
 */

public class VehicleSpecsPresenter implements Presenter<VehicleSpecsView>{

    private VehicleSpecsView view;

    private final static String TAG = VehicleSpecsPresenter.class.getSimpleName();
    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;

    public VehicleSpecsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    @Override
    public void subscribe(VehicleSpecsView view) {
        this.view  = view;


    }


    @Override
    public void unsubscribe() {
        this.view = null;

    }

    public void onUpdateLicensePlateDialogConfirmClicked(int carID, String s) {
        if(this.view == null)return;
        view.showLicensePlate(s);
        useCaseComponent.addLicensePlateUseCase().execute(carID, s, new AddLicensePlateUseCase.Callback() {
            @Override
            public void onLicensePlateStored(String licensePlate) {

            }

            @Override
            public void onError(RequestError error) {

            }
        });
        Log.d(TAG, "onUpdateLicensePlateDialogConfirmClicked()");
        Log.d(TAG, s);

    }

    public void getLicensePlate(int carID){
        if (this.view == null) return;

        useCaseComponent.getLicensePlateUseCase().execute(carID, new GetLicensePlateUseCase.Callback() {
            @Override
            public void onLicensePlateGot(String licensePlate) {
                view.showLicensePlate(licensePlate);
            }

            @Override
            public void onError(RequestError error) {
            }
        });

    }


}