package com.pitstop.interactors.add;

import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.interactors.Interactor;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 9/19/2017.
 */

public interface GenerateReportUseCase extends Interactor {

    interface Callback{
        void onReportAddedWithoutEmissions(VehicleHealthReport vehicleHealthReport);
        void onReportAdded(VehicleHealthReport vehicleHealthReport
                , EmissionsReport emissionsReport);
        void onError(RequestError requestError);
    }

    void execute(PidPackage pid, DtcPackage dtc, Callback callback);
}
