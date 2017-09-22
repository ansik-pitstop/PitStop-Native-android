package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.models.report.VehicleHealthReport;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/22/2017.
 */

public class SortVehicleHealthReportUseCaseImpl implements SortVehicleHealthReportsUseCase {

    private Handler useCaseHandler;
    private Handler mainHandler;
    private List<VehicleHealthReport> vehicleHealthReports;
    private Callback callback;

    public SortVehicleHealthReportUseCaseImpl(Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    private void onSorted(List<VehicleHealthReport> vehicleHealthReports){
        mainHandler.post(() -> callback.onSorted(vehicleHealthReports));
    }

    @Override
    public void execute(List<VehicleHealthReport> vehicleHealthReports, Callback callback) {
        this.vehicleHealthReports = vehicleHealthReports;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {

    }
}
