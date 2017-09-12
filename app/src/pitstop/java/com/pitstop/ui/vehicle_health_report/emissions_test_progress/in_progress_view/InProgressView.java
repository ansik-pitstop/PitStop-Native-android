package com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view;

/**
 * Created by Matt on 2017-08-14.
 */

public interface InProgressView {
    void switchToProgress();
    void setReady();
    void bounceCards();
    void changeStep(String step);
    int getCardNumber();
    void back();
    void next();
    void startTimer();
    void endProgress(String message);
    void toast(String message);

}
