package com.pitstop.ui.scan_car;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.models.CarIssue;
import com.pitstop.ui.BSAbstractedFragmentActivity;
import com.pitstop.ui.BaseView;
import com.pitstop.ui.BluetoothPresenter;
import com.pitstop.ui.ILoadingActivity;

import java.util.Set;


public interface ScanCarContract {

    interface View extends BaseView<Presenter>, ILoadingActivity{

        void onDeviceConnected();

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
        void onRecallRetrieved(@Nullable Set<CarIssue> recalls);

        /**
         * callback for getServicesAndRecalls();
         */
        void onServicesRetrieved(@Nullable Set<CarIssue> services);

        void onRealTimeDataRetrieved();

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

        BSAbstractedFragmentActivity getActivity();
    }

    interface Presenter extends BluetoothPresenter{

        double getLatestMileage();

        void connectToDevice();

        /**
         * @param input validated mileage(non-negative, less than max value)
         */
        void updateMileage(double input);

        void checkRealTime();

        void getEngineCodes();

        void getServicesAndRecalls();

        void finishScan();

        void onActivityFinish();
    }

}
