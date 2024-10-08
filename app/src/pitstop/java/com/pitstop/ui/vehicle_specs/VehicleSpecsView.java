package com.pitstop.ui.vehicle_specs;

import com.pitstop.models.Car;
import com.pitstop.models.Dealership;

/**
 * Created by ishan on 2017-09-25.
 */

public interface VehicleSpecsView {

    void showLicensePlate(String s);
    void toast(String message);
    void showLoading();
    void hideLoading();
    void showLoadingDialog(String message);
    void hideLoadingDialog();
    void setCarView(Car car);
    void updateMainActivityCar(Car car);
    void showBuyDeviceDialog();
    void showNoCarView();
    void showOfflineErrorView();
    void showUnknownErrorView();
    void displayUnknownErrorDialog();
    void displayOfflineErrorDialog();
    boolean hasBeenPopulated();
    void showNormalLayout();
    void showFuelConsumptionExplanationDialog();
    void showFuelConsumed(double fuelConsumed);
    void showFuelExpensesDialog();
    void showFuelExpense(float v);
    String getLastKnowLocation();
    void displayDefaultDealershipVisuals(Dealership dealership);
    void displayCarDetails(Car car);
    void openAlarmsActivity();
    void hideBadge();
    void showBadges(int alarmCount);
    void startAddCarActivity();
    void showPairScannerDialog();
    void showScannerAlreadyActiveDialog();
    void showScannerID(String s);
    void showConfirmUpdateScannerDialog(String s);
    void setCarUnitOfLength(String s);
    void displayCityMileage(String s);
    void displayHighwayMileage(String s);
}
