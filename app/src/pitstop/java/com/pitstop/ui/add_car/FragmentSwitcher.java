package com.pitstop.ui.add_car;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public interface FragmentSwitcher {
    void setViewAskHasDevice();
    void setViewDeviceSearch();
    void setViewVinEntry(String scannerId, String scannerName);
}
