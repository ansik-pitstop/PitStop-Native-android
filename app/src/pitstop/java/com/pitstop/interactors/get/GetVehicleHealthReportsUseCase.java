package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public interface GetVehicleHealthReportsUseCase extends Interactor {
    interface Callback {
        void onGotVehicleHealthReports(List<VehicleHealthReport> vehicleHealthReports);
        void onError(RequestError error);
    }

    void execute(Callback callback);
}
