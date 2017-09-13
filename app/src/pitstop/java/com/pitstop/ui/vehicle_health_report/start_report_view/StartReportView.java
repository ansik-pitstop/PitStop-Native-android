package com.pitstop.ui.vehicle_health_report.start_report_view;

import com.pitstop.observer.BluetoothConnectionObservable;

/**
 * Created by Matt on 2017-08-11.
 */

public interface StartReportView {
    void setModeEmissions();
    void setModeHealthReport();
    void startEmissionsProgressActivity();
    void startVehicleHealthReportProgressActivity();
    void displayNoBluetoothConnection();
    BluetoothConnectionObservable getBluetoothConnectionObservable();
}
