package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.models.report.VehicleHealthReport;

import java.util.Collections;
import java.util.List;

/**
 * Created by Karol Zdebel on 9/22/2017.
 */

public class SortVehicleHealthReportUseCaseImpl implements SortVehicleHealthReportsUseCase {

    private final String TAG = getClass().getSimpleName();

    private Handler useCaseHandler;
    private Handler mainHandler;
    private List<VehicleHealthReport> vehicleHealthReports;
    private SortType sortType;
    private Callback callback;

    public SortVehicleHealthReportUseCaseImpl(Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(List<VehicleHealthReport> vehicleHealthReports, SortType sortType
            , Callback callback) {
        this.vehicleHealthReports = vehicleHealthReports;
        this.sortType = sortType;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        switch (sortType){
            case DATE_NEW:
                Collections.sort(vehicleHealthReports
                        ,(t1, t2) -> t1.getDate().compareTo(t2.getDate()));
                break;
            case DATE_OLD:
                Collections.sort(vehicleHealthReports
                        ,(t1, t2) -> t2.getDate().compareTo(t1.getDate()));
                break;
            case ENGINE_ISSUE:
                Collections.sort(vehicleHealthReports
                        ,(t1,t2) -> t2.getEngineIssues().size() - t1.getEngineIssues().size());
                break;
            case SERVICE:
                Collections.sort(vehicleHealthReports
                        , (t1,t2) -> t2.getServices().size() - t1.getServices().size());
                break;
            case RECALL:
                Collections.sort(vehicleHealthReports
                        , (t1,t2) -> t2.getRecalls().size() - t1.getServices().size());
                break;
            default:
                Collections.sort(vehicleHealthReports
                        , (t1, t2) -> t1.getDate().compareTo(t2.getDate()));
        }

        mainHandler.post(() -> callback.onSorted(vehicleHealthReports));

    }
}
