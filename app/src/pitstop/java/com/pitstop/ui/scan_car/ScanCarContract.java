package com.pitstop.ui.scan_car;

import android.support.annotation.Nullable;

import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.models.CarIssue;
import com.pitstop.ui.BSAbstractedFragmentActivity;
import com.pitstop.ui.BaseView;
import com.pitstop.ui.BluetoothPresenter;
import com.pitstop.ui.ILoadingActivity;

import java.util.Set;


public interface ScanCarContract {

    interface View extends BaseView, ILoadingActivity{

        void onDeviceConnected();

        void onDeviceDisconnected();

        void onConnectingTimeout();

        /**
         * callback for updateMileage(String input);
         * @param updatedMileage
         */
        void onInputtedMileageUpdated(double updatedMileage);

        /**
         * Trip mileage updates
         * @param updatedMileage
         */
        void onTripMileageUpdated(double updatedMileage);

        /**
         * callback for getServicesAndRecalls();
         */
        void onRecallRetrieved(Set<CarIssue> recalls);

        /**
         * callback for getServicesAndRecalls();
         */
        void onServicesRetrieved(Set<CarIssue> services);

        void onGetRealtimeDataTimeout();

        /**
         * callback for getEngineCodes();
         * @param dtcCodes set of retrieved dtcs, can be empty.
         */
        void onEngineCodesRetrieved(Set<String> dtcCodes);

        /**
         * Check if connected, if not show a toast
         * @param errorToShow Error message show to the user if network is unavailable
         * @return false if not connected, true if connected
         */
        boolean checkNetworkConnection(@Nullable String errorToShow);

        void onNetworkError(String errorMessage);

        boolean isScanning();

        BluetoothAutoConnectService getAutoConnectService();

        BSAbstractedFragmentActivity getActivity();
    }

    interface Presenter extends BluetoothPresenter{

        void connectToDevice();

        /**
         * @param input validated mileage(non-negative, less than max value)
         */
        void updateMileage(double input);

        void getEngineCodes();

        void getServicesAndRecalls();

        void finishScan();

        void onActivityFinish();
    }

}
