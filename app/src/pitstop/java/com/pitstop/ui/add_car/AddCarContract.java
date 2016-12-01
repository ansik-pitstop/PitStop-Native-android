package com.pitstop.ui.add_car;

import android.content.DialogInterface;
import android.support.annotation.Nullable;

import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.ui.BaseView;
import com.pitstop.ui.BluetoothPresenter;
import com.pitstop.utils.BSAbstractedFragmentActivity;
import com.pitstop.utils.ILoadingActivity;

/**
 * Created by yifan on 16/11/21.
 */

public interface AddCarContract {

    interface View extends BaseView, ILoadingActivity {

        /**
         * After user entered the mileage
         */
        void onMileageEntered();

        /**
         * After device is successfully connected
         */
        void onDeviceConnected();

        /**
         * After retrieved RTC
         * @param needToBeSet true if RTC is off by one year
         */
        void onRTCRetrieved(boolean needToBeSet);

        /**
         * After reset RTC to current time
         */
        void onRTCReset();

        /**
         * @param VIN retrieved VIN, can be null if isValid is false
         * @param isValid
         */
        void onVINRetrieved(@Nullable String VIN, boolean isValid);

        /**
         * Show alert dialog when Search/Connect/GetVin timed out
         */
        void onTimeoutRetry(final String timeoutEvent, final String mixpanelEvent);

        /**
         * Show confirm postCar dialog
         */
        void onConfirmPostCar(String deviceId, String VIN);

        void onConfirmAddingDeletedCar(Car deletedCar, DialogInterface.OnClickListener positiveButton);

        void onPostCarStarted();

        void onPostCarFailed(String errorMessage);

        void onPostCarSucceeded(Car createdCar);

        /**
         * If VIN returned is "not support" for more than 8 times, or VIN exists in the backend
         */
        void askForManualVinInput();

        /**
         * If user is adding a new car (with no previous dealership info), or <br>
         * the user is using a scanner that has not been associate with a dealership in the backend
         */
        void askForDealership();

        /**
         * If during pairing unrecognized scanner, VIN is "not support" or has not been returned
         *
         * @param scannerName
         * @param scannerId
         */
        void showSelectCarDialog(String scannerName, String scannerId);


        /**
         * Show a confirm dialog to asking user to pair car with unrecognized device
         * @param existedCar
         * @param scannerName
         * @param scannerId
         */
        void confirmPairCarWithDevice(Car existedCar, String scannerName, String scannerId);


        void onPairingDeviceWithCar();

        /**
         * Show error message for pairing unrecognized device
         * @param errorMessage
         */
        void pairCarError(final String errorMessage);

        /**
         * After unrecognized device got successfully paired
         */
        void onDeviceSuccessfullyPaired();

        /**
         * Check if connected, if not show a toast
         * @param errorToShow Error message show to the user if network is unavailable
         * @return false if not connected, true if connected
         */
        boolean checkNetworkConnection(@Nullable String errorToShow);

        void startPendingAddCarActivity(Car pendingCar);

        void showRetryDialog(String title, String message,
                        DialogInterface.OnClickListener retryButton,
                        DialogInterface.OnClickListener cancelButton);

        BluetoothAutoConnectService getAutoConnectService();

        BSAbstractedFragmentActivity getActivity();
    }



    interface Presenter extends BluetoothPresenter{

        /**
         * @param mileage the mileage entered by user after starting searching for car
         */
        void updatePendingCarMileage(int mileage);

        void cancelUpdateMileage();

        /**
         * Search for OBD devices <br>
         * On device connected, ask for VIN <br>
         * If Vin has already been entered, create car with pending Vin <br>
         * BT Searching has a timeout of 15 seconds * 3 <br>
         * Get VIN has a timeout of 30 seconds * 2 <br>
         */
        void searchAndGetVin();

        /**
         * Similar to searchAndGetVin <br>
         * But does not create the car <br>
         */
        void searchForUnrecognizedDevice();

        /**
         * If the user is adding car without a device, we let them enter the VIN themselves
         * @param VIN
         */
        void setPendingCarVin(final String VIN);

        /**
         * Do VIN validation (or not) and POST new car on success <br>
         * Using synchronized (?) to prevent adding the same car twice at the same time
         */
        void startAddingNewCar();

        /**
         * Do Vin validation and update scanner info
         */
        void startPairingUnrecognizedDevice();

        /**
         * @param car
         * @return true if this car has no scanner
         */
        boolean selectedValidCar(Car car);

        /**
         * See if the scanner is in use, if not create new scanner.
         * @param car
         * @param scannerId
         * @param scannerName
         */
        void validateAndPostScanner(final Car car, final String scannerId, final String scannerName);

        /**
         * Update the shop for created car. <br>
         * @param dealership
         */
        void updateCreatedCarDealership(final Dealership dealership);

        void cancelAllTimeouts();

        void finish();

        Car getPendingCar();

        Car getCreatedCar();

        boolean hasGotMileage();
    }

}
