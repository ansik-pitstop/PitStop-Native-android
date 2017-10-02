package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Settings;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.ReportRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

import java.util.Collections;
import java.util.List;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class GetReportUseCaseImpl implements GetReportsUseCase {

    private UserRepository userRepository;
    private ReportRepository reportRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private Callback callback;

    public GetReportUseCaseImpl(UserRepository userRepository
            , ReportRepository reportRepository, Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Callback callback) {
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onError(RequestError error){
        mainHandler.post(() -> callback.onError(error));
    }

    private void onGotVehicleHealthReports(List<VehicleHealthReport> vehicleHealthReports){
        mainHandler.post(() -> callback.onGotReports(vehicleHealthReports));
    }

    @Override
    public void run() {
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings data) {
                if (!data.hasMainCar()){
                    GetReportUseCaseImpl.this.onError(RequestError.getUnknownError());
                    return;
                }

                reportRepository.getVehicleHealthReports(data.getCarId(), new Repository.Callback<List<VehicleHealthReport>>() {
                    @Override
                    public void onSuccess(List<VehicleHealthReport> vehicleHealthReports) {
                        Collections.sort(vehicleHealthReports
                                , (t1,t2) -> t2.getDate().compareTo(t1.getDate()));
                        GetReportUseCaseImpl.this
                                .onGotVehicleHealthReports(vehicleHealthReports);
                    }

                    @Override
                    public void onError(RequestError error) {
                        GetReportUseCaseImpl.this.onError(error);
                    }
                });
            }

            @Override
            public void onError(RequestError error) {
                GetReportUseCaseImpl.this.onError(error);
            }
        });
    }
}
