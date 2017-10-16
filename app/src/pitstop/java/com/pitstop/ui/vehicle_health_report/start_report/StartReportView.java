package com.pitstop.ui.vehicle_health_report.start_report;

import com.pitstop.observer.BluetoothConnectionObservable;

/**
 * Created by Matt on 2017-08-11.
 */

public interface StartReportView {
    void setModeEmissions();
    void setModeHealthReport();
    void startEmissionsProgressActivity();
    void startVehicleHealthReportProgressActivity();
    void startPastReportsActivity();
    void promptBluetoothSearch();
    void displaySearchInProgress();
    void displayOffline();
    BluetoothConnectionObservable getBluetoothConnectionObservable();
}
