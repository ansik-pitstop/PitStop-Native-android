package com.pitstop.interactors.add;

import android.os.Handler;
import android.util.Log;

import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.ReportRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

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
        Logger.getInstance().logI(TAG,"Use case execution started: pid="+pid+", dtc="+dtc
                , DebugMessage.TYPE_USE_CASE);
        this.pid = pid;
        this.dtc = dtc;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    private void onReportAddedWithoutEmissions(VehicleHealthReport vehicleHealthReport){
        Logger.getInstance().logI(TAG,"Use case finished: added without emissions result="+vehicleHealthReport
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onReportAddedWithoutEmissions(vehicleHealthReport));
    }

    private void onReportAdded(VehicleHealthReport vehicleHealthReport
            , EmissionsReport emissionsReport){
        Logger.getInstance().logI(TAG,"Use case execution started: added with emissions, vhr="+vehicleHealthReport
                +"et="+emissionsReport, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onReportAdded(vehicleHealthReport,emissionsReport));
    }

    @Override
    public void run() {
        Log.d(TAG,"run()");
        userRepository.getCurrentUserSettings(new Repository.Callback<Settings>() {
            @Override
            public void onSuccess(Settings settings) {
                Log.d(TAG,"getCurrentUserSettings.onSuccess(), settings.hasMainCar()? "+settings.hasMainCar());
                if (!settings.hasMainCar()){
                    GenerateReportUseCaseImpl
                            .this.onError(RequestError.getUnknownError());
                    return;
                }

                reportRepository.createVehicleHealthReport(settings.getCarId()
                        , false, dtc, pid, new Repository.Callback<VehicleHealthReport>() {
                            @Override
                            public void onSuccess(VehicleHealthReport vhr) {
                                Log.d(TAG,"vhr generated: "+vhr);
                                if (pid.pids.containsKey("2141")){
                                    reportRepository.createEmissionsReport(settings.getCarId()
                                            ,vhr.getId(), false, dtc, pid
                                            , new Repository.Callback<EmissionsReport>() {

                                                @Override
                                                public void onSuccess(EmissionsReport et) {
                                                    Log.d(TAG,"onSuccess() vhr report: "+vhr
                                                            + "et report: "+et);
                                                    GenerateReportUseCaseImpl.this
                                                            .onReportAdded(vhr, et);
                                                }

                                                @Override
                                                public void onError(RequestError error) {
                                                    Log.d(TAG,"Error generating emissions report error: "
                                                            +error.getMessage());
                                                    if (error.getStatusCode() == 400){
                                                        GenerateReportUseCaseImpl.this
                                                                .onReportAddedWithoutEmissions(vhr);
                                                    }
                                                    else{
                                                        GenerateReportUseCaseImpl.this.onError(error);
                                                    }
                                                }
                                            });
                                }else{
                                    GenerateReportUseCaseImpl.this
                                            .onReportAddedWithoutEmissions(vhr);
                                }

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

}
