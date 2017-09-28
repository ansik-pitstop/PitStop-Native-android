package com.pitstop.interactors.add;

import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.EmissionsReport;
import com.pitstop.models.Settings;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.ReportRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;

/**
 * Created by Karol Zdebel on 9/19/2017.
 */

public class GenerateReportUseCaseImpl implements GenerateReportUseCase {

    private final String TAG = getClass().getSimpleName();

    private ReportRepository reportRepository;
    private UserRepository userRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;

    private PidPackage pid;
    private DtcPackage dtc;
    private Callback callback;

    public GenerateReportUseCaseImpl(ReportRepository reportRepository
            , UserRepository userRepository, Handler mainHandler, Handler useCaseHandler) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.mainHandler = mainHandler;
        this.useCaseHandler = useCaseHandler;
    }

    @Override
    public void execute(PidPackage pid, DtcPackage dtc, Callback callback) {
        Log.d(TAG,"execute() pid: "+pid+", dtc: "+dtc);
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
            public void onSuccess(Settings settings) {

                if (!settings.hasMainCar()) GenerateReportUseCaseImpl
                        .this.onError(RequestError.getUnknownError());

                reportRepository.createEmissionsReport(settings.getCarId()
                        , false, dtc, pid, new Repository.Callback<EmissionsReport>() {
                            
                            @Override
                            public void onSuccess(EmissionsReport emissionsReport) {

                                reportRepository.createVehicleHealthReport(settings.getCarId(), false, dtc, pid
                                        , new Repository.Callback<VehicleHealthReport>() {
                                            @Override
                                            public void onSuccess(VehicleHealthReport report) {
                                                Log.d(TAG,"onSuccess() report: "+report);
                                                GenerateReportUseCaseImpl.this.onReportAdded(report);
                                            }

                                            @Override
                                            public void onError(RequestError error) {
                                                Log.d(TAG,"onError() error: "+error);
                                                GenerateReportUseCaseImpl.this.onError(error);
                                            }
                                        });
                            }

                            @Override
                            public void onError(RequestError error) {
                                GenerateReportUseCaseImpl.this.onError(error);
                            }
                        });


            }

            @Override
            public void onError(RequestError error) {
                GenerateReportUseCaseImpl.this.onError(error);
            }
        });
    }

}
