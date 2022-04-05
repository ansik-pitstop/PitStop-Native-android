package com.pitstop.interactors.get;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.Settings;
import com.pitstop.models.report.EmissionsReport;
import com.pitstop.models.report.FullReport;
import com.pitstop.models.report.VehicleHealthReport;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.ReportRepository;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class GetReportUseCaseImpl implements GetReportsUseCase {

    private final String TAG = getClass().getSimpleName();

    private UserRepository userRepository;
    private ReportRepository reportRepository;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private Callback callback;
    private Integer userId;

    public GetReportUseCaseImpl(UserRepository userRepository
            , ReportRepository reportRepository, Handler useCaseHandler, Handler mainHandler) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(Integer userId, Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.userId = userId;
        useCaseHandler.post(this);
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    private void onGotReports(List<FullReport> fullReports){
        Logger.getInstance().logI(TAG, "Use case finished: reports="+fullReports
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onGotReports(fullReports));
    }

    @Override
    public void run() {
        reportRepository.getVehicleHealthReports(userId, new Repository.Callback<List<VehicleHealthReport>>() {
            @Override
            public void onSuccess(List<VehicleHealthReport> vehicleHealthReports) {
                Collections.sort(vehicleHealthReports
                        , (t1,t2) -> t2.getDate().compareTo(t1.getDate()));

                reportRepository.getEmissionReports(userId, new Repository.Callback<List<EmissionsReport>>() {
                    @Override
                    public void onSuccess(List<EmissionsReport> emissionsReports) {
                        Log.d(TAG,"Got emission reports: "+emissionsReports);

                        List<FullReport> fullReports = new ArrayList<>();
                        List<VehicleHealthReport> toRemove = new ArrayList<>();
                        for (VehicleHealthReport v: vehicleHealthReports){
                            for (EmissionsReport e: emissionsReports){
                                if (e.getVhrId() != -1 && e.getVhrId() == v.getId()){
                                    fullReports.add(new FullReport(v,e));
                                    toRemove.add(v);
                                }

                            }
                        }
                        Log.d(TAG,"Got full reports: "+fullReports);
                        vehicleHealthReports.removeAll(toRemove);
                        for (VehicleHealthReport v: vehicleHealthReports){
                            fullReports.add(new FullReport(v));
                        }
                        GetReportUseCaseImpl.this
                                .onGotReports(fullReports);

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
