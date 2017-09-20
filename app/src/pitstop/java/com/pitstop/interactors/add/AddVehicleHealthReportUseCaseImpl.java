package com.pitstop.interactors.add;

import android.os.Handler;

import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.Settings;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.ReportRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 9/19/2017.
 */

public class AddVehicleHealthReportUseCaseImpl implements AddVehicleHealthReportUseCase {

    private ReportRepository reportRepository;
    private UserRepository userRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;

    private PidPackage pid;
    private DtcPackage dtc;
    private Callback callback;

    public AddVehicleHealthReportUseCaseImpl(ReportRepository reportRepository
            , UserRepository userRepository, Handler mainHandler, Handler useCaseHandler) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.mainHandler = mainHandler;
        this.useCaseHandler = useCaseHandler;
    }

    @Override
    public void execute(PidPackage pid, DtcPackage dtc, Callback callback) {
        this.pid = pid;
        this.dtc = dtc;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    private void onReportAdded(VehicleHealthReport vehicleHealthReport){
        mainHandler.post(() -> callback.onReportAdded(vehicleHealthReport));
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                if (!data.hasMainCar()) AddVehicleHealthReportUseCaseImpl
                        .this.onError(RequestError.getUnknownError());
                reportRepository.createVehicleHealthReport(data.getCarId(), false, dtc, pid
                        , new Repository.Callback<VehicleHealthReport>() {
                    @Override
                    public void onSuccess(VehicleHealthReport report) {
                        AddVehicleHealthReportUseCaseImpl.this.onReportAdded(report);
                    }

                    @Override
                    public void onError(RequestError error) {
                        AddVehicleHealthReportUseCaseImpl.this.onError(error);
                    }
                });

            }

            @Override
            public void onError(RequestError error) {
                AddVehicleHealthReportUseCaseImpl.this.onError(error);
            }
        });
    }

}
