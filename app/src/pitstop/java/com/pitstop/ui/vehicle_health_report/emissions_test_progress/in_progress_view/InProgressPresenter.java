package com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view;

/**
 * Created by Matt on 2017-08-14.
 */

public class InProgressPresenter {

    private InProgressView view;

    public void subscribe(InProgressView view){
        this.view = view;
    }
}
