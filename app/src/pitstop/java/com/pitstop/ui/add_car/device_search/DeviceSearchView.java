package com.pitstop.ui.add_car.device_search;

import android.view.View;

import com.continental.rvd.mobile_sdk.BindingQuestion;
import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error;
import com.pitstop.bluetooth.BluetoothService;
import com.pitstop.models.Car;
import com.pitstop.ui.LoadingView;

import io.reactivex.Observable;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public interface DeviceSearchView extends LoadingView{

    void onVinRetrievalFailed(String scannerName, String scannerId, int mileage);
    void onCannotFindDevice();
    String getMileage();
    void onMileageInvalid();
    void onCarAddedWithShop(Car car);
    void onCarAddedWithoutShop(Car car);
    void onErrorAddingCar(String message);
    void onCarAlreadyAdded(Car car);
    void showAskHasDeviceView();
    void beginPendingAddCarActivity(String vin, double mileage, String scannerId);
    void onCouldNotConnectToDevice();
    void connectingToDevice();
    void displayToast(int message);
    boolean isBluetoothServiceRunning();
    void startBluetoothService();
    void endBluetoothService();
    Observable<BluetoothService> getBluetoothService();
    boolean checkPermissions();
    String getDeviceType();
    void displayBindingDialog(BindingDialog.AnswerListener answerListener);
    void displayBindingQuestion(BindingQuestion bindingQuestion);
    void displayBindingProgress(float progress);
    void displayBindingFinished();
    void displayBindingError(Error error);
    void displayFirmwareInstallationDialog(View.OnClickListener onClickListener);
    void displayFirmwareInstallationInstruction(String instruction);
    void displayFirmwareInstallationProgress(float progress);
    void displayFirmwareInstallationFinished();
    void displayFirmwareInstallationError(String err);

}
