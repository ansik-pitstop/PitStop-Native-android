package com.pitstop.ui.vehicle_health_report.start_report;

import com.jjoe64.graphview.series.DataPoint;
import com.pitstop.observer.BluetoothConnectionObservable;

import io.reactivex.Observable;

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
    void promptAddCar();
    void startAddCar();
    void displaySearchInProgress();
    void displayOffline();
    void changeTitle(int stringId, boolean progress);
    boolean checkPermissions();
    void startBluetoothService();
    boolean isBluetoothServiceRunning();
    void displaySeriesData(String series, DataPoint dataPoint);
    void startGraphActivity();
    Observable<BluetoothConnectionObservable> getBluetoothConnectionObservable();
    void displayBluetoothConnectionRequirePrompt();
    void displayLiveDataNotSupportedPrompt();
    void setLiveDataButtonEnabled(boolean enabled);
}
