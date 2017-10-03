package com.pitstop.ui.vehicle_health_report.past_reports;

import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetReportsUseCase;
import com.pitstop.interactors.other.SortReportsUseCase;
import com.pitstop.models.report.FullReport;
import com.pitstop.network.RequestError;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class PastReportsPresenter {

    private final String TAG = getClass().getSimpleName();

    private UseCaseComponent useCaseComponent;
    private MixpanelHelper mixpanelHelper;
    private PastReportsView view;
    private List<FullReport> savedReports;

    private boolean populating = false;

    public PastReportsPresenter(UseCaseComponent useCaseComponent, MixpanelHelper mixpanelHelper) {
        this.useCaseComponent = useCaseComponent;
        this.mixpanelHelper = mixpanelHelper;
    }

    void subscribe(PastReportsView view){
        Log.d(TAG,"subscribe() savedVehicleHealthReports == null? "
                +(savedReports==null));
        this.view = view;
    }

    void unsubscribe(){
        Log.d(TAG,"unsubscribe()");
        this.view = null;
    }

    void populateUI(){
        Log.d(TAG,"populateUI()");
        if (view == null || populating) return;
        view.displayLoading(true);
        populating = true;

        if (savedReports != null){
            populating = false;
            displayReports(savedReports);
            if (view != null)
                view.displayLoading(false);
            return;
        }

        //Get all the reports and call view.displayReports
        useCaseComponent.getGetVehicleHealthReportsUseCase()
                .execute(new GetReportsUseCase.Callback() {
                    @Override
                    public void onGotReports(List<FullReport> fullReports) {
                        savedReports = fullReports;
                        populating = false;
                        if (view == null) return;
                        displayReports(fullReports);
                        view.displayLoading(false);
                    }

                    @Override
                    public void onError(RequestError error) {
                        Log.d(TAG,"populateUI() error retrieving reports: "
                                +error.getMessage());
                        populating = false;
                        if (view == null) return;
                        view.displayError();
                        view.displayLoading(false);
                    }
                });
    }

    void onReportClicked(FullReport report){
        Log.d(TAG,"onReportClicked() report: "+report);
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_PAST_REPORT_ITEM, MixpanelHelper.VIEW_VHR_PAST_REPORTS);
        if (view != null)
            view.displayReport(report);
    }

    private void displayReports(List<FullReport> reports){
        if (view == null) return;
        if (reports.isEmpty()){
            view.displayNoHealthReports();
        }else{
            view.displayReports(reports);
        }
    }

    void onSortNewestDateClicked() {
        Log.d(TAG,"onSortNewestDateClicked()");
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_SORT_REPORTS_NEWEST, MixpanelHelper.VIEW_VHR_PAST_REPORTS);
        if (populating || savedReports == null) return;

        useCaseComponent.getSortReportsUseCase().execute(view.getDisplayedReports()
                , SortReportsUseCase.SortType.DATE_NEW
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }

    void onSortOldestDateClicked() {
        Log.d(TAG,"onSortOldestDateClicked()");
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_SORT_REPORTS_OLDEST, MixpanelHelper.VIEW_VHR_PAST_REPORTS);
        if (populating || savedReports == null) return;

        useCaseComponent.getSortReportsUseCase().execute(view.getDisplayedReports()
                , SortReportsUseCase.SortType.DATE_OLD
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }

    void onSortEngineIssuesClicked() {
        Log.d(TAG,"onSortEngineIssuesClicked()");
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_SORT_REPORTS_ENGINE_ISSUES, MixpanelHelper.VIEW_VHR_PAST_REPORTS);
        if (populating || savedReports == null || view == null) return;

        useCaseComponent.getSortReportsUseCase().execute(view.getDisplayedReports()
                , SortReportsUseCase.SortType.ENGINE_ISSUE
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }

    void onSortServicesClicked() {
        Log.d(TAG,"onSortServicesClicked()");
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_SORT_REPORTS_SERVICES, MixpanelHelper.VIEW_VHR_PAST_REPORTS);
        if (populating || savedReports == null) return;

        useCaseComponent.getSortReportsUseCase().execute(view.getDisplayedReports()
                , SortReportsUseCase.SortType.SERVICE
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }

    void onSortRecallsClicked() {
        Log.d(TAG,"onSortRecallsClicked()");
        mixpanelHelper.trackButtonTapped(
                MixpanelHelper.BUTTON_VHR_SORT_REPORTS_RECALL, MixpanelHelper.VIEW_VHR_PAST_REPORTS);
        if (populating || savedReports == null) return;

        useCaseComponent.getSortReportsUseCase().execute(view.getDisplayedReports()
                , SortReportsUseCase.SortType.RECALL
                , (vehicleHealthReports) -> { if (view != null) view.notifyReportDataChange(); });
    }
}
