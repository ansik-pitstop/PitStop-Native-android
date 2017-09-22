package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.models.report.VehicleHealthReport;

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

    private void onSorted(List<VehicleHealthReport> vehicleHealthReports){
        mainHandler.post(() -> callback.onSorted(vehicleHealthReports));
    }

    @Override
    public void execute(List<VehicleHealthReport> vehicleHealthReports, SortType sortType
            , Callback callback) {
        this.vehicleHealthReports = vehicleHealthReports;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        switch (sortType){
            case DATE_NEW:
                break;
            case DATE_OLD:
                break;
            case ENGINE_ISSUE:
                break;
            case SERVICE:
                break;
            case RECALL:
                break;
            default:

        }
    }

    private void sortByDate(List<VehicleHealthReport> vehicleHealthReports, boolean newest){
        if (newest){
            vehicleHealthReports.sort((t1, t2) -> t1.getDate().compareTo(t2.getDate()));
        }else{
            vehicleHealthReports.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
        }
    }

    private void sortByEngineIssues(List<VehicleHealthReport> vehicleHealthReports){

    }
}
