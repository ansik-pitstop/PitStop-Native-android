package com.pitstop.interactors.other;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.report.VehicleHealthReport;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/22/2017.
 */

public interface SortVehicleHealthReportsUseCase extends Interactor {

    enum SortType{ DATE_NEW, DATE_OLD, ENGINE_ISSUE, SERVICE, RECALL }

    interface Callback{
        void onSorted(List<VehicleHealthReport> vehicleHealthReports);
        void onError();
    }

    void execute(List<VehicleHealthReport> vehicleHealthReports, SortType sortType
            , Callback callback);
}
