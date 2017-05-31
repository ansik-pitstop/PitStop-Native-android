package com.pitstop.ui.scan_car;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.models.CarIssue;
import com.pitstop.ui.BaseView;
import com.pitstop.ui.BluetoothPresenter;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.ILoadingActivity;

import java.util.Set;


public interface ScanCarContract {

    public interface MainActivity {
        public void onScanCarFinished(boolean updatedMileageOrDtcsFound);
    }

    interface View extends BaseView<Presenter>, ILoadingActivity{

        /**
         * Invoked when the OBD device is connected
         */
        void onDeviceConnected();

        /**
         * Invoked when user started scan car process, but we cannot connect to the OBD device for too long
         */
        void onConnectingTimeout();

        /**
         * callback for updateMileage(String input);
         * @param updatedMileage
         */
        void onInputtedMileageUpdated(double updatedMileage);

        /**
         * callback for live mileage updates (OBD trip data)
         * @param updatedMileage
         */
        void onTripMileageUpdated(double updatedMileage);

        /**
         * callback for getServicesAndRecalls();
         */
        void onRecallRetrieved(@Nullable Set<CarIssue> recalls);

        /**
         * callback for getServicesAndRecalls();
         */
        void onServicesRetrieved(@Nullable Set<CarIssue> services);

        /**
         * Invoked when we retrieved real time data
         */
        void onRealTimeDataRetrieved();

        /**
         * Invoked when we should be getting DTC, but we cannot retrieve real time data for too long
         */
        void onGetRealTimeDataTimeout();

        /**
         * callback for getEngineCodes();
         * @param dtcCodes set of retrieved dtcs, can be empty.
         */
        void onEngineCodesRetrieved(@Nullable Set<String> dtcCodes);

        /**
         * Check if connected, if not show a toast
         * @param errorToShow Error message show to the user if network is unavailable
         * @return false if not connected, true if connected
         */
        boolean checkNetworkConnection(@Nullable String errorToShow);

        void onNetworkError(@NonNull String errorMessage);

        boolean isScanning();

        BluetoothAutoConnectService getAutoConnectService();

        IBluetoothServiceActivity getBluetoothActivity();
    }

    interface Presenter extends BluetoothPresenter{

        double getLatestMileage();

        void connectToDevice();

        /**
         * @param input validated mileage(non-negative, less than max value)
         */
        void updateMileage(double input);

        /**
         * Check if OBD device is uploading the real time data to the app <br>
         * The reason we should check this prior to getting engine codes is that
         * for IDD-212B devices, engine codes will not be returned if it is still uploading non-realtime data. <br>
         */
        void checkRealTime();

        /**
         * Ask BluetoothAutoConnectService to get engine codes from the device <br>
         * The callback for this method is dtcData(DtcPackage) <br>
         */
        void getEngineCodes();

        /**
         * Make network calls to our backend to see if any services/recalls is currently active
         */
        void getServicesAndRecalls();

        /**
         * Do cleanup when scan is finished, e.g. cancel all timeoutTimers
         */
        void finishScan();

        void onActivityFinish();
    }

}
