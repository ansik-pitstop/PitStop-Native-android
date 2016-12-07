package com.pitstop.ui.scan_car;

import com.pitstop.ui.BSAbstractedFragmentActivity;
import com.pitstop.ui.BaseView;
import com.pitstop.ui.BluetoothPresenter;
import com.pitstop.ui.ILoadingActivity;

import java.util.Set;

/**
 * Created by yifan on 16/12/6.
 */

public interface ScanCarContract {

    interface View extends BaseView, ILoadingActivity{

        void onDeviceConnected();

        void onConnectingTimeout();

        /**
         * callback for updateMileage(String input);
         */
        void onMileageUpdated();

        /**
         * callback for getIssues();
         */
        void onRecallRetrieved();

        /**
         * callback for getIssues();
         */
        void onServicesRetrieved();

        void onGetRealtimeDataTimeout();

        /**
         * callback for getEngineCodes();
         * @param dtcCodes set of retrieved dtcs, can be empty.
         */
        void onEngineCodesRetrieved(Set<String> dtcCodes);


        BSAbstractedFragmentActivity getActivity();
    }

    interface Presenter extends BluetoothPresenter{

        void connectToDevice();

        void updateMileage(String input);

        void getEngineCodes();

        void getIssues();

        void finish();
    }

}
