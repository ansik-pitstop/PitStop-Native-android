package com.pitstop.interactors.other;

import android.os.Handler;

import com.pitstop.models.DebugMessage;
import com.pitstop.models.report.FullReport;
import com.pitstop.utils.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by Karol Zdebel on 9/22/2017.
 */

public class SortReportsUseCaseImpl implements SortReportsUseCase {

    private final String TAG = getClass().getSimpleName();

    private Handler useCaseHandler;
    private Handler mainHandler;
    private List<FullReport> reports;
    private SortType sortType;
    private Callback callback;

    public SortReportsUseCaseImpl(Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    @Override
    public void execute(List<FullReport> reports, SortType sortType
            , Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: reports="+reports
                , DebugMessage.TYPE_USE_CASE);
        this.reports = reports;
        this.sortType = sortType;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        switch (sortType){
            case DATE_NEW:
                Collections.sort(reports
                        ,(t1, t2) -> t2.getVehicleHealthReport().getDate()
                                .compareTo(t1.getVehicleHealthReport().getDate()));
                break;
            case DATE_OLD:
                Collections.sort(reports
                        ,(t1, t2) -> t1.getVehicleHealthReport().getDate()
                                .compareTo(t2.getVehicleHealthReport().getDate()));
                break;
            case ENGINE_ISSUE:
                Collections.sort(reports
                        ,(t1,t2) -> t2.getVehicleHealthReport().getEngineIssues().size()
                                - t1.getVehicleHealthReport().getEngineIssues().size());
                break;
            case SERVICE:
                Collections.sort(reports
                        , (t1,t2) -> t2.getVehicleHealthReport().getServices().size()
                                - t1.getVehicleHealthReport().getServices().size());
                break;
            case RECALL:
                Collections.sort(reports
                        , (t1,t2) -> t2.getVehicleHealthReport().getRecalls().size()
                                - t1.getVehicleHealthReport().getServices().size());
                break;
            default:
                Collections.sort(reports
                        , (t1, t2) -> t1.getVehicleHealthReport().getDate()
                                .compareTo(t2.getVehicleHealthReport().getDate()));
        }

        mainHandler.post(() -> callback.onSorted(reports));
        Logger.getInstance().logI(TAG,"Use case finished: reports="+reports
                , DebugMessage.TYPE_USE_CASE);

    }
}
